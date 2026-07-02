package architecture.resonator_combat_framework.core

import architecture.goldenboughs_lib.util.Value
import architecture.resonator_combat_framework.animation.AttackPhase
import architecture.resonator_combat_framework.event.AnimationColliderEvent
import architecture.resonator_combat_framework.module.collision.CollisionEntityData
import architecture.resonator_combat_framework.module.collision.CollisionEntry
import architecture.resonator_combat_framework.module.collision.event.CollisionEntityEvent
import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationDef
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayConfig
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.model.GeometryModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.module.entity_animation.event.*
import architecture.resonator_combat_framework.module.entity_state_machine.combat.Action
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionState
import architecture.resonator_combat_framework.module.entity_state_machine.event.CombatActionEvent
import architecture.resonator_combat_framework.module.entity_state_machine.event.CombatActionEvent.Changed.Type
import architecture.resonator_combat_framework.module.entity_state_machine.event.CombatEvent
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import net.minecraft.core.particles.ParticleType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.neoforged.neoforge.common.NeoForge
import org.joml.Vector3d

object RcfEventHooks {

	// ===== Combat (entity_state_machine) =====

	@JvmStatic
	fun CombatActionStart(holder: EntityStateHolder<*>, entity: LivingEntity, action: Action) {
		NeoForge.EVENT_BUS.post(CombatActionEvent.Start(holder, entity, action))
	}

	@JvmStatic
	fun CombatActionTickPre(holder: EntityStateHolder<*>, entity: LivingEntity, action: Action): Boolean {
		return NeoForge.EVENT_BUS.post(CombatActionEvent.Tick.Pre(holder, entity, action)).isCanceled
	}

	@JvmStatic
	fun CombatActionTickPost(holder: EntityStateHolder<*>, entity: LivingEntity, action: Action) {
		NeoForge.EVENT_BUS.post(CombatActionEvent.Tick.Post(holder, entity, action))
	}

	@JvmStatic
	fun CombatActionEnd(holder: EntityStateHolder<*>, entity: LivingEntity, action: Action) {
		NeoForge.EVENT_BUS.post(CombatActionEvent.End(holder, entity, action))
	}

	@JvmStatic
	fun CombatActionInterruptible(
		holder: EntityStateHolder<*>, entity: LivingEntity, action: Action, target: Action, value: Boolean
	): Boolean {
		val event = NeoForge.EVENT_BUS.post(CombatActionEvent.Interruptible(holder, entity, action, target, value, value))
		return if (event.isCanceled) value else event.newValue
	}

	@JvmStatic
	fun CombatActionChanged(
		holder: EntityStateHolder<*>, entity: LivingEntity, oldValue: Action?, newValue: Action?, type: Type
	): CombatActionEvent.Changed {
		return NeoForge.EVENT_BUS.post(CombatActionEvent.Changed(holder, entity, oldValue, newValue, type))
	}

	@JvmStatic
	fun CombatActionStateChanged(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		oldValue: ActionState,
		newValue: ActionState
	) {
		NeoForge.EVENT_BUS.post(CombatEvent.ActionStateChanged(holder, entity, oldValue, newValue))
	}

	// ===== Animation Trigger =====

	@JvmStatic
	fun AnimationTriggerPre(controller: IEntityAnimationController<*>, anim: AnimationDef, config: PlayConfig) {
		NeoForge.EVENT_BUS.post(AnimationTriggerEvent.Pre(controller, anim, config))
	}

	@JvmStatic
	fun AnimationTriggerPost(
		controller: IEntityAnimationController<*>,
		anim: AnimationDef,
		config: PlayConfig
	) {
		NeoForge.EVENT_BUS.post(AnimationTriggerEvent.Post(controller, anim, config))
	}

	// ===== Animation Complete =====

	@JvmStatic
	fun AnimationComplete(controller: IEntityAnimationController<*>) {
		NeoForge.EVENT_BUS.post(AnimationCompleteEvent(controller))
	}

	// ===== Animation Controller Tick =====

	@JvmStatic
	fun <T : Entity> AnimationControllerTickPre(
		id: ResourceLocation,
		controller: IEntityAnimationController<T>,
		mapper: IEntityAnimationMapperProvider<T, *>
	): Boolean {
		return NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickPre(id, controller, mapper)).isCanceled
	}

	@JvmStatic
	fun <T : Entity> AnimationControllerTickPost(
		id: ResourceLocation,
		controller: IEntityAnimationController<T>,
		mapper: IEntityAnimationMapperProvider<T, *>
	) {
		NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickPost(id, controller, mapper))
	}

	@JvmStatic
	fun <T : Entity> AnimationControllerTickHandlerPre(
		id: ResourceLocation,
		controller: IEntityAnimationController<T>,
		mapper: IEntityAnimationMapperProvider<T, *>
	): Boolean {
		return NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickHandlerPre(id, controller, mapper)).isCanceled
	}

	@JvmStatic
	fun <T : Entity> AnimationControllerTickHandlerPost(
		id: ResourceLocation,
		controller: IEntityAnimationController<T>,
		mapper: IEntityAnimationMapperProvider<T, *>
	) {
		NeoForge.EVENT_BUS.post(AnimationControllerEvent.TickHandlerPost(id, controller, mapper))
	}

	// ===== Animation Collider =====

	@JvmStatic
	fun AnimationColliderPre(
		controller: IEntityAnimationController<*>,
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: GeometryModel,
		mergedProxy: PoseData
	) {
		NeoForge.EVENT_BUS.post(AnimationColliderEvent.Pre(controller, entity, animTime, poseData, brModel, mergedProxy))
	}

	@JvmStatic
	fun AnimationColliderPost(
		controller: IEntityAnimationController<*>,
		entity: Entity,
		animTime: Float,
		poseData: PoseData,
		brModel: GeometryModel,
		mergedProxy: PoseData
	) {
		NeoForge.EVENT_BUS.post(AnimationColliderEvent.Post(controller, entity, animTime, poseData, brModel, mergedProxy))
	}

	// ===== Animation Phase =====

	@JvmStatic
	fun AnimationPhaseStart(controller: IEntityAnimationController<*>, phase: AttackPhase) {
		NeoForge.EVENT_BUS.post(AnimationPhaseEvent.Start(controller, phase))
	}

	@JvmStatic
	fun AnimationPhaseEnd(controller: IEntityAnimationController<*>, phase: AttackPhase) {
		NeoForge.EVENT_BUS.post(AnimationPhaseEvent.End(controller, phase))
	}

	// ===== Collision =====

	@JvmStatic
	fun CollisionEntityCheck(
		attacker: Entity,
		entry: CollisionEntry,
		target: Entity,
		data: CollisionEntityData
	): CollisionEntityEvent.Check {
		return NeoForge.EVENT_BUS.post(CollisionEntityEvent.Check(attacker, entry, target, data))
	}

	@JvmStatic
	fun CollisionEntityHit(attacker: Entity, entry: CollisionEntry, target: Entity, data: CollisionEntityData) {
		NeoForge.EVENT_BUS.post(CollisionEntityEvent.Hit(attacker, entry, target, data))
	}

	// ===== Particle =====

	@JvmStatic
	fun AnimationParticlePre(
		controller: IEntityAnimationController<*>,
		locatorName: String,
		particleId: ResourceLocation,
		particle: Value<ParticleType<*>?>,
		rotate: Value<Vector3d>,
		pos: Value<Vector3d>
	): AnimationParticleEvent.Pre {
		return NeoForge.EVENT_BUS.post(
			AnimationParticleEvent.Pre(
				controller,
				locatorName,
				particleId,
				particle,
				rotate,
				pos
			)
		)
	}

	@JvmStatic
	fun AnimationParticlePost(
		controller: IEntityAnimationController<*>,
		locatorName: String,
		particleId: ResourceLocation,
		particle: ParticleType<*>?,
		rotate: Vector3d,
		pos: Vector3d
	) {
		NeoForge.EVENT_BUS.post(AnimationParticleEvent.Post(controller, locatorName, particleId, particle, rotate, pos))
	}

	// ===== Controller Register =====

	@JvmStatic
	fun <T : Entity> AnimationControllerRegister(): AnimationControllerRegisterEvent<T> {
		return NeoForge.EVENT_BUS.post(AnimationControllerRegisterEvent<T>())
	}
}
