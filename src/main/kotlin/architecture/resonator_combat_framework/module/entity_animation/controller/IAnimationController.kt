package architecture.resonator_combat_framework.module.entity_animation.controller

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.entity_animation.data.AnimationPlayData
import net.minecraft.resources.ResourceLocation

@AllOpe
interface IAnimationController {
	val id: ResourceLocation
	var blendFactor: Float
	var blendTarget: Float
	var currentTransitionTicks: Int
	var speedMultiplier: Float
	val isOverriding: Boolean
	var currentAnimId: String?
	var affectedBones: Set<String>

	fun isActive(): Boolean
	val effectiveWeight: Float
	val currentAnimTime: Float

	fun trigger(config: AnimationPlayData)

	fun trigger(
		animId: String,
		speedMultiplier: Float = 1f,
		fadeInTicks: Int = -1,
		fadeOutTicks: Int = -1
	) = trigger(
		AnimationPlayData(
			animId = animId,
			speedMultiplier = speedMultiplier,
			fadeInTicks = fadeInTicks,
			fadeOutTicks = fadeOutTicks
		)
	)

	fun stop(fadeOutTicks: Int = -1)
	fun pause()
	fun resume()
	fun tickAdvance(animationMapper: IAnimationMapper)
}
