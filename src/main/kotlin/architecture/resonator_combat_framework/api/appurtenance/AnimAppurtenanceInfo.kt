package architecture.resonator_combat_framework.api.appurtenance

import cn.solarmoon.spark_core.animation.IAnimatable
import cn.solarmoon.spark_core.animation.anim.AnimController
import cn.solarmoon.spark_core.animation.model.ModelController
import cn.solarmoon.spark_core.animation.model.ModelIndex
import cn.solarmoon.spark_core.event.BoneUpdateEvent
import cn.solarmoon.spark_core.physics.body.attachToBone
import cn.solarmoon.spark_core.util.toRadians
import com.jme3.bullet.objects.PhysicsRigidBody
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import org.joml.Matrix4f
import kotlin.math.PI

open class AnimatedAccessoryInfo<T, O>(
	owner: O,
	override val animatable: T,
	override val defaultModelIndex: ModelIndex,
) : AppurtenanceInfo<O>(owner), IAnimatable<T>
	where O : LivingEntity {

	override var animController: AnimController = AnimController(this)
	override var modelController: ModelController = ModelController(this)

	override val variables: MutableMap<String, Any> = mutableMapOf()
	override val animLevel: Level? = owner.level()

	override fun addPhysicsCollision(name: String, body: PhysicsRigidBody) {
		physicsCollisions[name] = body
		body.attachToBone(this, name)
	}

	override fun getWorldPositionMatrix(partialTicks: Number): Matrix4f {
		return Matrix4f()
			.translate(owner.getPosition(partialTicks.toFloat()).toVector3f())
			.rotateY(PI.toFloat() - owner.getPreciseBodyRotation(partialTicks.toFloat()).toRadians())
	}

	override fun onBoneUpdate(event: BoneUpdateEvent) {
	}
}
