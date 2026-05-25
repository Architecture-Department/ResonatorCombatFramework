package architecture.resonator_combat_framework.module.player_animation.controller.eyelib

import architecture.resonator_combat_framework.module.player_animation.api.ProxyBone
import architecture.resonator_combat_framework.module.player_animation.api.ProxyLocator
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigLoader
import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController
import architecture.resonator_combat_framework.module.player_animation.util.EyeLibUtil
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.capability.component.AnimationComponent
import io.github.tt432.eyelib.capability.component.ClientEntityComponent
import io.github.tt432.eyelib.client.ClientTickHandler
import io.github.tt432.eyelib.client.animation.Animation
import io.github.tt432.eyelib.client.animation.AnimationEffects
import io.github.tt432.eyelib.client.animation.BrAnimator
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry
import io.github.tt432.eyelib.client.animation.bedrock.BrLoopType
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos
import io.github.tt432.eyelib.molang.MolangValue

/** eyelib 动画控制器 — 过渡系统 + BrAnimator 驱动 + ProxyModel lerp */
class EyeLibAnimationController(
	private val renderData: RenderData<*>,
	private val isClient: Boolean
) : IAnimationController {

	/** 当前帧的骨骼数据, Mapper 通过 cast 读取 */
	val proxyModel = ProxyModel("eyelib")

	/** 旧帧 ProxyModel 快照, 用于交叉淡入淡出 lerp */
	private var previousProxyModel: ProxyModel? = null

	override var blendFactor = 0f
	override var blendTarget = 0f
	override var currentTransitionTicks = ProxyBoneConfigData.DEFAULT_TRANSITION_TICKS

	override var speedMultiplier = 1f
	override var priority = 0
	override var isOverriding = true
	override var currentAnimId: String? = null
	override var affectedBones = emptySet<String>()

	/** eyelib 动画注册表 — 同时只能有一个动画 */
	private val activeAnimations = linkedMapOf<String, Animation<*>>()
	private val activeMultipliers = mutableMapOf<String, MolangValue>()
	private val configLoader = ProxyBoneConfigLoader.getInstance(isClient)
	private val boneController = EyelibBoneController()
	private val itemController = EyelibItemController()
	private var lastTickSec = 0f

	override fun isActive(): Boolean = activeAnimations.isNotEmpty()

	override fun trigger(animId: String, transitionTicks: Int) {
		trigger(animId, transitionTicks, 1f)
	}

	override fun trigger(animId: String, transitionTicks: Int, speedMultiplier: Float) {
		val anim = EyeLibUtil.getAnimation(isClient, animId) ?: return
		currentTransitionTicks = transitionTicks
		this.speedMultiplier = speedMultiplier
		val config = configLoader.getConfig(animId)
		val multiplier = MolangValue.getConstant(speedMultiplier)

		if (activeAnimations.containsKey(animId)) {
			restartAnimationInternal(animId, config, multiplier)
			return
		}

		// 单动画约束: 清除旧动画再触发新动画
		if (activeAnimations.isNotEmpty()) {
			previousProxyModel = ProxyModel("prev").also { deepCopyProxy(proxyModel, it) }
			activeAnimations.clear()
			activeMultipliers.clear()
		}

		activeAnimations[animId] = anim
		activeMultipliers[animId] = multiplier
		currentAnimId = animId
		affectedBones = config.resolveCurrentBoneNames()
		blendFactor = 0f; blendTarget = 1f
		if (currentTransitionTicks <= 0) {
			blendFactor = 1f; previousProxyModel = null
		}

		lastTickSec = 0f
		rebuildAnimate()
	}

	override fun triggerForDuration(
		animId: String,
		transitionTicks: Int,
		durationTicks: Int,
		originalAnimLengthSec: Float
	) {
		// 计算倍数: durationTicks / 20 = 期望秒数, originalAnimLengthSec / 期望秒数 = 倍数
		val desiredSec = durationTicks / 20f
		val calculatedMultiplier = if (desiredSec > 0f) originalAnimLengthSec / desiredSec else 1f
		trigger(animId, transitionTicks, calculatedMultiplier)
	}

	override fun stop() {
		val hasFade = activeAnimations.isNotEmpty()
		if (hasFade) {
			previousProxyModel = ProxyModel("prev").also { deepCopyProxy(proxyModel, it) }
		}
		blendTarget = 0f
		if (currentTransitionTicks <= 0 || !hasFade) {
			blendFactor = 0f; activeAnimations.clear(); activeMultipliers.clear()
			currentAnimId = null; affectedBones = emptySet()
			lastTickSec = 0f; rebuildAnimate()
		}
	}

	override fun stopAnimation(animId: String) {
		activeAnimations.remove(animId)
		activeMultipliers.remove(animId)
		if (activeAnimations.isEmpty()) {
			blendFactor = 0f; previousProxyModel = null; lastTickSec = 0f
			currentAnimId = null; affectedBones = emptySet()
		}
		rebuildAnimate()
	}

	override fun restartAnimation(animId: String) {
		restartAnimationInternal(animId, configLoader.getConfig(animId), MolangValue.getConstant(speedMultiplier))
	}

	private fun restartAnimationInternal(animId: String, config: ProxyBoneConfigData, multiplier: MolangValue) {
		activeMultipliers[animId] = multiplier
		currentAnimId = animId
		affectedBones = config.resolveCurrentBoneNames()
		blendTarget = 1f; blendFactor = 1f; previousProxyModel = null
		if (isClient) {
			val anim = EyeLibUtil.getAnimation(true, animId)
			if (anim != null) EyeLibUtil.setAnimateEntry(renderData, anim, multiplier)
			val data = EyeLibUtil.getEntryData(renderData, animId)
			if (data != null) {
				data.animTime = 0f; data.lastTicks = 0f; data.deltaTime = 0f
			}
		}
		lastTickSec = 0f
	}

	/** 每帧: 驱动 eyelib → 写骨骼/物品 Proxy → lerp 过渡 */
	override fun tick(partialTick: Float, deltaSec: Float) {
		if (!isClient) return
		val scope = renderData.scope
		val ticks = (ClientTickHandler.getTick() + partialTick) / 20
		val dSec = if (lastTickSec == 0f) 0f else ticks - lastTickSec
		lastTickSec = ticks
		tickBlend(dSec)
		if (activeAnimations.isEmpty()) return

		val ac: AnimationComponent = renderData.animationComponent
		val cec: ClientEntityComponent = renderData.clientEntityComponent
		val effects = AnimationEffects()

		val infos = if (ac.getSerializableInfo() != null)
			BrAnimator.tickAnimation(ac, scope, effects, ticks) {
				val ce = cec.clientEntity ?: return@tickAnimation
				ce.scripts().ifPresent { it.pre_animation().eval(scope) }
			} else BoneRenderInfos.EMPTY

		checkOnceAnimations()
		boneController.writeToProxy(infos, proxyModel)
		val la = proxyModel.getBone("left_arm") ?: return
		val ra = proxyModel.getBone("right_arm") ?: return
		itemController.writeToProxy(infos, la, ra)

		// lerp 旧姿态 → 新姿态
		previousProxyModel?.let { prev ->
			lerpProxyModels(prev, proxyModel, blendFactor)
			if (blendFactor >= 1f || blendFactor <= 0f) previousProxyModel = null
		}

		ac.tickedInfos = infos; ac.effects = effects
	}

	private fun checkOnceAnimations() {
		for ((animId, anim) in activeAnimations.toList()) {
			if (anim is BrAnimationEntry && anim.loop() == BrLoopType.ONCE) {
				val data = EyeLibUtil.getEntryData(renderData, animId) ?: continue
				// 考虑 speedMultiplier: animTime * multiplier 与 animationLength 比较
				val effectiveTime = data.animTime * speedMultiplier
				if (effectiveTime > anim.animationLength()) stopAnimation(animId)
			}
		}
	}

	/** 过渡 tick: blendFactor → blendTarget */
	private fun tickBlend(ds: Float) {
		if (blendFactor == blendTarget) return
		if (currentTransitionTicks <= 0) {
			blendFactor = blendTarget; return
		}
		val step = (ds * 20f) / currentTransitionTicks
		blendFactor = if (blendFactor < blendTarget) (blendFactor + step).coerceAtMost(blendTarget)
		else (blendFactor - step).coerceAtLeast(blendTarget)
		if (blendTarget == 0f && blendFactor <= 0f) {
			previousProxyModel = null; lastTickSec = 0f
			currentAnimId = null; affectedBones = emptySet()
			rebuildAnimate()
		}
	}

	private fun rebuildAnimate() {
		if (!isClient) return
		EyeLibUtil.animateSetup(renderData, activeAnimations.mapValues { it.value.name() }, activeMultipliers.toMap())
	}

	companion object {
		private fun deepCopyProxy(src: ProxyModel, dst: ProxyModel) {
			dst.bones.clear()
			for ((name, bone) in src.bones) {
				val copy = ProxyBone(name)
				copy.pos.set(bone.pos)
				copy.rotation.set(bone.rotation)
				copy.scale.set(bone.scale)
				for ((ln, ll) in bone.locators) {
					copy.addLocator(ProxyLocator(ln).also {
						it.pos.set(ll.pos);
						it.rotation.set(ll.rotation)
						it.scale.set(ll.scale)
					})
				}
				dst.addBone(copy)
			}
		}

		private fun lerpProxyModels(from: ProxyModel, to: ProxyModel, t: Float) {
			for ((name, fromBone) in from.bones) {
				val toBone = to.getBone(name) ?: continue
				fromBone.pos.lerp(toBone.pos, t, toBone.pos)
				fromBone.rotation.lerp(toBone.rotation, t, toBone.rotation)
				fromBone.scale.lerp(toBone.scale, t, toBone.scale)
			}
		}
	}
}
