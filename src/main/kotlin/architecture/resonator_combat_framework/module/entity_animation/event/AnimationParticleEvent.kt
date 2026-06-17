package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.ICancellableEvent
import org.joml.Vector3d

abstract class AnimationParticleEvent(
	val particleId: ResourceLocation,
	animationController: IEntityAnimationController<*>
) : AnimationEvent(animationController) {

	class Pre(
		animationController: IEntityAnimationController<*>,
		particleId: ResourceLocation,
		val particle: Value<ParticleOptions?>,
		val pos: Value<Vector3d>
	) : AnimationParticleEvent(particleId, animationController), ICancellableEvent

	class Post(
		animationController: IEntityAnimationController<*>,
		particleId: ResourceLocation,
		val particle: ParticleOptions?,
		val pos: Vector3d
	) : AnimationParticleEvent(particleId, animationController)
}

data class Value<T>(
	val oldValue: T,
	var newValue: T
) {
	companion object {
		@JvmStatic
		fun <T> of(value: T): Value<T> {
			return Value(value, value)
		}
	}
}