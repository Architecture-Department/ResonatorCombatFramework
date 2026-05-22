package architecture.resonator_combat_framework.module.player_animation.core

import architecture.resonator_combat_framework.module.player_animation.client.RcfPlayerAnimationBridge
import architecture.resonator_combat_framework.module.player_animation.config.RcfBoneConfig
import architecture.resonator_combat_framework.module.player_animation.config.RcfBoneConfigLoader
import architecture.resonator_combat_framework.module.player_animation.config.RcfBoneFlags
import io.github.tt432.eyelib.Eyelib
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

	// 客户端
	private val renderData = if (isClient) RenderData.getComponent<Player>(player) else null
	private val scope = MolangScope()

	var blendFactor = 0f; private set
	var blendTarget = 0f; private set
	private var currentBlendSpeed = 0.12f
	private var currentBoneConfig: RcfBoneConfig = RcfBoneConfig.EMPTY

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
		currentBoneConfig = RcfBoneConfigLoader.getConfig(animId)

		currentBlendSpeed = currentBoneConfig.resolveBlendSpeed()
		blendTarget = 1f

		if (currentBoneConfig.hasNoFadeIn()) {
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
		}
		data.activeAnimationId = animId
		data.activeBoneConfig = currentBoneConfig
		data.crossFadeProgress = blendFactor
	}

	fun stop() {
		if (currentBoneConfig.hasNoFadeOut()) {
			blendFactor = 0f
		}
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
		val entry = Eyelib.getAnimationManager().get(animId) as? BrAnimationEntry ?: return
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
		if (blendFactor <= 0.001f && blendTarget <= 0f) return

		scope.set("query.anim_time", (player.tickCount + partialTick) / 20f)
		scope.set("query.life_time", (player.tickCount + partialTick) / 20f)

		val ac = renderData!!.animationComponent
		val ticks = (player.tickCount + partialTick) / 20f
		val data = bridgeData()
		data.crossFadeProgress = blendFactor
		val infos = BrAnimator.tickAnimation(ac, scope, AnimationEffects(), ticks) {}

		val boneFlags = data.activeBoneConfig.resolveBoneFlags(ticks % 100f)

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
		val info = infos.getData(id)
		val rp = info.renderPosition
		val rr = info.renderRotation

		val lock = boneFlags[name]?.hasAnyLockState() == true

		for (part in parts) {
			if (lock) {
				part.x = rp.x * blendFactor
				part.y = -rp.y * blendFactor
				part.z = -rp.z * blendFactor
				part.xRot = rr.x * blendFactor
				part.yRot = rr.y * blendFactor
				part.zRot = rr.z * blendFactor
			} else {
				part.x += rp.x * blendFactor
				part.y -= rp.y * blendFactor
				part.z -= rp.z * blendFactor
				part.xRot += rr.x * blendFactor
				part.yRot += rr.y * blendFactor
				part.zRot += rr.z * blendFactor
			}
		}
	}
}
