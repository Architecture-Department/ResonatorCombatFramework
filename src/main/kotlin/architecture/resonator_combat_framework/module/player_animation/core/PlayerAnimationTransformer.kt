package architecture.resonator_combat_framework.module.player_animation.core

import architecture.resonator_combat_framework.module.player_animation.animdata.AnimationBoneData
import architecture.resonator_combat_framework.module.player_animation.animdata.BoneStateRegistry
import io.github.tt432.eyelib.capability.RenderData
import io.github.tt432.eyelib.client.animation.AnimationEffects
import io.github.tt432.eyelib.client.animation.BrAnimator
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos
import io.github.tt432.eyelib.molang.MolangScope
import net.minecraft.client.model.PlayerModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.world.entity.player.Player
import kotlin.math.abs

class PlayerAnimationTransformer(val player: Player) {
	@Suppress("UNCHECKED_CAST")
	private val renderData = RenderData.getComponent<Player>(player)
	private val scope = MolangScope()

	var blendFactor = 0f; private set
	var blendTarget = 0f; private set
	var blendSpeed = 0.12f

	private var currentAnimation: String? = null
	private var currentAnimData: AnimationBoneData? = null

	init {
		PlayerAnimationSetup.setupRenderData(renderData)
	}

	// 直接使用 eyelib 动画 ID（如 "animation.player.otsuchi_hold"）
	fun trigger(id: String) {
		currentAnimation = id
		currentAnimData = AnimationBoneData.load(id)
		blendTarget = 1f

		val states = currentAnimData?.resolveBoneStates(0f) ?: emptyMap()
		if (hasState(states, "no_fade_in")) {
			blendFactor = 1f
			blendTarget = 1f
		}
	}

	fun stop() {
		val states = currentAnimData?.resolveBoneStates(0f) ?: emptyMap()
		if (hasState(states, "no_fade_out")) {
			blendFactor = 0f
		}
		currentAnimation = null
		currentAnimData = null
		blendTarget = 0f
	}

	fun tick() {
		val d = blendTarget - blendFactor
		if (abs(d) < 0.001f) blendFactor = blendTarget
		else if (d > 0) blendFactor = (blendFactor + blendSpeed).coerceAtMost(1f)
		else blendFactor = (blendFactor - blendSpeed).coerceAtLeast(0f)
	}

	fun applyTransform(model: PlayerModel<*>, partialTick: Float) {
		if (blendFactor <= 0.001f && blendTarget <= 0f) return

		scope.set("query.anim_time", (player.tickCount + partialTick) / 20f)
		scope.set("query.life_time", (player.tickCount + partialTick) / 20f)

		val ac = renderData.animationComponent
		val ticks = (player.tickCount + partialTick) / 20f
		val infos = BrAnimator.tickAnimation(ac, scope, AnimationEffects(), ticks) {}

		val animTime = ticks % 100f
		val boneStates = currentAnimData?.resolveBoneStates(animTime) ?: emptyMap()

		applyBone("head", infos, boneStates, model.head, model.hat)
		applyBone("body", infos, boneStates, model.body, model.jacket)
		applyBone("left_arm", infos, boneStates, model.leftArm, model.leftSleeve)
		applyBone("right_arm", infos, boneStates, model.rightArm, model.rightSleeve)
		applyBone("left_leg", infos, boneStates, model.leftLeg, model.leftPants)
		applyBone("right_leg", infos, boneStates, model.rightLeg, model.rightPants)
	}

	private fun applyBone(
		name: String,
		infos: BoneRenderInfos,
		boneStates: Map<String, Set<String>>,
		vararg parts: ModelPart
	) {
		val id = GlobalBoneIdHandler.get(name)
		val info = infos.getData(id)
		val rp = info.renderPosition
		val rr = info.renderRotation

		val states = boneStates[name] ?: emptySet()
		val lock = states.any { BoneStateRegistry.get(it)?.lockVanilla() == true }

		for (part in parts) {
			if (lock) {
				part.x = rp.x * blendFactor
				part.y = -rp.y * blendFactor
				part.z = -rp.z * blendFactor
				part.xRot = -rr.x * blendFactor
				part.yRot = -rr.y * blendFactor
				part.zRot = rr.z * blendFactor
			} else {
				part.x += rp.x * blendFactor
				part.y -= rp.y * blendFactor
				part.z -= rp.z * blendFactor
				part.xRot -= rr.x * blendFactor
				part.yRot -= rr.y * blendFactor
				part.zRot += rr.z * blendFactor
			}
		}
	}

	private fun hasState(boneStates: Map<String, Set<String>>, stateName: String): Boolean {
		for ((_, states) in boneStates) {
			if (stateName in states) return true
		}
		return false
	}
}
