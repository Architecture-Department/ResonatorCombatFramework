package architecture.resonator_combat_framework.module.collision

import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.collision.collision.OBB
import architecture.resonator_combat_framework.module.collision.collision.WorldBounds
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import kotlin.math.sqrt

/**
 * 碰撞系统 —— 每 tick 处理实体的碰撞检测与命中事件。
 *
 * 核心流程：
 * 1. 通过 Attachment 获取实体的 [CollisionEntityData]
 * 2. 构建搜索 AABB（基于所有碰撞体的世界包围球）
 * 3. 筛选候选目标实体（LivingEntity）
 * 4. 对每个候选执行：
 *    a. 发射 [CollisionEntityEvent.Check] 事件（可取消）
 *    b. 防重复命中检测
 *    c. 球体距离提前退出
 *    d. SAT 碰撞检测
 *    e. 射线遮挡检查
 *    f. 标记命中并发射 [CollisionEntityEvent.Hit] 事件
 */
object CollisionSystem {

	/**
	 * 获取指定实体的 [CollisionEntityData]。
	 *
	 * @param entity 目标实体
	 * @return 实体的碰撞数据
	 */
	@JvmStatic
	fun getData(entity: Entity): CollisionEntityData {
		return entity.getData(RcfAttachmentTypes.ENTITY_COLLISION)
	}

	@JvmStatic
	fun hasData(entity: Entity): Boolean {
		return entity.hasData(RcfAttachmentTypes.ENTITY_COLLISION)
	}

	/**
	 * 每 tick 调用一次，处理指定实体的碰撞检测逻辑。
	 * 仅在服务端执行。
	 *
	 * @param entity 需进行碰撞检测的实体
	 */
	@JvmStatic
	fun tick(entity: Entity) {
		if (entity.level().isClientSide || hasData(entity)) return
		val data = getData(entity)
		if (data.activeColliders.isEmpty()) {
			return
		}
		processEntity(entity, data)
	}

	// ===== 主流程 =====

	/**
	 * 对单个实体的所有碰撞体执行碰撞检测主流程。
	 *
	 * 1. 构建搜索 AABB
	 * 2. 获取候选目标实体
	 * 3. 对每个候选 + 碰撞体执行碰撞处理
	 */
	private fun processEntity(attacker: Entity, data: CollisionEntityData) {
		val (searchBox, boundsCache) = buildSearchBox(attacker, data) ?: return

		val targets = attacker.level().getEntities(attacker, searchBox) { e ->
			e is LivingEntity && e != attacker && e.isAlive
		}
		RcfUtil.LOGGER.info("[COLLISION] {} found {} potential targets", attacker, targets.size)

		for (target in targets) {
			for (entry in data.activeColliders) {
				processCollision(attacker, target, entry, data, boundsCache)
			}
		}
	}

	// ===== 搜索 AABB 构建 =====

	/** 搜索 AABB 与包围球缓存的中间结果 */
	private data class SearchResult(
		val searchBox: AABB,
		val boundsCache: Map<CollisionEntry, WorldBounds>,
	)

	/**
	 * 根据所有碰撞体的世界包围球构建搜索 AABB。
	 * 同时缓存每个碰撞体的包围球信息，供后续距离判定使用。
	 *
	 * @param attacker 攻击者实体
	 * @param data     攻击者的碰撞数据
	 * @return SearchResult，包含搜索 AABB 和包围球缓存；若无碰撞体则返回 null
	 */
	private fun buildSearchBox(attacker: Entity, data: CollisionEntityData): SearchResult? {
		var minX = Double.MAX_VALUE
		var minY = Double.MAX_VALUE
		var minZ = Double.MAX_VALUE
		var maxX = Double.MIN_VALUE
		var maxY = Double.MIN_VALUE
		var maxZ = Double.MIN_VALUE
		var hasCollider = false
		val boundsCache = mutableMapOf<CollisionEntry, WorldBounds>()

		for (entry in data.activeColliders) {
			val bounds = computeWorldBounds(entry, attacker)
			boundsCache[entry] = bounds

			val expand = bounds.sphereRadius.toDouble() + 1.0
			if (bounds.cx - expand < minX) minX = bounds.cx - expand
			if (bounds.cy - expand < minY) minY = bounds.cy - expand
			if (bounds.cz - expand < minZ) minZ = bounds.cz - expand
			if (bounds.cx + expand > maxX) maxX = bounds.cx + expand
			if (bounds.cy + expand > maxY) maxY = bounds.cy + expand
			if (bounds.cz + expand > maxZ) maxZ = bounds.cz + expand
			hasCollider = true
		}

		if (!hasCollider) return null
		return SearchResult(AABB(minX, minY, minZ, maxX, maxY, maxZ), boundsCache)
	}

	// ===== 碰撞处理 =====

	/**
	 * 对单个碰撞条目与单个目标执行完整的碰撞处理管线。
	 *
	 * 依次执行：事件检查 → 重复命中检测 → 球体距离提前退出 →
	 * SAT 碰撞检测 → 射线遮挡检查 → 标记命中 → 发射命中事件。
	 */
	private fun processCollision(
		attacker: Entity,
		target: Entity,
		entry: CollisionEntry,
		data: CollisionEntityData,
		boundsCache: Map<CollisionEntry, WorldBounds>,
	) {
		val check = RcfEventHooks.collisionEntityCheck(attacker, entry, target, data)
		if (check.isCanceled) return
		if (data.isAlreadyHit(entry.groupId, target.uuid)) return

		if (!isWithinSphereRange(entry, target, boundsCache)) return
		if (!checkCollision(entry, attacker, target)) return
		if (!isValidHit(entry, attacker, target)) return
		if (!entry.hasEffect) return
		RcfUtil.LOGGER.info("[COLLISION] HIT! {} -> {}", attacker, target)

		if (check.isRecord) {
			data.markHit(entry.groupId, target.uuid)
		}
		RcfEventHooks.collisionEntityHit(attacker, entry, target, data)
	}

	// ===== 球体距离提前退出 =====

	/**
	 * 球体距离检测 —— 用包围球快速剔除远距离目标，避免执行完整的 SAT 检测。
	 *
	 * @param entry   碰撞条目
	 * @param target  目标实体
	 * @param boundsCache 包围球缓存
	 * @return true 表示目标在碰撞体的有效范围内
	 */
	private fun isWithinSphereRange(
		entry: CollisionEntry,
		target: Entity,
		boundsCache: Map<CollisionEntry, WorldBounds>,
	): Boolean {
		val wb = boundsCache[entry] ?: return false
		val box = target.boundingBox
		val cx = (box.minX + box.maxX) * 0.5
		val cy = (box.minY + box.maxY) * 0.5
		val cz = (box.minZ + box.maxZ) * 0.5
		val distSqr = (wb.cx - cx) * (wb.cx - cx) + (wb.cy - cy) * (wb.cy - cy) + (wb.cz - cz) * (wb.cz - cz)
		val halfExtents =
			sqrt(((box.maxX - box.minX) * 0.5).let { it * it } + ((box.maxY - box.minY) * 0.5).let { it * it } + ((box.maxZ - box.minZ) * 0.5).let { it * it }).toFloat()
		val maxDist = wb.sphereRadius + halfExtents
		return distSqr <= maxDist * maxDist
	}

	// ===== SAT 碰撞检测 =====

	/**
	 * 对碰撞体执行 SAT 碰撞检测。
	 */
	private fun checkCollision(entry: CollisionEntry, attacker: Entity, target: Entity): Boolean {
		return entry.shape.checkCollision(entry, attacker, target.boundingBox)
	}

	// ===== 世界包围球 =====

	/**
	 * 计算碰撞体的世界空间包围球信息。
	 */
	private fun computeWorldBounds(entry: CollisionEntry, attacker: Entity): WorldBounds {
		return entry.shape.computeWorldBounds(entry, attacker)
	}

	// ===== 射线遮挡检查 =====

	/**
	 * 执行射线遮挡检查 —— 确认碰撞体与目标之间没有被方块遮挡。
	 *
	 * @param entry   碰撞条目
	 * @param attacker 攻击者
	 * @param target  目标实体
	 * @return true 表示命中有效（无遮挡或遮挡可忽略）
	 */
	private fun isValidHit(entry: CollisionEntry, attacker: Entity, target: Entity): Boolean {
		val mode = entry.raycastMode
		if (mode == CollisionRaycastMode.NONE) return true

		val from = getRaycastOrigin(entry, attacker, mode)
		val to = target.position().add(0.0, target.bbHeight * 0.5, 0.0)
		val clip = attacker.level().clip(
			ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker)
		)
		return clip.type == HitResult.Type.MISS || clip.location.distanceToSqr(from) >= from.distanceToSqr(to) * 0.9
	}

	/**
	 * 根据射线检查模式获取射线起点。
	 *
	 * @param entry   碰撞条目
	 * @param attacker 攻击者
	 * @param mode    射线检查模式
	 * @return 射线起点坐标
	 */
	private fun getRaycastOrigin(entry: CollisionEntry, attacker: Entity, mode: CollisionRaycastMode): Vec3 {
		return when (mode) {
			CollisionRaycastMode.FROM_COLLIDER -> getColliderCenter(entry, attacker)
			CollisionRaycastMode.FROM_ENTITY -> attacker.position().add(0.0, attacker.eyeHeight * 0.7, 0.0)

			CollisionRaycastMode.NONE -> Vec3.ZERO
		}
	}

	/**
	 * 计算碰撞体在世界空间中的中心位置。
	 * 对 [OBB] 类型使用世界矩阵精确计算，其余类型使用包围球中心。
	 *
	 * @param entry   碰撞条目
	 * @param attacker 攻击者
	 * @return 碰撞体中心的世界坐标
	 */
	private fun getColliderCenter(entry: CollisionEntry, attacker: Entity): Vec3 {
		return when (val shape = entry.shape) {
			is OBB -> {
				val worldCenter = if (entry.worldMatrix != null) {
					Vector3f(shape.center).mulPosition(entry.worldMatrix)
				} else {
					Vector3f(
						(attacker.x + shape.center.x).toFloat(),
						(attacker.y + shape.center.y).toFloat(),
						(attacker.z + shape.center.z).toFloat(),
					)
				}
				Vec3(worldCenter.x.toDouble(), worldCenter.y.toDouble(), worldCenter.z.toDouble())
			}

			else -> {
				val bounds = shape.computeWorldBounds(entry, attacker)
				Vec3(bounds.cx, bounds.cy, bounds.cz)
			}
		}
	}
}
