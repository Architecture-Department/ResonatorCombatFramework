package architecture.resonator_combat_framework.combat

import architecture.goldenboughs_lib.util.PoseStack
import architecture.goldenboughs_lib.util.toRadians
import architecture.goldenboughs_lib.util.translate
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.animation.AnimationDef
import architecture.resonator_combat_framework.module.animation.IAnimationProvider
import architecture.resonator_combat_framework.module.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.module.animation.registry.AnimationDefRegistry
import architecture.resonator_combat_framework.module.collision.MultiCollider
import architecture.resonator_combat_framework.module.combat.*
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
	fadeInTick: Int = 1,
	windupTick: Int = 0,
	activeTick: Int = 4,
	recoveryTick: Int = 2,
	fadeOutTick: Int = 1,
	interruptData: InterruptData = InterruptData(),
	weight: Int = 2500,
	vararg val phases: AttackActionPhase,
) : AnimationAction(
	id, animation, controllerId,
	fadeInTick, windupTick + activeTick + recoveryTick, fadeOutTick, interruptData, weight
) {
	constructor(
		id: ResourceLocation, animationId: ResourceLocation, controllerId: ResourceLocation?,
		fadeInTick: Int = 1, windupTick: Int = 0, activeTick: Int = 4, recoveryTick: Int = 2,
		fadeOutTick: Int = 1, interruptData: InterruptData = InterruptData(), weight: Int = 2500,
		vararg phases: AttackActionPhase,
	) : this(
		id, AnimationDefRegistry.get(animationId)!!, controllerId,
		fadeInTick, windupTick, activeTick, recoveryTick, fadeOutTick, interruptData, weight, *phases,
	)

	override val fadeInTime = fadeInTick / 20f
	val windupTime = (fadeInTick + windupTick) / 20f
	override val activeTime = (fadeInTick + animationTick) / 20f
	override val durationTime = (fadeInTick + animationTick + fadeOutTick) / 20f

	override fun getState(time: Float, entity: LivingEntity): ActionState = when {
		time < 0f -> ActionState.IDLE
		time < windupTime -> ActionState.WINDUP
		time < activeTime -> ActionState.ACTIVE
		time < durationTime * 20f -> ActionState.RECOVERY
		else -> ActionState.IDLE
	}

	// ===== 阶段状态修饰 =====

	fun applyPhaseModifiers(entity: LivingEntity, startedIndices: Set<Int>, endedIndices: Set<Int>) {
		val stateHolder = entity.getExistingData(RcfAttachmentTypes.STATE_HOLDER).orElse(null) ?: return
		for (idx in endedIndices) {
			val phase = phases[idx]
			val boolKeys = phase.getAllProperties().keys.filterIsInstance<BooleanStateProperty>()
			if (boolKeys.isNotEmpty()) stateHolder.applyStateModifiers(boolKeys.associate { RcfUtil.modRl(it.name) to false })
			val floatKeys = phase.getAllProperties().keys.filterIsInstance<FloatStateProperty>()
			if (floatKeys.isNotEmpty()) stateHolder.applyFloatModifiers(floatKeys.associate { RcfUtil.modRl(it.name) to 0f })
		}
		for (idx in startedIndices) {
			val phase = phases[idx]
			val boolMods = phase.getStateModifiers()
			if (boolMods.isNotEmpty()) stateHolder.applyStateModifiers(boolMods.mapKeys { RcfUtil.modRl(it.key.name) })
			val floatMods = phase.getFloatModifiers()
			if (floatMods.isNotEmpty()) stateHolder.applyFloatModifiers(floatMods.mapKeys { RcfUtil.modRl(it.key.name) })
		}
	}

	// ===== 碰撞检测 =====

	fun processActivePhases(
		pose: Matrix4fc,
		manager: AnimationControllerManager<*>,
		indices: Set<Int>,
		record: AttackHitRecord,
		attacker: LivingEntity,
	) {
		val brModel = manager.brModel
		for (idx in indices) {
			val phase = phases[idx]
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

				// Epic Fight 风格：只处理未尝试过的实体
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

	fun getDamageSource(attacker: LivingEntity, phase1: AttackActionPhase): DamageSource =
		if (attacker is Player) attacker.damageSources().playerAttack(attacker)
		else attacker.damageSources().mobAttack(attacker)

	fun getDamage(attacker: LivingEntity, phase: AttackActionPhase): Float =
		(attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * phase.damageMultiplier).toFloat()

	fun firePhaseEvents(
		controller: IEntityAnimationController<*>,
		started: Set<Int>, ended: Set<Int>,
		attacker: LivingEntity,
	) {
		started.forEach { RcfEventHooks.animationPhaseStart(controller, phases[it]) }
		ended.forEach { RcfEventHooks.animationPhaseEnd(controller, phases[it]) }
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

		val current = phases.indices.filter { idx ->
			time >= phases[idx].startTime && time <= phases[idx].endTime
		}.toSet()
		val started = current - record.activePhases
		val ended = (record.activePhases - current).filter { it in phases.indices }.toSet()

		val poseStack = PoseStack()
		poseStack.pushPose()
		poseStack.translate(attacker.position())
		poseStack.mulPose(Axis.YP.rotation(-attacker.getPreciseBodyRotation(1.0f).toRadians()))
		processActivePhases(poseStack.last().pose, controller.manager, current, record, attacker)
		poseStack.popPose()

		ended.forEach { record.removeGroup(it) }
		firePhaseEvents(controller, started, ended, attacker)
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
