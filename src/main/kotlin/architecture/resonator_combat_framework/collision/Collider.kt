package architecture.resonator_combat_framework.collision

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import org.joml.Matrix4fc
import org.joml.Vector3f
import org.joml.Vector3fc

/**
 * 碰撞器抽象基类。
 *
 * 对应 [Epic Fight Collider](https://github.com/Antikythera-Studios/epicfight/blob/1.21.1/src/main/java/yesman/epicfight/api/collider/Collider.java)。
 * 剔除 Epic Fight 特有依赖（[OpenMatrix4f]、[Armature]、[LivingEntityPatch]、[PoseMirror] 等），
 * 改用 JOML [Matrix4fc] 进行矩阵运算。
 *
 * @param modelCenter 碰撞体在模型空间中的中心偏移
 * @param outerAABB 用于 broad-phase 的粗略包围盒（模型空间）
 */
abstract class Collider(
	val modelCenter: Vector3fc,
	val outerAABB: AABB?,
) {
	/** 碰撞体在世界空间中的中心位置，由 [transform] 更新 */
	val worldCenter: Vector3f = Vector3f()

	/**
	 * 用指定的变换矩阵更新碰撞体的世界空间数据。
	 *
	 * 对应 Epic Fight `Collider.transform(OpenMatrix4f)`。
	 * 将 [modelCenter] 变换为 [worldCenter]。
	 */
	open fun transform(matrix: Matrix4fc) {
		matrix.transformPosition(Vector3f(modelCenter), worldCenter)
	}

	/**
	 * 窄相位碰撞检测：判断指定实体是否与此碰撞体相交。
	 *
	 * 对应 Epic Fight `Collider.isCollide(Entity)`。
	 */
	abstract fun isCollide(target: Entity): Boolean

	/**
	 * 深拷贝。
	 *
	 * 对应 Epic Fight `Collider.deepCopy()`。
	 * 用于 [MultiCollider] 中创建多个 sweep 样本。
	 */
	abstract fun deepCopy(): Collider

	/**
	 * 获取与此碰撞体相交的所有实体。
	 *
	 * 对应 Epic Fight `Collider.getCollideEntities(Entity)`。
	 * 先通过 [getHitboxAABB] 做 broad-phase AABB 筛选，
	 * 再对每个候选者调用 [isCollide] 做窄相位检测。
	 */
	open fun getCollideEntities(attacker: Entity): List<LivingEntity> {
		val hitbox = getHitboxAABB()
		return attacker.level().getEntities(attacker, hitbox) { e ->
			e is LivingEntity && !e.isSpectator && e.isAlive && e != attacker && isCollide(e)
		}.filterIsInstance<LivingEntity>()
	}

	/**
	 * 获取用于 broad-phase 的世界空间 AABB。
	 *
	 * 对应 Epic Fight `Collider.getHitboxAABB()`。
	 */
	open fun getHitboxAABB(): AABB {
		return outerAABB?.move(worldCenter.x().toDouble(), worldCenter.y().toDouble(), worldCenter.z().toDouble())
			?: AABB(
				worldCenter.x().toDouble(), worldCenter.y().toDouble(), worldCenter.z().toDouble(),
				worldCenter.x().toDouble(), worldCenter.y().toDouble(), worldCenter.z().toDouble()
			)
	}
}
