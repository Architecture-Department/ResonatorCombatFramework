package architecture.resonator_combat_framework.combat

import architecture.goldenboughs_lib.util.PoseStack
import architecture.goldenboughs_lib.util.toRadians
import architecture.goldenboughs_lib.util.translate
import architecture.resonator_combat_framework.animation.IAnimationProvider
import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.state.EntityStateHolder
import com.mojang.math.Axis
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import org.joml.Matrix4fc

class AttackAnimationAction(
	id: ResourceLocation,
	animationId: ResourceLocation,
	controllerId: ResourceLocation?,
	fadeInTimeLength: Float = 1f / 20f,
	/**
	 * 前摇
	 */
	val windupTimeLength: Float = 0f,
	/**
	 * 效果
	 */
	val activeTimeLength: Float = 4f / 20f,
	/**
	 * 后摇
	 */
	val recoveryTimeLength: Float = 2f / 20f,
	fadeOutTimeLength: Float = 1f / 20f,
	interruptData: InterruptData = InterruptData(),
	weight: Int = 2500,
	vararg val phases: AttackActionPhase,
) : AnimationAction(
	id, animationId, controllerId,
	fadeInTimeLength,
	windupTimeLength + activeTimeLength + recoveryTimeLength,
	fadeOutTimeLength, interruptData, weight
) {

	init {

	}

	override fun getState(time: Float, entity: LivingEntity): ActionState = when {
		time < 0f -> ActionState.IDLE
		time < fadeInTimeLength + windupTimeLength -> ActionState.WINDUP
		time < fadeInTimeLength + windupTimeLength + activeTimeLength -> ActionState.ACTIVE
		time < fadeInTimeLength + windupTimeLength + activeTimeLength + recoveryTimeLength + fadeOutTimeLength -> ActionState.RECOVERY
		else -> ActionState.IDLE
	}

	fun isWindup(time: Float): Boolean {
		return 0f < time && time <= fadeInTimeLength + windupTimeLength
	}

	fun isRecovery(time: Float): Boolean {
		return fadeInTimeLength + windupTimeLength + activeTimeLength < time && time <= fadeInTimeLength + windupTimeLength + activeTimeLength + recoveryTimeLength
	}

	@Suppress("DuplicatedCode")
	fun applyPhaseModifiers(entity: LivingEntity, startedIndices: Set<Int>, endedIndices: Set<Int>) {
		if (!EntityStateHolder.has(entity)) return
		for (idx in endedIndices) {
			getPhase(idx).resetState(entity)
		}
		for (idx in startedIndices) {
			getPhase(idx).applyState(entity)
		}
	}

	override fun resetState(entity: LivingEntity) {
		for (phase in phases) {
			phase.resetState(entity)
		}
		super.resetState(entity)
	}

	override fun isMove(time: Float, controller: ActionController, entity: LivingEntity): Boolean {
		return when (getState(time, entity)) {
			ActionState.ACTIVE -> false
			ActionState.EMPTY -> true
			ActionState.WINDUP -> false
			ActionState.RECOVERY -> false
			ActionState.IDLE -> true
		}
	}

	fun getDamageSource(attacker: LivingEntity, phase: AttackActionPhase): DamageSource =
		if (attacker is Player) attacker.damageSources().playerAttack(attacker)
		else attacker.damageSources().mobAttack(attacker)

	fun getDamage(attacker: LivingEntity, phase: AttackActionPhase): Float =
		(attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * phase.damageMultiplier).toFloat()

	fun processActivePhases(
		actionController: ActionController,
		pose: Matrix4fc,
		manager: AnimationControllerManager<*>,
		indices: Set<Int>,
		record: AttackHitRecord,
		attacker: LivingEntity,
	) {
		val actionControllerId = actionController.id
		val brModel = manager.brModel
		for (idx in indices) {
			val phase = getPhase(idx)
			val tried = record.getTried(actionControllerId, idx)
			val hit = record.getHit(actionControllerId, idx)
			if (hit.size >= phase.maxStrikes) continue

			for (pair in phase.colliders) {
				val boneName = pair.boneName
				if (!brModel.bones.containsKey(boneName)) continue
				val multi = MultiCollider(pair.collider, phase.colliderCount)
				val function = { partialTick: Float ->
					brModel.computeLocatorGlobalMatrix(boneName, manager.getInterpolatedProxy(partialTick), true)
				}
				val results = multi.updateAndSelectCollideEntity(pose, function, attacker)

				for (target in results) {
					if (hit.size >= phase.maxStrikes) break
					if (!tried.add(target.uuid)) continue

					val damageAmount = getDamage(attacker, phase)
					val source = getDamageSource(attacker, phase)
					if (target.hurt(source, damageAmount)) {
						hit.add(target.uuid)
					}
				}
			}
		}
	}

	fun firePhaseEvents(
		controller: IEntityAnimationController<*>,
		started: Set<Int>, ended: Set<Int>,
		attacker: LivingEntity,
	) {
		started.forEach { RcfEventHooks.animationPhaseStart(controller, getPhase(it)) }
		ended.forEach { RcfEventHooks.animationPhaseEnd(controller, getPhase(it)) }
		applyPhaseModifiers(attacker, started, ended)
	}

	override fun onTick(
		attacker: LivingEntity,
		actionController: ActionController,
		actionSequence: ActionSequence?, time: Float,
	) {
		super.onTick(attacker, actionController, actionSequence, time)
		if (attacker !is IAnimationProvider) return
		val controller = getAnimationController(attacker) ?: return
		val record = AttackHitRecord.of(attacker)

		val current = getCurrentPhaseIndex(time)
		val actionControllerId = actionController.id
		val started = current - record.getActivePhases(actionControllerId)
		val ended = (record.getActivePhases(actionControllerId) - current).filter { it in phases.indices }.toSet()

		if (current.isNotEmpty() && !attacker.level().isClientSide) {
			val poseStack = PoseStack()
			poseStack.pushPose()
			poseStack.translate(attacker.position())
			poseStack.mulPose(Axis.YP.rotation(-attacker.getPreciseBodyRotation(1.0f).toRadians()))
			processActivePhases(actionController, poseStack.last().pose, controller.manager, current, record, attacker)
			poseStack.popPose()
		}

		ended.forEach { record.removeGroup(actionControllerId, it) }
		firePhaseEvents(controller, started, ended, attacker)
	}

	fun getCurrentPhaseIndex(time: Float): Set<Int> {
		return phases.indices.filter { idx ->
			val phase = getPhase(idx)
			time >= phase.startTime && time < phase.endTime
		}.toSet()
	}

	fun getPhase(idx: Int): AttackActionPhase {
		return phases[idx]
	}

	fun getCurrentPhase(time: Float): List<AttackActionPhase> {
		return getCurrentPhaseIndex(time).map { getPhase(it) }
	}

	override fun onEnd(entity: LivingEntity, actionController: ActionController, actionSequence: ActionSequence?) {
		AttackHitRecord.clear(entity)
		super.onEnd(entity, actionController, actionSequence)
	}

	override fun onForcedEnd(entity: LivingEntity, actionSequence: ActionSequence?) {
		AttackHitRecord.clear(entity)
		super.onForcedEnd(entity, actionSequence)
	}
}
