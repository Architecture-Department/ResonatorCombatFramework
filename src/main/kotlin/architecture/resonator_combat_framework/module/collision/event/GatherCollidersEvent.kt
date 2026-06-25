package architecture.resonator_combat_framework.module.collision.event

import architecture.resonator_combat_framework.animation.AttackPhase
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event
import org.joml.Matrix4f
import org.joml.Vector3f

/**
 * 碰撞体收集事件 —— 由 [AnimationControllerManager] 在每 tick 触发，
 * 让各系统将本 tick 的碰撞数据写入 [CollisionEntityData]。
 *
 * 监听此事件并调用 [addCollider]，或使用 [architecture.resonator_combat_framework.events.collision.CollisionBridge] 自动处理。
 *
 * @property entity  持有碰撞数据的实体
 */
class GatherCollidersEvent(val entity: Entity) : Event() {
	/** 碰撞体列表：id → (colliderPair, phase, animId, worldMatrix) */
	private val colliders = mutableListOf<ColliderEntry>()

	fun addCollider(
		id: ResourceLocation,
		phase: AttackPhase,
		boneName: String,
		center: Vector3f,
		halfExtents: Vector3f,
		worldMatrix: Matrix4f?,
	) {
		colliders.add(ColliderEntry(id, phase, boneName, center, halfExtents, worldMatrix))
	}

	fun getColliders(): List<ColliderEntry> = colliders

	data class ColliderEntry(
		val id: ResourceLocation,
		val phase: AttackPhase,
		val boneName: String,
		val center: Vector3f,
		val halfExtents: Vector3f,
		val worldMatrix: Matrix4f?,
	)
}