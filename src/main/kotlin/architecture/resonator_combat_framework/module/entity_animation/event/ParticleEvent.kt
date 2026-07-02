package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.goldenboughs_lib.util.Value
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import net.minecraft.core.particles.ParticleType
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.ICancellableEvent
import org.joml.Vector3d

/**
 * 粒子生成事件 —— [AttackAnimation.tickAdvance] 中每 tick 生成粒子时触发。
 */
abstract class ParticleEvent(
	val particleId: ResourceLocation,
	val locatorName: String,
	animationController: IEntityAnimationController<*>
) : AnimEvent(animationController) {

	class Pre(
		animationController: IEntityAnimationController<*>,
		locatorName: String,
		particleId: ResourceLocation,
		val particle: Value<ParticleType<*>?>,
		val rotate: Value<Vector3d>,
		val pos: Value<Vector3d>
	) : ParticleEvent(particleId, locatorName, animationController), ICancellableEvent

	class Post(
		animationController: IEntityAnimationController<*>,
		locatorName: String,
		particleId: ResourceLocation,
		val particle: ParticleType<*>?,
		val rotate: Vector3d,
		val pos: Vector3d
	) : ParticleEvent(particleId, locatorName, animationController)
}