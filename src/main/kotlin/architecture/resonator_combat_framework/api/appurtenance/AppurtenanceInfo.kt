package architecture.resonator_combat_framework.api.appurtenance

import cn.solarmoon.spark_core.physics.PhysicsHost
import cn.solarmoon.spark_core.physics.body.addPhysicsBody
import cn.solarmoon.spark_core.physics.body.attachToEntity
import cn.solarmoon.spark_core.physics.body.removePhysicsBody
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.objects.PhysicsRigidBody
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level

open class AppurtenanceInfo<O>(
	val owner: O,
) where O : Entity, O : PhysicsHost {
	protected val physicsCollisions: LinkedHashMap<String, PhysicsCollisionObject> = linkedMapOf()

	open fun tick() {

	}

	open fun addPhysicsCollision(name: String, body: PhysicsRigidBody) {
		physicsCollisions[name] = body
		body.attachToEntity(owner)
		level().addPhysicsBody(body)
	}

	open fun removePhysicsCollision(name: String) {
		physicsCollisions.remove(name)
		owner.allPhysicsBodies.forEach {
			level().removePhysicsBody(it.value)
		}
	}

	protected fun level(): Level = owner.level()

	fun getPhysicsCollision(name: String): PhysicsCollisionObject? {
		return physicsCollisions[name]
	}
}

