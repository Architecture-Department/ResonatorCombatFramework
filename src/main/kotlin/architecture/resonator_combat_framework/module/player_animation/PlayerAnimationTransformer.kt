package architecture.resonator_combat_framework.module.player_animation

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.client.RcfPlayerAnimationBridge
import architecture.resonator_combat_framework.module.player_animation.config.RcfBoneConfig
import architecture.resonator_combat_framework.module.player_animation.config.RcfBoneConfigLoader
import architecture.resonator_combat_framework.module.player_animation.config.RcfBoneFlags
import architecture.resonator_combat_framework.module.player_animation.mixed.IBoneRenderInfoEntry
import architecture.resonator_combat_framework.module.player_animation.util.EyeLibUtil
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.client.animation.AnimationEffects
import io.github.tt432.eyelib.client.animation.BrAnimator
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos
import io.github.tt432.eyelib.molang.MolangScope
import net.minecraft.client.model.PlayerModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.world.entity.player.Player
import kotlin.math.abs

class PlayerAnimationTransformer(val player: Player) {
	private val isClient = player.level().isClientSide

	val rcfBoneConfigLoaderInstance = RcfBoneConfigLoader.getInstance(isClient)
	val animationManagerInstance = EyeLibUtil.getAnimationManager(isClient)

	// 客户端
	private val renderData: RenderData<Player>? = if (isClient) RenderData.getComponent(player) else null
	private val scope = MolangScope()

	var blendFactor = 0f; private set
	var blendTarget = 0f; private set
	private var currentBlendSpeed = 0.12f
	private var currentBoneConfig: RcfBoneConfig = RcfBoneConfig.EMPTY
	private var animTimeTracker = 0f

	init {
		if (isClient) {
			PlayerAnimationSetup.setupRenderData(renderData!!)
		}
	}

	// 客户端
	private fun bridgeData(): RcfPlayerAnimationBridge.BridgeData {
		return renderData!!.animationComponent.getAnimationData(RcfPlayerAnimationBridge.NAME) as RcfPlayerAnimationBridge.BridgeData
	}

	fun trigger(animId: String) {
		Rcf.LOGGER.info("[anim] trigger {} bf={} bt={} bs={}", animId, blendFactor, blendTarget, currentBlendSpeed)
		currentBoneConfig = rcfBoneConfigLoaderInstance.getConfig(animId)

		animTimeTracker = 0f
		currentBlendSpeed = currentBoneConfig.resolveBlendSpeed()
		blendTarget = 1f

		if (currentBlendSpeed >= 1f) {
			blendFactor = 1f
			blendTarget = 1f
		}

		clientTrigger(animId)
	}

	// 客户端
	private fun clientTrigger(animId: String) {
		if (!isClient) return
		val data = bridgeData()
		val prevId = data.activeAnimationId
		if (prevId != null && prevId != animId) {
			data.previousAnimationId = prevId
			resetEntryState(prevId)
			blendFactor = 0f
		} else {
			data.previousAnimationId = null
		}
		resetEntryState(animId)
		data.activeAnimationId = animId
		data.activeBoneConfig = currentBoneConfig
		data.crossFadeProgress = blendFactor
	}

	fun stop() {
		if (currentBlendSpeed >= 1f) {
			blendFactor = 0f
		}
		animTimeTracker = 0f
		currentBoneConfig = RcfBoneConfig.EMPTY
		blendTarget = 0f

		clientStop()
	}

	// 客户端
	private fun clientStop() {
		if (!isClient) return
		val data = bridgeData()
		val currId = data.activeAnimationId
		if (currId != null) {
			data.previousAnimationId = currId
			data.crossFadeProgress = blendFactor
		}
		data.activeAnimationId = null
		data.activeBoneConfig = RcfBoneConfig.EMPTY
	}

	private fun resetEntryState(animId: String) {
		val entry = animationManagerInstance.get(animId) as? BrAnimationEntry ?: return
		entry.onFinish(bridgeData().getEntryData(animId))
	}

	fun tick() {
		val d = blendTarget - blendFactor
		blendFactor = if (abs(d) < 0.001f) blendTarget
		else if (d > 0) (blendFactor + currentBlendSpeed).coerceAtMost(1f)
		else (blendFactor - currentBlendSpeed).coerceAtLeast(0f)
	}

	// 客户端
	fun applyTransform(model: PlayerModel<*>, partialTick: Float) {
		if (!isClient) return
		Rcf.LOGGER.debug("[anim] transform bf={} bt={} animT={}", blendFactor, blendTarget, animTimeTracker)
		if (blendFactor <= 0.001f && blendTarget <= 0f) {
			bridgeData().crossFadeProgress = 1f
			return
		}

//		MolangQuery.animTime(scope)
//		scope.set("query.anim_time", animTimeTracker * 20)

		val ac = renderData!!.animationComponent
		val ticks = (player.tickCount + partialTick) / 20f
		val data = bridgeData()
		data.crossFadeProgress = blendFactor
		val infos = BrAnimator.tickAnimation(ac, scope, AnimationEffects(), ticks) {}

		val animId = data.activeAnimationId
		if (animId != null) {
			animTimeTracker = data.getEntryData(animId).animTime
		}

		val boneFlags = data.activeBoneConfig.resolveBoneFlags(animTimeTracker)

		applyBone("head", infos, boneFlags, model.head, model.hat)
		applyBone("body", infos, boneFlags, model.body, model.jacket)
		applyBone("left_arm", infos, boneFlags, model.leftArm, model.leftSleeve)
		applyBone("right_arm", infos, boneFlags, model.rightArm, model.rightSleeve)
		applyBone("left_leg", infos, boneFlags, model.leftLeg, model.leftPants)
		applyBone("right_leg", infos, boneFlags, model.rightLeg, model.rightPants)
	}

	// 客户端
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

		for (part in parts) {
			val ip = part.initialPose

			if (!iInfos.renderPositionEmpty) {
				if (lock) {
					part.x += (ip.x - rp.x * 16f - part.x) * blendFactor
					part.y += (ip.y - rp.y * 16f - part.y) * blendFactor
					part.z += (ip.z + rp.z * 16f - part.z) * blendFactor
				} else {
					part.x += (-rp.x * 16f) * blendFactor
					part.y += (-rp.y * 16f) * blendFactor
					part.z += (+rp.z * 16f) * blendFactor
				}
			}

			if (!iInfos.renderRotationEmpty) {
				if (lock) {
					part.xRot += (ip.xRot - rr.x - part.xRot) * blendFactor
					part.yRot += (ip.yRot + rr.y - part.yRot) * blendFactor
					part.zRot += (ip.zRot + rr.z - part.zRot) * blendFactor
				} else {
					part.xRot += (-rr.x) * blendFactor
					part.yRot += (+rr.y) * blendFactor
					part.zRot += (+rr.z) * blendFactor
				}
			}

			if (!iInfos.renderScalaEmpty) {
				if (lock) {
					part.xScale += (1 + rs.x - part.xScale) * blendFactor
					part.yScale += (1 + rs.y - part.yScale) * blendFactor
					part.zScale += (1 + rs.z - part.zScale) * blendFactor
				} else {
					part.xScale += (rs.x) * blendFactor
					part.yScale += (rs.y) * blendFactor
					part.zScale += (rs.z) * blendFactor
				}
			}
		}
	}
}
