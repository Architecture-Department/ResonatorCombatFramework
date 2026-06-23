package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import net.minecraft.core.particles.ParticleType
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.ICancellableEvent
import org.joml.Vector3d

abstract class AnimationParticleEvent(
	val particleId: ResourceLocation,
	val locatorName: String,
	animationController: IEntityAnimationController<*>
) : AnimationEvent(animationController) {

	class Pre(
		animationController: IEntityAnimationController<*>,
		locatorName: String,
		particleId: ResourceLocation,
		val particle: Value<ParticleType<*>?>,
		val rotate: Value<Vector3d>,
		val pos: Value<Vector3d>
	) : AnimationParticleEvent(particleId, locatorName, animationController), ICancellableEvent

	class Post(
		animationController: IEntityAnimationController<*>,
		locatorName: String,
		particleId: ResourceLocation,
		val particle: ParticleType<*>?,
		val rotate: Vector3d,
		val pos: Vector3d
	) : AnimationParticleEvent(particleId, locatorName, animationController)
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