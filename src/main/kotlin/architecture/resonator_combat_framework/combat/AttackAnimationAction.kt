package architecture.resonator_combat_framework.combat

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.module.collision.CollisionEntry
import architecture.resonator_combat_framework.module.collision.CollisionSystem
import architecture.resonator_combat_framework.module.entity_animation.IAnimationProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationDef
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.AnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.model.GeometryModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionSequence
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionState
import architecture.resonator_combat_framework.module.entity_state_machine.combat.AttackPhase
import architecture.resonator_combat_framework.module.entity_state_machine.combat.InterruptData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import org.joml.Matrix4f
import java.util.function.Supplier

/**
 * 攻击动画动作。
 *
 * 扩展 [AnimationAction]，为攻击动画提供前摇（WINDUP）、执行（ACTIVE）、后摇（RECOVERY）三个阶段的时间划分，
 * 攻击阶段（[AttackPhase]）的碰撞体管理、伤害计算等战斗逻辑在此类中处理。
 *
 * @property phases 攻击阶段列表，每个阶段定义了碰撞体与时间范围
 * @property activeTick 执行阶段 tick 数
 * @property damageMultiplier 伤害倍率
 */
class AttackAnimationAction(
	id: ResourceLocation,
	animation: Supplier<out AnimationDef?>,
	controllerId: ResourceLocation?,
	fadeInTick: Int = 1,
	windupTick: Int = 0,
	/** 攻击阶段列表 */
	val phases: List<AttackPhase> = emptyList(),
	/** 执行阶段 tick 数 */
	val activeTick: Int = 4,
	recoveryTick: Int = 2,
	fadeOutTick: Int = 1,
	interruptData: InterruptData = InterruptData(),
	stateModifiers: Map<ResourceLocation, Boolean> = emptyMap(),
	floatModifiers: Map<ResourceLocation, Float> = emptyMap(),
	weight: Int = 2500,
	/** 伤害倍率 */
	val damageMultiplier: Float = 1.0f
) : AnimationAction(
	id,
	animation,
	controllerId,
	fadeInTick,
	windupTick + activeTick + recoveryTick,
	fadeOutTick,
	interruptData,
	stateModifiers,
	floatModifiers,
	weight
) {
	/** 前摇总 tick 数（含淡入） */
	val totalWindupTick: Int = fadeInTick + windupTick

	/** 后摇总 tick 数（含淡出） */
	val totalRecoveryTick: Int = recoveryTick + fadeOutTick

	override fun getState(time: Float, entity: LivingEntity): ActionState {
		val t = time * 20f // 秒转 tick
		return when {
			t < 0f -> ActionState.IDLE
			t < totalWindupTick.toFloat() -> ActionState.WINDUP
			t < (totalWindupTick + activeTick).toFloat() -> ActionState.ACTIVE
			t < timeLength * 20f -> ActionState.RECOVERY
			else -> ActionState.IDLE
		}
	}

	// ===== 碰撞体管理 =====

	override fun onTick(entity: LivingEntity, actionSequence: ActionSequence?, time: Float) {
		super.onTick(entity, actionSequence, time)
		if (entity !is IAnimationProvider) return
		val controller = getController(entity) as? AnimationController<*> ?: return
		tickCollision(controller)
	}

	override fun onEnd(entity: LivingEntity, actionSequence: ActionSequence?) {
		// 先清理碰撞体
		cleanupColliders(entity)
		super.onEnd(entity, actionSequence)
	}

	override fun onForcedEnd(entity: LivingEntity, actionSequence: ActionSequence?) {
		cleanupColliders(entity)
		super.onForcedEnd(entity, actionSequence)
	}

	/**
	 * 碰撞体管理入口，每 tick 调用。
	 * 对比当前动画时间与各阶段的时间窗口，判断哪些阶段开始、结束或保持活跃，
	 * 然后触发对应的事件钩子，并更新碰撞体状态。
	 */
	@Suppress("UNCHECKED_CAST")
	private fun tickCollision(controller: AnimationController<*>) {
		val entity = controller.manager.holder
		if (entity !is LivingEntity) return
		val animTime = controller.currentAnimTime
		val mergedProxy = controller.manager.mergedPose
		val brModel = controller.manager.brModel
		val poseData = controller.poseData

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
				existingPhaseIndices.forEach {
					CollisionSystem.getData(entity).removeColliders(phaseGroupId(id, it))
				}
			}
			return
		}

		val startedIndices = currentPhaseIndices - existingPhaseIndices
		val endedIndices = existingPhaseIndices - currentPhaseIndices

		startedIndices.forEach { RcfEventHooks.AnimationPhaseStart(controller, phases[it]) }
		endedIndices.forEach { RcfEventHooks.AnimationPhaseEnd(controller, phases[it]) }

		updateColliders(
			entity, animTime, poseData, brModel, mergedProxy, controller,
			currentPhaseIndices, startedIndices, endedIndices
		)
	}

	/**
	 * 碰撞体更新核心逻辑。
	 */
	@Suppress("UNCHECKED_CAST")
	private fun updateColliders(
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

		// 移除结束阶段的碰撞体
		for (phaseIdx in endedIndices) {
			data.removeColliders(phaseGroupId(id, phaseIdx))
		}

		val entityPos = entity.position()
		val bodyRot = -entity.getPreciseBodyRotation(1.0f) * (Math.PI.toFloat() / 180f)

		// 处理活跃阶段：新阶段创建、持续阶段原地更新 worldMatrix
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

	/**
	 * 碰撞体更新钩子，供子类覆写以自定义碰撞体属性。
	 */
	protected open fun onColliderUpdate(
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: GeometryModel,
		mergedProxy: PoseData,
		controller: IEntityAnimationController<*>,
	) {
	}

	/**
	 * 清理所有阶段分组的碰撞体及命中记录。
	 */
	private fun cleanupColliders(entity: LivingEntity) {
		val data = CollisionSystem.getData(entity)
		for (phaseIdx in phases.indices) {
			data.removeGroup(phaseGroupId(id, phaseIdx))
		}
	}

	// ===== 伤害计算 =====

	/**
	 * 命中实体时的伤害回调，由碰撞系统在检测到碰撞时自动调用。
	 */
	fun onHurtEntity(attacker: LivingEntity, target: LivingEntity, phase: AttackPhase): Boolean {
		val amount = getDamage(attacker).toFloat()
		val source = if (attacker is Player) {
			attacker.damageSources().playerAttack(attacker)
		} else {
			attacker.damageSources().mobAttack(attacker)
		}
		return target.hurt(source, amount)
	}

	/**
	 * 计算攻击伤害值。
	 */
	fun getDamage(attacker: LivingEntity): Double {
		return attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * damageMultiplier
	}

	private companion object {
		/** 阶段分组 ID：每阶段独立 groupId */
		private fun phaseGroupId(actionId: ResourceLocation, phaseIdx: Int): ResourceLocation =
			rlOf(actionId.namespace, "${actionId.path}_${phaseIdx}")
	}
}
