package architecture.resonator_combat_framework.module.animation.event

import architecture.goldenboughs_lib.util.Value
import architecture.resonator_combat_framework.module.animation.controller.IEntityAnimationController
import net.minecraft.core.particles.ParticleType
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.ICancellableEvent
import org.joml.Vector3d

/**
 * 粒子生成事件 —— 在动画每 tick 生成粒子时触发。
 * 包含 [Pre]（生成前，可取消并可修改粒子属性）和 [Post]（生成后）两个子事件。
 */
abstract class ParticleEvent(
	val particleId: ResourceLocation,
	val locatorName: String,
	animationController: IEntityAnimationController<*>
) : AnimEvent(animationController) {

	/**
	 * 粒子生成前事件 —— 在粒子实际生成前触发。
	 * 可取消，取消后将跳过本次粒子生成。
	 * 可通过修改 [particle]、[rotate] 和 [pos] 的值来调整即将生成的粒子。
	 */
	class Pre(
		animationController: IEntityAnimationController<*>,
		locatorName: String,
		particleId: ResourceLocation,
		val particle: Value<ParticleType<*>?>,
		val rotate: Value<Vector3d>,
		val pos: Value<Vector3d>
	) : ParticleEvent(particleId, locatorName, animationController), ICancellableEvent

	/**
	 * 粒子生成后事件 —— 在粒子已生成后触发。
	 * 包含最终生成的粒子类型、旋转和位置信息。
	 */
	class Post(
		animationController: IEntityAnimationController<*>,
		locatorName: String,
		particleId: ResourceLocation,
		val particle: ParticleType<*>?,
		val rotate: Vector3d,
		val pos: Vector3d
	) : ParticleEvent(particleId, locatorName, animationController)
}
