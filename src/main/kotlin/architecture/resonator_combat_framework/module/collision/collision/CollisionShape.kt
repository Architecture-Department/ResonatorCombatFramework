package architecture.resonator_combat_framework.module.collision.collision

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.collision.CollisionEntry
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB

/**
 * 碰撞形状 —— sealed interface，所有碰撞体类型都实现此接口。
 *
 * 坐标约定：
 * - 若 [boneName] 不为 null，center/halfExtents 为**骨骼局部坐标**
 * - 若 [boneName] 为 null，center/halfExtents 为**实体局部坐标**（相对实体原点）
 *
 * 外部系统（如攻击动画）只需关注相对坐标，世界变换由
 * [architecture.resonator_combat_framework.module.collision.CollisionSystem]
 * 在 tick 时处理。
 */
@AllOpe
interface CollisionShape {
	/**
	 * 检测此碰撞体是否与目标实体的 AABB 相交。
	 *
	 * @param entry    碰撞条目，包含 ID、分组、变换矩阵等元信息
	 * @param attacker 持有此碰撞体的攻击者实体
	 * @param targetBox 目标实体的 AABB（世界坐标）
	 * @return true 表示碰撞体与目标实体相交
	 */
	fun checkCollision(entry: CollisionEntry, attacker: Entity, targetBox: AABB): Boolean

	/**
	 * 计算此碰撞体在世界空间中的包围球半径和中心位置，用于构建搜索 AABB。
	 *
	 * @param entry    碰撞条目
	 * @param attacker 攻击者实体
	 * @return 世界包围球信息
	 */
	fun computeWorldBounds(entry: CollisionEntry, attacker: Entity): WorldBounds
}
