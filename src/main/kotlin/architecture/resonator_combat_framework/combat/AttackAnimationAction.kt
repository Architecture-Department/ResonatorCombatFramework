package architecture.resonator_combat_framework.combat

import architecture.goldenboughs_lib.util.PoseStack
import architecture.goldenboughs_lib.util.toRadians
import architecture.goldenboughs_lib.util.translate
import architecture.resonator_combat_framework.animation.AnimationDef
import architecture.resonator_combat_framework.animation.IAnimationProvider
import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.collision.MultiCollider
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.registry.AnimationDefRegistry
import architecture.resonator_combat_framework.state_machine.holder.EntityStateHolder
import architecture.resonator_combat_framework.util.RcfUtil
import com.mojang.math.Axis
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import org.joml.Matrix4fc
import java.util.function.Supplier

class AttackAnimationAction(
	id: ResourceLocation,
	animation: Supplier<AnimationDef?>, controllerId: ResourceLocation?,
	val fadeInTime: Float = 1f / 20f,
	val windupTime: Float = 0f,
	val activeTime: Float = 4f / 20f,
	val recoveryTime: Float = 2f / 20f,
	val fadeOutTime: Float = 1f / 20f,
	interruptData: InterruptData = InterruptData(),
	weight: Int = 2500,
	vararg val phases: AttackActionPhase,
) : AnimationAction(
	id, animation, controllerId,
	fadeInTime, windupTime + activeTime + recoveryTime, fadeOutTime, interruptData, weight
) {
	constructor(
		id: ResourceLocation, animationId: ResourceLocation, controllerId: ResourceLocation?,
		fadeInTime: Float = 1f / 20f, windupTime: Float = 0f, activeTime: Float = 4f / 20f, recoveryTime: Float = 2f / 20f,
		fadeOutTime: Float = 1f / 20f, interruptData: InterruptData = InterruptData(), weight: Int = 2500,
		vararg phases: AttackActionPhase,
	) : this(
		id, AnimationDefRegistry.get(animationId)!!, controllerId,
		fadeInTime, windupTime, activeTime, recoveryTime, fadeOutTime, interruptData, weight, *phases,
	)

	override fun getState(time: Float, entity: LivingEntity): ActionState = when {
		time < 0f -> ActionState.IDLE
		time < fadeInTime + windupTime -> ActionState.WINDUP
		time < fadeInTime + windupTime + activeTime -> ActionState.ACTIVE
		time < fadeInTime + windupTime + activeTime + recoveryTime + fadeOutTime -> ActionState.RECOVERY
		else -> ActionState.IDLE
	}

	fun isWindup(time: Float): Boolean {
		return 0f < time && time <= fadeInTime + windupTime
	}

	fun isRecovery(time: Float): Boolean {
		return fadeInTime + windupTime + activeTime < time && time <= fadeInTime + windupTime + activeTime + recoveryTime
	}

	// ===== 阶段状态修饰 =====

	fun applyPhaseModifiers(entity: LivingEntity, startedIndices: Set<Int>, endedIndices: Set<Int>) {
		val stateHolder = entity.getExistingData(RcfAttachmentTypes.STATE_HOLDER).orElse(null) ?: return
		for (idx in endedIndices) {
			val phase = getPhase(idx)
			val boolKeys = phase.getAllProperties().keys.filterIsInstance<BooleanStateProperty>()
			if (boolKeys.isNotEmpty()) stateHolder.applyStateModifiers(boolKeys.associate { RcfUtil.modRl(it.name) to false })
			val floatKeys = phase.getAllProperties().keys.filterIsInstance<FloatStateProperty>()
			if (floatKeys.isNotEmpty()) stateHolder.applyFloatModifiers(floatKeys.associate { RcfUtil.modRl(it.name) to 0f })
		}
		for (idx in startedIndices) {
			val phase = getPhase(idx)
			val boolMods = phase.getStateModifiers()
			if (boolMods.isNotEmpty()) stateHolder.applyStateModifiers(boolMods.mapKeys { RcfUtil.modRl(it.key.name) })
			val floatMods = phase.getFloatModifiers()
			if (floatMods.isNotEmpty()) stateHolder.applyFloatModifiers(floatMods.mapKeys { RcfUtil.modRl(it.key.name) })
		}
	}

	override fun isMove(time: Float, holder: EntityStateHolder<*>, entity: LivingEntity): Boolean {
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
		pose: Matrix4fc,
		manager: AnimationControllerManager<*>,
		indices: Set<Int>,
		record: AttackHitRecord,
		attacker: LivingEntity,
	) {
		val brModel = manager.brModel
		for (idx in indices) {
			val phase = getPhase(idx)
			val tried = record.getTried(idx)
			val hit = record.getHit(idx)
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
		attacker: LivingEntity, actionController: ActionController,
		actionSequence: ActionSequence?, time: Float,
	) {
		super.onTick(attacker, actionController, actionSequence, time)
		if (attacker !is IAnimationProvider) return
		val controller = getAnimationController(attacker) ?: return
		val record = attacker.getData(RcfAttachmentTypes.ATTACK_HIT_RECORD)

		val current = getCurrentPhaseIndex(time)
		val started = current - record.activePhases
		val ended = (record.activePhases - current).filter { it in phases.indices }.toSet()

		if (current.isNotEmpty() && !attacker.level().isClientSide) {
			val poseStack = PoseStack()
			poseStack.pushPose()
			poseStack.translate(attacker.position())
			poseStack.mulPose(Axis.YP.rotation(-attacker.getPreciseBodyRotation(1.0f).toRadians()))
			processActivePhases(poseStack.last().pose, controller.manager, current, record, attacker)
			poseStack.popPose()
		}

		ended.forEach { record.removeGroup(it) }
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
		if (entity.hasData(RcfAttachmentTypes.ATTACK_HIT_RECORD)) entity.getData(RcfAttachmentTypes.ATTACK_HIT_RECORD)
			.clear()
		super.onEnd(entity, actionController, actionSequence)
	}

	override fun onForcedEnd(entity: LivingEntity, actionSequence: ActionSequence?) {
		if (entity.hasData(RcfAttachmentTypes.ATTACK_HIT_RECORD)) entity.getData(RcfAttachmentTypes.ATTACK_HIT_RECORD)
			.clear()
		super.onForcedEnd(entity, actionSequence)
	}
}
