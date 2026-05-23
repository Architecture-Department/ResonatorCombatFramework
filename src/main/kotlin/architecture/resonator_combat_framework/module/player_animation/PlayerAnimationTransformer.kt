package architecture.resonator_combat_framework.module.player_animation

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.api.IPlayerAnimator
import architecture.resonator_combat_framework.module.player_animation.config.RcfBoneConfig
import architecture.resonator_combat_framework.module.player_animation.config.RcfBoneConfigLoader
import architecture.resonator_combat_framework.module.player_animation.config.RcfBoneFlags
import architecture.resonator_combat_framework.module.player_animation.mixed.IBoneRenderInfoEntry
import architecture.resonator_combat_framework.module.player_animation.util.EyeLibUtil
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.capability.component.AnimationComponent
import io.github.tt432.eyelib.capability.component.ClientEntityComponent
import io.github.tt432.eyelib.client.ClientTickHandler
import io.github.tt432.eyelib.client.animation.Animation
import io.github.tt432.eyelib.client.animation.AnimationEffects
import io.github.tt432.eyelib.client.animation.BrAnimator
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry
import io.github.tt432.eyelib.client.animation.bedrock.BrLoopType
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos
import io.github.tt432.eyelib.molang.MolangScope
import io.github.tt432.eyelib.molang.MolangValue
import net.minecraft.client.model.PlayerModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.world.entity.player.Player
import org.joml.Quaternionf

/** 每玩家实例，负责动画的触发、停止、过渡和骨骼变换应用 */
class PlayerAnimationTransformer(val player: Player) : IPlayerAnimator {
	private val isClient = player.level().isClientSide

	private val configLoader = RcfBoneConfigLoader.getInstance(isClient)

	private val renderData: RenderData<Player> = RenderData.getComponent(player)

	// 过渡状态
	private var blendFactor = 0f       // 当前混合权重 (0=原版, 1=完全动画)
	private var blendTarget = 0f       // 混合目标值
	private var currentTransitionTicks = RcfBoneConfig.DEFAULT_TRANSITION_TICKS  // 过渡用时 (tick 制), 0=即时, 20=1秒

	// 动画注册表
	private val activeAnimations = linkedMapOf<String, Animation<*>>()    // 当前动画: animId → Animation
	private val activeMultipliers = mutableMapOf<String, MolangValue>()   // 当前动画 multiplier
	private val previousAnimations = linkedMapOf<String, Animation<*>>()  // 前一个动画 (交叉淡入淡出)
	private val previousMultipliers = mutableMapOf<String, MolangValue>() // 前一个动画 multiplier
	private val boneConfigs = mutableMapOf<String, RcfBoneConfig>()       // 动画骨骼配置
	private var animTimeTracker = 0f   // 动画运行时间 (秒), 用于 timeline 解析
	private var lastTickSec = 0f       // 上一帧时间戳, 用于 delta 计算

	// 物品动画, 由 eyelib 虚拟骨骼 "right_item" / "left_item" 驱动
	private var rightItemTransform = ItemTransform()
	private var leftItemTransform = ItemTransform()

	init {
		PlayerAnimationSetup.setupRenderData(renderData)
	}

	// ---- IPlayerAnimator ----

	override fun isActive(): Boolean = activeAnimations.isNotEmpty() || previousAnimations.isNotEmpty()

	/** 触发动画。若同一动画已在播放中则重启；否则交叉淡入淡出覆盖当前动画 */
	override fun trigger(animId: String) {
		val anim = EyeLibUtil.getAnimation(isClient, animId)
		if (anim == null) {
			Rcf.LOGGER.error("[anim] trigger {} failed: not found in AnimationManager", animId)
			return
		}
		val config = configLoader.getConfig(animId)
		currentTransitionTicks = config.transitionTicks

		// 同一动画重复触发：重置时间线，跳过交叉淡入淡出
		if (activeAnimations.containsKey(animId) || previousAnimations.containsKey(animId)) {
			restartAnimation(animId, config)
			return
		}

		// 将当前动画移入 previous，新动画放入 active
		previousAnimations.putAll(activeAnimations)
		previousMultipliers.putAll(activeMultipliers)
		activeAnimations.clear()
		activeMultipliers.clear()
		boneConfigs.clear()
		boneConfigs[animId] = config
		activeAnimations[animId] = anim
		val hasCrossFade = previousAnimations.isNotEmpty()
		if (hasCrossFade) {
			activeMultipliers[animId] = MolangValue.getConstant(0f)
			blendFactor = 0f
		} else {
			activeMultipliers[animId] = MolangValue.ONE
			blendFactor = 0f
		}
		blendTarget = 1f
		if (currentTransitionTicks <= 0) {
			blendFactor = 1f
			previousAnimations.clear()
			previousMultipliers.clear()
		}

		animTimeTracker = 0f
		lastTickSec = 0f

		// setup() 会重置所有 animationData → 保存/恢复 previous 动画时间线
		val savedTimes = if (hasCrossFade) saveAnimTimes() else emptyMap()
		rebuildAnimate()
		restoreAnimTimes(savedTimes)
	}

	/** 停止所有动画，带过渡淡出 */
	override fun stop() {
		previousAnimations.clear()
		previousMultipliers.clear()
		blendTarget = 0f
		if (currentTransitionTicks <= 0) {
			blendFactor = 0f
			activeAnimations.clear()
			activeMultipliers.clear()
			boneConfigs.clear()
			animTimeTracker = 0f
			lastTickSec = 0f
			rebuildAnimate()
		}
	}

	/** 移除指定动画 */
	override fun stopAnimation(animId: String) {
		activeAnimations.remove(animId)
		activeMultipliers.remove(animId)
		previousAnimations.remove(animId)
		previousMultipliers.remove(animId)
		boneConfigs.remove(animId)
		if (activeAnimations.isEmpty() && previousAnimations.isEmpty()) {
			blendFactor = 0f
			animTimeTracker = 0f
			lastTickSec = 0f
		}
		rebuildAnimate()
	}

	// ---- 内部: 同一动画重启 ----

	/** 同一动画重复触发时直接重置时间线，不和自己交叉淡入淡出 */
	private fun restartAnimation(animId: String, config: RcfBoneConfig) {
		previousAnimations.remove(animId)
		previousMultipliers.remove(animId)

		activeMultipliers[animId] = MolangValue.ONE
		boneConfigs[animId] = config
		blendTarget = 1f
		blendFactor = 1f

		if (isClient) {
			val anim = EyeLibUtil.getAnimation(true, animId)
			if (anim != null) {
				EyeLibUtil.setAnimateEntry(renderData, anim, MolangValue.ONE)
			}
			val data = EyeLibUtil.getEntryData(renderData, animId)
			if (data != null) {
				data.animTime = 0f
				data.lastTicks = 0f
				data.deltaTime = 0f
			}
		}

		animTimeTracker = 0f
		lastTickSec = 0f
	}

	// ---- 内部: AnimationComponent 管理 ----

	/** 将 active + previous 合并且写入 AnimationComponent (调用 setup, 会重建 animationData) */
	private fun rebuildAnimate() {
		if (!isClient) return
		val allAnims = linkedMapOf<String, Animation<*>>().apply {
			putAll(previousAnimations)
			putAll(activeAnimations)
		}
		val allMultipliers = mutableMapOf<String, MolangValue>().apply {
			putAll(previousMultipliers)
			putAll(activeMultipliers)
		}
		EyeLibUtil.animateSetup(renderData, allAnims.mapValues { it.value.name() }, allMultipliers.toMap())
	}

	/** 只更新 multiplier 值，不重建 animationData (用于交叉淡入淡出期间的逐帧更新) */
	private fun updateAnimateMultipliers() {
		if (!isClient) return
		for ((name, anim) in previousAnimations) {
			EyeLibUtil.setAnimateEntry(renderData, anim, previousMultipliers[name]!!)
		}
		for ((name, anim) in activeAnimations) {
			EyeLibUtil.setAnimateEntry(renderData, anim, activeMultipliers[name]!!)
		}
	}

	/** 从 AnimationComponent 中移除指定动画，不重建 Data */
	private fun removeFromAnimate(anims: Map<String, Animation<*>>) {
		if (!isClient) return
		EyeLibUtil.removeAnimateEntries(renderData, anims.values.toList())
	}

	// ---- 内部: animTime 保存/恢复 ----

	/** rebuildAnimate() 调用 setup 会重置所有 animationData → 保存 previous 动画时间 */
	private fun saveAnimTimes(): Map<String, Float> {
		val map = mutableMapOf<String, Float>()
		for (name in previousAnimations.keys) {
			EyeLibUtil.getAnimTime(renderData, name)?.let { map[name] = it }
		}
		return map
	}

	/** 恢复 previous 动画时间到新创建的 Data 中 */
	private fun restoreAnimTimes(saved: Map<String, Float>) {
		for ((name, time) in saved) {
			EyeLibUtil.setAnimTime(renderData, name, time)
		}
	}

	// ---- 客户端: 每帧渲染 ----
	/** 由 ItemInHandLayerMixin 每帧调用: 应用物品偏移 */
	fun applyItemTransform(
		leftHand: Boolean,
		poseStack: PoseStack
	) {
		val t = if (leftHand) getLeftItemTransform() else getRightItemTransform()
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))
		if (t.posX != 0f || t.posY != 0f || t.posZ != 0f) {
			poseStack.translate(t.posX, t.posY, t.posZ)
		}
		if (t.rotZ != 0f || t.rotY != 0f || t.rotX != 0f) {
			poseStack.mulPose(Quaternionf().rotationZYX(t.rotZ, t.rotY, t.rotX))
		}
		if (t.scaleX != 1f || t.scaleY != 1f || t.scaleZ != 1f) {
			poseStack.scale(t.scaleX, t.scaleY, t.scaleZ)
		}
		poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f))
	}

	/** 由 LivingEntityRendererMixin 每帧调用: 驱动 eyelib 、更新过渡、应用骨骼变换 */
	fun applyTransform(model: PlayerModel<*>, partialTick: Float) {
		if (!isClient) return

		// 计算 delta 时间
		val ticks = (ClientTickHandler.getTick() + partialTick) / 20
		val deltaSec = if (lastTickSec == 0f) 0f else ticks - lastTickSec
		lastTickSec = ticks
		tickBlend(deltaSec)

		if (activeAnimations.isEmpty() && previousAnimations.isEmpty()) return

		// eyelib 动画计算
		val scope: MolangScope = renderData.scope
		val animationComponent: AnimationComponent = renderData.animationComponent
		val clientEntityComponent: ClientEntityComponent = renderData.clientEntityComponent
		val effects = AnimationEffects()
		scope.set("variable.partial_tick", partialTick)
		scope.set("variable.attack_time", (player.swingTime.toFloat()) / player.currentSwingDuration)

		val infos = if (animationComponent.getSerializableInfo() != null) {
			BrAnimator.tickAnimation(animationComponent, scope, effects, ticks) {
				val ce = clientEntityComponent.clientEntity ?: return@tickAnimation
				ce.scripts().ifPresent { it.pre_animation().eval(scope) }
			}
		} else {
			BoneRenderInfos.EMPTY
		}

		animTimeTracker += deltaSec

		// 自动停止 ONCE 模式已完成的动画
		for ((animId, anim) in activeAnimations.toList()) {
			if (anim is BrAnimationEntry && anim.loop() == BrLoopType.ONCE) {
				val data = EyeLibUtil.getEntryData(renderData, animId) ?: continue
				if (data.animTime > anim.animationLength()) {
					stopAnimation(animId)
				}
			}
		}

		// 合并所有活跃动画的骨骼 flags
		val boneFlags = mutableMapOf<String, RcfBoneFlags>()
		for ((animId, config) in boneConfigs) {
			boneFlags.putAll(config.resolveBoneFlags(animTimeTracker))
		}

		extractItemTransform("left_item", infos, leftItemTransform)
		extractItemTransform("right_item", infos, rightItemTransform)

		// 将 eyelib 计算结果应用到 ModelPart
		applyBone("head", infos, boneFlags, model.head, model.hat)
		applyBone("body", infos, boneFlags, model.body, model.jacket)
		applyBone("left_arm", infos, boneFlags, model.leftArm, model.leftSleeve)
		applyBone("right_arm", infos, boneFlags, model.rightArm, model.rightSleeve)
		applyBone("left_leg", infos, boneFlags, model.leftLeg, model.leftPants)
		applyBone("right_leg", infos, boneFlags, model.rightLeg, model.rightPants)

		animationComponent.tickedInfos = infos
		animationComponent.effects = effects
	}

	// ---- 内部: 过渡 tick ----

	/** 每帧更新 blendFactor, 驱动交叉淡入淡出 / fade-in / fade-out */
	private fun tickBlend(deltaSec: Float) {
		if (blendFactor == blendTarget) return
		if (currentTransitionTicks <= 0) {
			blendFactor = blendTarget
			return
		}
		val step = (deltaSec * 20f) / currentTransitionTicks
		blendFactor = if (blendFactor < blendTarget)
			(blendFactor + step).coerceAtMost(blendTarget)
		else
			(blendFactor - step).coerceAtLeast(blendTarget)

		// 交叉淡入淡出: 逐帧更新 eyelib 中 previous/active 的 multiplier 权重
		if (previousAnimations.isNotEmpty()) {
			for (key in previousMultipliers.keys) {
				previousMultipliers[key] = MolangValue.getConstant(1f - blendFactor)
			}
			for (key in activeMultipliers.keys) {
				activeMultipliers[key] = MolangValue.getConstant(blendFactor)
			}
			updateAnimateMultipliers()
			if (blendFactor >= 1f) {
				removeFromAnimate(previousAnimations)
				previousAnimations.clear()
				previousMultipliers.clear()
			}
		}

		// fade-out 完成: 清理所有动画并同步 serializableInfo
		if (blendTarget == 0f && blendFactor <= 0f) {
			removeFromAnimate(activeAnimations)
			removeFromAnimate(previousAnimations)
			activeAnimations.clear()
			activeMultipliers.clear()
			boneConfigs.clear()
			previousAnimations.clear()
			previousMultipliers.clear()
			animTimeTracker = 0f
			lastTickSec = 0f
			rebuildAnimate()
		}
	}

	// ---- 客户端: 物品变换 ----

	private fun extractItemTransform(boneName: String, infos: BoneRenderInfos, target: ItemTransform) {
		val id = GlobalBoneIdHandler.get(boneName)
		if (!infos.infos.containsKey(id)) {
			target.posX = 0f;
			target.posY = 0f;
			target.posZ = 0f
			target.rotX = 0f;
			target.rotY = 0f;
			target.rotZ = 0f
			target.scaleX = 1f;
			target.scaleY = 1f;
			target.scaleZ = 1f
			return
		}
		val info = infos.getData(id)
		val w = if (previousAnimations.isNotEmpty()) 1f else blendFactor
		target.posX = info.renderPosition.x * w
		target.posY = info.renderPosition.y * w
		target.posZ = info.renderPosition.z * w
		target.rotX = info.renderRotation.x * w
		target.rotY = info.renderRotation.y * w
		target.rotZ = info.renderRotation.z * w
		target.scaleX = 1f + info.renderScala.x * w
		target.scaleY = 1f + info.renderScala.y * w
		target.scaleZ = 1f + info.renderScala.z * w
	}

	fun getRightItemTransform(): ItemTransform = rightItemTransform
	fun getLeftItemTransform(): ItemTransform = leftItemTransform

	// ---- 客户端: 骨骼变换 ----

	/**
	 * 将 eyelib 计算的 BoneRenderInfos 应用到 ModelPart
	 *
	 * lock 模式: 替换原版旋转, 叠加位置
	 *
	 * 交叉淡入淡出期间 weight=1f (eyelib multiplier 控制权重), 否则用 blendFactor
	 */
	private fun applyBone(
		name: String,
		infos: BoneRenderInfos,
		boneFlags: Map<String, RcfBoneFlags>,
		vararg parts: ModelPart
	) {
		val id = GlobalBoneIdHandler.get(name)
		if (!infos.infos.containsKey(id)) return

		val info = infos.getData(id)
		val rp = info.renderPosition
		val rr = info.renderRotation
		val rs = info.renderScala

		val lock = boneFlags[name]?.hasAnyLockState() ?: false
		val iInfos = IBoneRenderInfoEntry.of(info)
		// 交叉淡入淡出时 eyelib multiplier 已控制权重, applyBone 不应再乘 blendFactor
		val weight = if (previousAnimations.isNotEmpty()) 1f else blendFactor

		for (part in parts) {
			val ip = part.initialPose

			if (!iInfos.renderPositionEmpty) {
				if (lock) {
					part.x += (ip.x - rp.x * 16f - part.x) * weight
					part.y += (ip.y - rp.y * 16f - part.y) * weight
					part.z += (ip.z + rp.z * 16f - part.z) * weight
				} else {
					part.x += (-rp.x * 16f) * weight
					part.y += (-rp.y * 16f) * weight
					part.z += (+rp.z * 16f) * weight
				}
			}

			if (!iInfos.renderRotationEmpty) {
				if (lock) {
					part.xRot += (ip.xRot - rr.x - part.xRot) * weight
					part.yRot += (ip.yRot + rr.y - part.yRot) * weight
					part.zRot += (ip.zRot + rr.z - part.zRot) * weight
				} else {
					part.xRot += (-rr.x) * weight
					part.yRot += (+rr.y) * weight
					part.zRot += (+rr.z) * weight
				}
			}

			if (!iInfos.renderScalaEmpty) {
				if (lock) {
					part.xScale += (1 + rs.x - part.xScale) * weight
					part.yScale += (1 + rs.y - part.yScale) * weight
					part.zScale += (1 + rs.z - part.zScale) * weight
				} else {
					part.xScale += (rs.x) * weight
					part.yScale += (rs.y) * weight
					part.zScale += (rs.z) * weight
				}
			}
		}
	}
}
