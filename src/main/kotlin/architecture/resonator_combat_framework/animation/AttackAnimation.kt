package architecture.resonator_combat_framework.animation

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.init.AnimationPropertys
import architecture.resonator_combat_framework.module.collision.CollisionEntry
import architecture.resonator_combat_framework.module.collision.CollisionSystem
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import org.joml.Matrix4f

class AttackAnimation
@JvmOverloads
constructor(
	id: ResourceLocation,
	animationId: ResourceLocation,
	stateModifiers: Map<ResourceLocation, Boolean> = emptyMap(),
	/** 攻击阶段列表 */
	val phases: List<AttackPhase> = emptyList(),
) : ActionAnimation(id, animationId, stateModifiers) {

	constructor(
		id: ResourceLocation,
		phases: List<AttackPhase> = emptyList()
	) : this(id, id, emptyMap(), phases)

	constructor(
		id: ResourceLocation,
		vararg phases: AttackPhase = emptyArray()
	) : this(id, phases.toList())

	// ===== 阶段查询 =====

	/**
	 * 获取当前动画时间下活跃的攻击阶段。
	 */
	fun getActivePhases(animTime: Float): List<AttackPhase> =
		phases.filter { animTime >= it.startTime && animTime < it.endTime }

	// ===== 碰撞回调 =====

	/**
	 * 命中实体时的回调，由 [CollisionBridge] 在检测到碰撞时自动调用。
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

	fun getDamage(
		attacker: LivingEntity,
		phase: AttackPhase
	): Double {
		return attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) *
			getProperty(AnimationPropertys.DAMAGE_MULTIPLIER, phase).orElse(1.0f)!!
	}

	/**
	 * 碰撞体更新钩子，在 [tickAdvance] 写入碰撞体前调用。
	 * 子类可在此修改碰撞体的属性（大小、位置、效果标志、射线模式等）。
	 */
	fun onColliderUpdate(
		entity: Entity,
		animTime: Float,
		proxyModel: ProxyModel,
		brModel: BrModel,
		mergedProxy: ProxyModel,
		controller: IEntityAnimationController<*>,
	) {
	}

	// ===== 生命周期 =====

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
		proxyModel: ProxyModel,
		brModel: BrModel,
		mergedProxy: ProxyModel,
		controller: IEntityAnimationController<*>
	) {
		starts(entity, animTime, proxyModel, brModel, mergedProxy, controller)
	}

	fun starts(
		entity: Entity,
		animTime: Float,
		proxyModel: ProxyModel,
		brModel: BrModel,
		mergedProxy: ProxyModel,
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
			proxyModel,
			brModel,
			mergedProxy,
			controller,
			currentPhaseIndices,
			startedIndices,
			endedIndices
		)
	}

	fun collider(
		entity: Entity,
		animTime: Float,
		proxyModel: ProxyModel,
		brModel: BrModel,
		mergedProxy: ProxyModel,
		controller: IEntityAnimationController<*>,
		activeIndices: Set<Int>,
		startedIndices: Set<Int>,
		endedIndices: Set<Int>,
	) {
		RcfEventHooks.AnimationColliderPre(controller, entity, animTime, proxyModel, brModel, mergedProxy)

		onColliderUpdate(entity, animTime, proxyModel, brModel, mergedProxy, controller)

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

		RcfEventHooks.AnimationColliderPost(controller, entity, animTime, proxyModel, brModel, mergedProxy)
	}

	private companion object {
		/** 阶段分组 ID：每阶段独立 groupId，支持按阶段增删碰撞体 */
		private fun phaseGroupId(animationId: ResourceLocation, phaseIdx: Int): ResourceLocation =
			rlOf(animationId.namespace, "${animationId.path}_${phaseIdx}")
	}
}
