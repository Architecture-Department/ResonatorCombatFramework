package architecture.resonator_combat_framework.combat

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.collision.CollisionEntry
import architecture.resonator_combat_framework.module.collision.CollisionSystem
import architecture.resonator_combat_framework.module.animation.IAnimationProvider
import architecture.resonator_combat_framework.module.animation.AnimationDef
import architecture.resonator_combat_framework.module.animation.controller.AnimationController
import architecture.resonator_combat_framework.module.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.animation.model.GeometryModel
import architecture.resonator_combat_framework.module.animation.model.PoseData
import architecture.resonator_combat_framework.module.combat.ActionSequence
import architecture.resonator_combat_framework.module.combat.ActionState
import architecture.resonator_combat_framework.module.combat.BooleanStateProperty
import architecture.resonator_combat_framework.module.combat.FloatStateProperty
import architecture.resonator_combat_framework.module.combat.InterruptData
import architecture.resonator_combat_framework.util.RcfUtil
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
 * 扩展 [AnimationAction]，为攻击动画提供前摇（WINDUP）、执行（ACTIVE）、后摇（RECOVERY）三个阶段的时间划分。
 * 攻击阶段（[AttackActionPhase]）的碰撞体管理、伤害计算等战斗逻辑在此类中处理。
 * 状态修饰通过 [addProperty] 存入 [Action.properties] 或 [AttackActionPhase.addProperty]，
 * 以 [BooleanStateProperty] 或 [FloatStateProperty] 为键，运行时自动应用。
 *
 * @property activeTick 执行阶段 tick 数
 * @property damageMultiplier 伤害倍率
 * @property phases 攻击阶段列表
 */
class AttackAnimationAction(
	id: ResourceLocation,
	animation: Supplier<AnimationDef?>,
	controllerId: ResourceLocation?,
	fadeInTick: Int = 1,
	windupTick: Int = 0,
	/** 执行阶段 tick 数 */
	val activeTick: Int = 4,
	recoveryTick: Int = 2,
	fadeOutTick: Int = 1,
	interruptData: InterruptData = InterruptData(),
	weight: Int = 2500,
	/** 伤害倍率 */
	val damageMultiplier: Float = 1.0f,
	/** 攻击阶段列表 */
	val phases: List<AttackActionPhase> = emptyList(),
) : AnimationAction(
	id,
	animation,
	controllerId,
	fadeInTick,
	windupTick + activeTick + recoveryTick,
	fadeOutTick,
	interruptData,
	weight
) {
	/** 前摇总 tick 数（含淡入） */
	val totalWindupTick: Int = fadeInTick + windupTick

	/** 后摇总 tick 数（含淡出） */
	val totalRecoveryTick: Int = recoveryTick + fadeOutTick

	override fun getState(time: Float, entity: LivingEntity): ActionState {
		val t = time * 20f
		return when {
			t < 0f -> ActionState.IDLE
			t < totalWindupTick.toFloat() -> ActionState.WINDUP
			t < (totalWindupTick + activeTick).toFloat() -> ActionState.ACTIVE
			t < timeLength * 20f -> ActionState.RECOVERY
			else -> ActionState.IDLE
		}
	}

	// ===== 碰撞体管理 =====

	/**
	 * 阶段状态修饰管理：从阶段属性的 [BooleanStateProperty]/[FloatStateProperty] 键中读取修饰，
	 * 阶段开始时应用，结束时恢复。
	 */
	@Suppress("UNCHECKED_CAST")
	private fun applyPhaseModifiers(entity: LivingEntity, startedIndices: Set<Int>, endedIndices: Set<Int>) {
		val stateHolder = entity.getExistingData(RcfAttachmentTypes.STATE_HOLDER).orElse(null) ?: return
		for (idx in endedIndices) {
			val phase = phases[idx]
			val boolKeys = phase.getAllProperties().keys.filterIsInstance<BooleanStateProperty>()
			if (boolKeys.isNotEmpty()) {
				stateHolder.applyStateModifiers(boolKeys.associate { RcfUtil.modRl(it.name) to false })
			}
			val floatKeys = phase.getAllProperties().keys.filterIsInstance<FloatStateProperty>()
			if (floatKeys.isNotEmpty()) {
				stateHolder.applyFloatModifiers(floatKeys.associate { RcfUtil.modRl(it.name) to 0f })
			}
		}
		for (idx in startedIndices) {
			val phase = phases[idx]
			val boolMods = phase.getStateModifiers()
			if (boolMods.isNotEmpty()) {
				stateHolder.applyStateModifiers(boolMods.mapKeys { RcfUtil.modRl(it.key.name) })
			}
			val floatMods = phase.getFloatModifiers()
			if (floatMods.isNotEmpty()) {
				stateHolder.applyFloatModifiers(floatMods.mapKeys { RcfUtil.modRl(it.key.name) })
			}
		}
	}

	override fun onTick(entity: LivingEntity, actionSequence: ActionSequence?, time: Float) {
		super.onTick(entity, actionSequence, time)
		if (entity !is IAnimationProvider) return
		val controller = getController(entity) as? AnimationController<*> ?: return
		tickCollision(controller)
	}

	override fun onEnd(entity: LivingEntity, actionSequence: ActionSequence?) {
		cleanupColliders(entity)
		super.onEnd(entity, actionSequence)
	}

	override fun onForcedEnd(entity: LivingEntity, actionSequence: ActionSequence?) {
		cleanupColliders(entity)
		super.onForcedEnd(entity, actionSequence)
	}

	@Suppress("UNCHECKED_CAST")
	private fun tickCollision(controller: AnimationController<*>) {
		val entity = controller.manager.holder
		if (entity !is LivingEntity) return
		val animTime = controller.currentAnimTime
		val mergedProxy = controller.manager.mergedPose
		val brModel = controller.manager.brModel
		val poseData = controller.poseData

		val existingPhaseIndices = phases.indices
			.filter { CollisionSystem.getData(entity).getColliders(phaseGroupId(id, it)) != null }
			.toSet()

		val currentPhaseIndices = phases.indices.filter { phaseIdx ->
			val phase = phases[phaseIdx]
			animTime >= phase.startTime && animTime < phase.endTime
		}.toSet()

		if (currentPhaseIndices.isEmpty()) {
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

		applyPhaseModifiers(entity, startedIndices, endedIndices)

		updateColliders(
			entity, animTime, poseData, brModel, mergedProxy, controller,
			currentPhaseIndices, startedIndices, endedIndices
		)
	}

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
		for (phaseIdx in endedIndices) data.removeColliders(phaseGroupId(id, phaseIdx))

		val entityPos = entity.position()
		val bodyRot = -entity.getPreciseBodyRotation(1.0f) * (Math.PI.toFloat() / 180f)

		for (phaseIdx in activeIndices) {
			val phase = phases[phaseIdx]
			val gid = phaseGroupId(id, phaseIdx)

			if (phaseIdx in startedIndices) {
				for (pair in phase.colliders) {
					if (mergedProxy.getBone(pair.boneName) == null) continue
					val worldMatrix = Matrix4f()
						.translate(entityPos.x.toFloat(), entityPos.y.toFloat(), entityPos.z.toFloat())
						.rotateY(bodyRot)
						.mul(brModel.computeBoneGlobalMatrix(pair.boneName, mergedProxy, true))
					data.addCollider(CollisionEntry(id = id, shape = pair.collider, worldMatrix = worldMatrix, groupId = gid))
				}
				continue
			}

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

	protected open fun onColliderUpdate(
		entity: Entity, animTime: Float, poseData: PoseData,
		brModel: GeometryModel, mergedProxy: PoseData,
		controller: IEntityAnimationController<*>,
	) {}

	private fun cleanupColliders(entity: LivingEntity) {
		val data = CollisionSystem.getData(entity)
		for (phaseIdx in phases.indices) data.removeGroup(phaseGroupId(id, phaseIdx))
	}

	// ===== 伤害计算 =====

	fun onHurtEntity(attacker: LivingEntity, target: LivingEntity, phase: AttackActionPhase): Boolean {
		val amount = getDamage(attacker).toFloat()
		val source = if (attacker is Player) attacker.damageSources().playerAttack(attacker)
		else attacker.damageSources().mobAttack(attacker)
		return target.hurt(source, amount)
	}

	fun getDamage(attacker: LivingEntity): Double =
		attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * damageMultiplier

	private companion object {
		private fun phaseGroupId(actionId: ResourceLocation, phaseIdx: Int): ResourceLocation =
			rlOf(actionId.namespace, "${actionId.path}_${phaseIdx}")
	}
}
