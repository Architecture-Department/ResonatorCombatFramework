package architecture.resonator_combat_framework.animation

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.init.AnimationProperties
import architecture.resonator_combat_framework.module.collision.CollisionEntry
import architecture.resonator_combat_framework.module.collision.CollisionSystem
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.model.GeometryModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import org.joml.Matrix4f

/**
 * 攻击动画定义。
 *
 * 扩展 [ActionAnimationDef]，为攻击动画提供多阶段碰撞体管理、伤害计算与生命周期回调。
 * 每个阶段（[AttackPhase]）包含一组骨骼绑定的碰撞体，在动画时间线中按 [startTime, endTime) 自动激活。
 *
 * @property phases 攻击阶段列表，每个阶段定义了碰撞体与时间范围
 */
class AttackAnimationDef
@JvmOverloads
constructor(
	id: ResourceLocation,
	animationId: ResourceLocation,
	stateModifiers: Map<ResourceLocation, Boolean> = emptyMap(),
	/** 攻击阶段列表 */
	val phases: List<AttackPhase> = emptyList(),
) : ActionAnimationDef(id, animationId, stateModifiers) {

	/**
	 * 使用与动画定义 ID 相同的 ID 同时作为动画资源 ID 的便捷构造方法。
	 *
	 * @param id 动画定义 ID 与动画资源 ID
	 * @param phases 攻击阶段列表
	 */
	constructor(
		id: ResourceLocation,
		phases: List<AttackPhase> = emptyList()
	) : this(id, id, emptyMap(), phases)

	/**
	 * 使用可变参数传递攻击阶段的便捷构造方法。
	 *
	 * @param id 动画定义 ID 与动画资源 ID
	 * @param phases 攻击阶段可变参数
	 */
	constructor(
		id: ResourceLocation,
		vararg phases: AttackPhase = emptyArray()
	) : this(id, phases.toList())

	// ===== 阶段查询 =====

	/**
	 * 获取当前动画时间下活跃的攻击阶段。
	 *
	 * @param animTime 当前动画时间（秒）
	 * @return 处于活跃时间窗口内的所有攻击阶段
	 */
	fun getActivePhases(animTime: Float): List<AttackPhase> =
		phases.filter { animTime >= it.startTime && animTime < it.endTime }

	// ===== 碰撞回调 =====

	/**
	 * 命中实体时的伤害回调，由碰撞系统在检测到碰撞时自动调用。
	 *
	 * 根据攻击者类型（玩家或生物）选择对应的伤害来源，
	 * 并结合阶段属性中的伤害倍率计算最终伤害。
	 *
	 * @param attacker 攻击者
	 * @param target 被击中的目标实体
	 * @param phase 命中发生时活跃的攻击阶段
	 * @return 目标是否成功受到伤害
	 */
	fun onHurtEntity(attacker: LivingEntity, target: LivingEntity, phase: AttackPhase): Boolean {
		val amount = getDamage(attacker, phase).toFloat()

		val source = if (attacker is Player) {
			attacker.damageSources().playerAttack(attacker)
		} else {
			attacker.damageSources().mobAttack(attacker)
		}

		return target.hurt(source, amount)
	}

	/**
	 * 计算攻击伤害值。
	 *
	 * 基于攻击者的基础攻击力属性乘以阶段配置的伤害倍率。
	 *
	 * @param attacker 攻击者
	 * @param phase 当前攻击阶段
	 * @return 计算后的伤害值
	 */
	fun getDamage(
		attacker: LivingEntity,
		phase: AttackPhase
	): Double {
		return attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) *
			getProperty(AnimationProperties.DAMAGE_MULTIPLIER, phase).orElse(1.0f)!!
	}

	/**
	 * 碰撞体更新钩子，在 [starts]/[collider] 写入碰撞体前调用。
	 * 子类可覆写此方法修改碰撞体的属性（大小、位置、效果标志、射线模式等）。
	 *
	 * @param entity 持有该动画的实体
	 * @param animTime 当前动画时间（秒）
	 * @param poseData 当前帧的姿态数据
	 * @param brModel 几何模型引用
	 * @param mergedProxy 合并后的姿态代理数据
	 * @param controller 动画控制器
	 */
	fun onColliderUpdate(
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: GeometryModel,
		mergedProxy: PoseData,
		controller: IEntityAnimationController<*>,
	) {
	}

	// ===== 生命周期 =====

	/**
	 * 动画结束时的清理回调。
	 *
	 * 移除所有阶段分组的碰撞体及命中记录，防止残留碰撞体影响后续动画。
	 */
	override fun onEnd(entity: Entity) {
		val data = CollisionSystem.getData(entity)
		// 清理所有阶段分组的碰撞体和命中记录
		for (phaseIdx in phases.indices) {
			data.removeGroup(phaseGroupId(id, phaseIdx))
		}
	}

	// ===== tickAdvance =====

	override fun tickAdvance(
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: GeometryModel,
		mergedProxy: PoseData,
		controller: IEntityAnimationController<*>
	) {
		starts(entity, animTime, poseData, brModel, mergedProxy, controller)
	}

	/**
	 * 阶段状态管理入口。
	 *
	 * 对比当前动画时间与各阶段的时间窗口，判断哪些阶段开始、结束或保持活跃，
	 * 然后触发对应的事件钩子，并委托 [collider] 更新碰撞体状态。
	 *
	 * @param entity 持有该动画的实体
	 * @param animTime 当前动画时间（秒）
	 * @param poseData 当前帧的姿态数据
	 * @param brModel 几何模型
	 * @param mergedProxy 合并后的姿态代理数据
	 * @param controller 动画控制器
	 */
	fun starts(
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: GeometryModel,
		mergedProxy: PoseData,
		controller: IEntityAnimationController<*>
	) {
		// 通过碰撞数据判断当前实际存在的阶段分组
		val existingPhaseIndices = phases.indices
			.filter { CollisionSystem.getData(entity).getColliders(phaseGroupId(id, it)) != null }
			.toSet()

		// 获取当前时间下应活跃的阶段索引
		val currentPhaseIndices = phases.indices.filter { phaseIdx ->
			val phase = phases[phaseIdx]
			animTime >= phase.startTime && animTime < phase.endTime
		}.toSet()

		if (currentPhaseIndices.isEmpty()) {
			// 无活跃阶段：清理残留碰撞体
			if (existingPhaseIndices.isNotEmpty()) {
				existingPhaseIndices.forEach { CollisionSystem.getData(entity).removeColliders(phaseGroupId(id, it)) }
			}
			return
		}

		val startedIndices = currentPhaseIndices - existingPhaseIndices
		val endedIndices = existingPhaseIndices - currentPhaseIndices

		startedIndices.forEach { RcfEventHooks.AnimationPhaseStart(controller, phases[it]) }
		endedIndices.forEach { RcfEventHooks.AnimationPhaseEnd(controller, phases[it]) }

		collider(
			entity,
			animTime,
			poseData,
			brModel,
			mergedProxy,
			controller,
			currentPhaseIndices,
			startedIndices,
			endedIndices
		)
	}

	/**
	 * 碰撞体更新核心逻辑。
	 *
	 * 负责以下操作：
	 * 1. 触发碰撞体更新前的事件钩子
	 * 2. 调用 [onColliderUpdate] 供子类自定义修改
	 * 3. 移除已结束阶段的碰撞体
	 * 4. 为新阶段创建骨骼绑定的碰撞体（计算全局变换矩阵）
	 * 5. 对持续阶段原地更新 worldMatrix（零分配优化）
	 * 6. 触发碰撞体更新后的事件钩子
	 *
	 * @param entity 持有该动画的实体
	 * @param animTime 当前动画时间（秒）
	 * @param poseData 当前帧的姿态数据
	 * @param brModel 几何模型
	 * @param mergedProxy 合并后的姿态代理数据
	 * @param controller 动画控制器
	 * @param activeIndices 当前活跃阶段的索引集合
	 * @param startedIndices 新启动阶段的索引集合
	 * @param endedIndices 刚结束阶段的索引集合
	 */
	fun collider(
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: GeometryModel,
		mergedProxy: PoseData,
		controller: IEntityAnimationController<*>,
		activeIndices: Set<Int>,
		startedIndices: Set<Int>,
		endedIndices: Set<Int>,
	) {
		RcfEventHooks.AnimationColliderPre(controller, entity, animTime, poseData, brModel, mergedProxy)

		onColliderUpdate(entity, animTime, poseData, brModel, mergedProxy, controller)

		val data = CollisionSystem.getData(entity)

		// 1. 移除结束阶段的碰撞体
		for (phaseIdx in endedIndices) {
			data.removeColliders(phaseGroupId(id, phaseIdx))
		}

		val entityPos = entity.position()
		val bodyRot = -entity.getPreciseBodyRotation(1.0f) * (Math.PI.toFloat() / 180f)

		// 2. 处理活跃阶段：新阶段创建、持续阶段原地更新 worldMatrix
		for (phaseIdx in activeIndices) {
			val phase = phases[phaseIdx]
			val gid = phaseGroupId(id, phaseIdx)

			if (phaseIdx in startedIndices) {
				// 新阶段：创建碰撞体
				for (pair in phase.colliders) {
					if (mergedProxy.getBone(pair.boneName) == null) continue

					val worldMatrix = Matrix4f()
						.translate(entityPos.x.toFloat(), entityPos.y.toFloat(), entityPos.z.toFloat())
						.rotateY(bodyRot)
						.mul(brModel.computeBoneGlobalMatrix(pair.boneName, mergedProxy, true))

					data.addCollider(
						CollisionEntry(
							id = id,
							shape = pair.collider,
							worldMatrix = worldMatrix,
							groupId = gid,
						)
					)
				}
				continue
			}

			// 持续阶段：原地更新 worldMatrix，零分配
			val existing = data.getColliders(gid) ?: continue

			for (i in phase.colliders.indices) {
				if (i >= existing.size) break
				val pair = phase.colliders[i]
				if (mergedProxy.getBone(pair.boneName) == null) continue
				val entry = existing[i]
				val mat = entry.worldMatrix ?: Matrix4f().also { entry.worldMatrix = it }
				mat.identity()
					.translate(entityPos.x.toFloat(), entityPos.y.toFloat(), entityPos.z.toFloat())
					.rotateY(bodyRot)
					.mul(brModel.computeBoneGlobalMatrix(pair.boneName, mergedProxy, true))
			}
		}

		RcfEventHooks.AnimationColliderPost(controller, entity, animTime, poseData, brModel, mergedProxy)
	}

	private companion object {
		/** 阶段分组 ID：每阶段独立 groupId，支持按阶段增删碰撞体 */
		private fun phaseGroupId(animationId: ResourceLocation, phaseIdx: Int): ResourceLocation =
			rlOf(animationId.namespace, "${animationId.path}_${phaseIdx}")
	}
}
