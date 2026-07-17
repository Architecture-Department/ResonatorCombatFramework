package architecture.resonator_combat_framework.combat

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import org.joml.Matrix4f
import org.joml.Matrix4fc

/**
 * 多重碰撞器——在前后帧之间插值生成多个 sweep 样本。
 *
 * 对应 [Epic Fight MultiCollider](https://github.com/Antikythera-Studios/epicfight/blob/1.21.1/src/main/java/yesman/epicfight/api/collider/MultiCollider.java)。
 * 剔除 Epic Fight 特有依赖（[Armature]/[Joint]/[Pose]/[AttackAnimation]/[LivingEntityPatch]），
 * 改为由调用方通过 [currBoneMatrix] lambda 提供插值骨骼矩阵。
 *
 * @param sourceCollider 源碰撞器（会被 [deepCopy] 多次用于 sweep 样本）
 * @param numberOfColliders sweep 样本数
 */
open class MultiCollider(
	private val sourceCollider: Collider?,
	val numberOfColliders: Int = 3,
) {
	/**
	 * 在前后帧骨骼矩阵之间平均插值，检测所有被碰撞的实体。
	 *
	 * 对应 Epic Fight `MultiCollider.updateAndSelectCollideEntity()`。
	 * 骨骼插值由调用方的 [function] lambda 实现，采样点均匀分布。
	 *
	 * 结果默认按距离排序。防穿墙通过从攻击者眼部到目标眼部发射射线检测：
	 * 若射线被方块阻挡且方块在目标前方，则判定目标在墙后。
	 *
	 * @param pose 实体基础变换矩阵（T(pos) * R(-yaw)），用于将骨骼矩阵变换到世界空间
	 * @param function 骨骼矩阵插值函数：接收 t（0~1），返回此 t 下骨骼的模型空间矩阵
	 * @param attacker 攻击者
	 * @param count sweep 步数（默认 [numberOfColliders]）
	 * @param sortByDistance 是否按距离排序（近→远）
	 * @param preventWallPenetration 是否检测墙体阻挡（眼部射线检测）
	 * @return 被碰撞的实体列表（已去重，按距离排序）
	 */
	open fun updateAndSelectCollideEntity(
		pose: Matrix4fc,
		function: (Float) -> Matrix4f,
		attacker: Entity,
		count: Int = numberOfColliders,
		sortByDistance: Boolean = true,
		preventWallPenetration: Boolean = true,
	): List<LivingEntity> {
		val samples = buildSweepSamples(sourceCollider, pose, function, count) ?: return emptyList()
		val hitbox = computeMergedAABB(samples) ?: return emptyList()

		// broad + narrow phase
		return attacker.level().getEntities(attacker, hitbox) { e ->
			if (e.isSpectator || !e.isAlive) return@getEntities false
			if (e is LivingEntity && e == attacker) return@getEntities false
			samples.any { sample -> sample.isCollide(e) }
		}.filterIsInstance<LivingEntity>()
			.filter { e -> !preventWallPenetration || hasLineOfSight(attacker, e) }
			.distinct()
			.let { if (sortByDistance) it.sortedBy { e -> e.distanceToSqr(attacker) } else it }
	}

	/**
	 * 获取碰撞器列表（未变换的深拷贝）。
	 */
	open fun getColliders(count: Int = numberOfColliders): List<Collider> {
		val collider = sourceCollider ?: return emptyList()
		return List(count) { collider.deepCopy() }
	}

	companion object {
		/**
		 * 构建 sweep 样本列表。
		 *
		 * 对 [sourceCollider] 进行 [count] 次 [deepCopy]，分别用 [pose] * [currBoneMatrix](t) 变换。
		 * 采样点均匀分布：t = 0, 1/(count-1), 2/(count-1), ..., 1。
		 *
		 * @param pose 实体基础变换矩阵
		 * @param function 骨骼矩阵插值函数
		 * @param count sweep 步数
		 * @return sweep 样本列表，若 [sourceCollider] 为 null 则返回 null
		 */
		@JvmStatic
		fun buildSweepSamples(
			sourceCollider: Collider?,
			pose: Matrix4fc,
			function: (Float) -> Matrix4f,
			count: Int,
		): List<Collider>? {
			val collider = sourceCollider ?: return null
			return List(count) { step ->
				val t = if (count > 1) step / (count - 1f) else 1f
				val mat = Matrix4f(pose).mul(function(t))
				val copy = collider.deepCopy()
				copy.transform(mat)
				return@List copy
			}
		}

		/**
		 * 从 sweep 样本列表计算合并后的 AABB。
		 * 合并所有样本的 [Collider.getHitboxAABB] 为单个 AABB，用于 broad-phase。
		 *
		 * @param samples sweep 样本列表
		 * @return 合并后的 AABB，若列表为空则返回 null
		 */
		@JvmStatic
		fun computeMergedAABB(samples: List<Collider>): AABB? {
			var merged: AABB? = null
			for (sample in samples) {
				val aabb = sample.getHitboxAABB()
				merged = if (merged == null) aabb else merged.minmax(aabb)
			}
			return merged
		}

		/**
		 * 从攻击者眼部向目标眼部发射射线，检测是否有方块阻挡。
		 *
		 * 若射线未命中任何方块，或命中的方块在目标后方，则视为有视线（可穿透）。
		 * 若命中的方块在目标前方，则判定目标在墙后。
		 */
		private fun hasLineOfSight(from: Entity, to: Entity): Boolean {
			val ctx = ClipContext(
				from.eyePosition, to.eyePosition,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from,
			)
			val hit = from.level().clip(ctx)
			if (hit.type != HitResult.Type.BLOCK) return true

			val distBlock = hit.location.distanceToSqr(from.eyePosition)
			val distTarget = to.eyePosition.distanceToSqr(from.eyePosition)
			return distBlock >= distTarget
		}
	}
}
