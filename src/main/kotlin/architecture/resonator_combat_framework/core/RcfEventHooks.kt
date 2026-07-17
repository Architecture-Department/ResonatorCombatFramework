package architecture.resonator_combat_framework.core

import architecture.goldenboughs_lib.util.LazySupplier
import architecture.goldenboughs_lib.util.Value
import architecture.resonator_combat_framework.animation.AnimationDef
import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.data.PlayConfig
import architecture.resonator_combat_framework.animation.mapper.IEntityAnimationMapperProvider
import architecture.resonator_combat_framework.combat.Action
import architecture.resonator_combat_framework.combat.ActionController
import architecture.resonator_combat_framework.combat.ActionState
import architecture.resonator_combat_framework.combat.AttackActionPhase
import architecture.resonator_combat_framework.event.definition.*
import architecture.resonator_combat_framework.event.definition.ActionEvent.Changed.Type
import architecture.resonator_combat_framework.registry.KeyframeAnimationRegistry
import net.minecraft.core.particles.ParticleType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.neoforged.neoforge.common.NeoForge
import org.joml.Vector3d
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

object RcfEventHooks {

	// ===== Combat (entity_state_machine) =====

	@JvmStatic
	fun combatActionStart(controller: ActionController, entity: LivingEntity, action: Action) {
		NeoForge.EVENT_BUS.post(ActionEvent.Start(controller, entity, action))
	}

	@JvmStatic
	fun combatActionTickPre(controller: ActionController, entity: LivingEntity, action: Action): Boolean {
		return NeoForge.EVENT_BUS.post(ActionEvent.Tick.Pre(controller, entity, action)).isCanceled
	}

	@JvmStatic
	fun combatActionTickPost(controller: ActionController, entity: LivingEntity, action: Action) {
		NeoForge.EVENT_BUS.post(ActionEvent.Tick.Post(controller, entity, action))
	}

	@JvmStatic
	fun combatActionEnd(controller: ActionController, entity: LivingEntity, action: Action) {
		NeoForge.EVENT_BUS.post(ActionEvent.End(controller, entity, action))
	}

	@JvmStatic
	fun combatActionInterruptible(
		controller: ActionController, entity: LivingEntity, action: Action, target: Action, value: Boolean
	): Boolean {
		val event =
			NeoForge.EVENT_BUS.post(ActionEvent.Interruptible(controller, entity, action, target, value, value))
		return if (event.isCanceled) value else event.newValue
	}

	@JvmStatic
	fun combatActionChanged(
		controller: ActionController, entity: LivingEntity, oldValue: Action?, newValue: Action?, type: Type
	): ActionEvent.Changed {
		return NeoForge.EVENT_BUS.post(ActionEvent.Changed(controller, entity, oldValue, newValue, type))
	}

	@JvmStatic
	fun combatActionStateChanged(
		controller: ActionController,
		entity: LivingEntity,
		oldValue: ActionState,
		newValue: ActionState
	) {
		NeoForge.EVENT_BUS.post(ActionStateChangedEvent(controller, entity, oldValue, newValue))
	}

	// ===== Animation Trigger =====

	@JvmStatic
	fun animationTriggerPre(controller: IEntityAnimationController<*>, anim: AnimationDef, config: PlayConfig) {
		NeoForge.EVENT_BUS.post(TriggerEvent.Pre(controller, anim, config))
	}

	@JvmStatic
	fun animationTriggerPost(
		controller: IEntityAnimationController<*>,
		anim: AnimationDef,
		config: PlayConfig
	) {
		NeoForge.EVENT_BUS.post(TriggerEvent.Post(controller, anim, config))
	}

	// ===== Animation Complete =====

	@JvmStatic
	fun animationComplete(controller: IEntityAnimationController<*>) {
		NeoForge.EVENT_BUS.post(AnimationEvent.Complete(controller))
	}

	// ===== Animation Controller Tick =====

	@JvmStatic
	fun <T : Entity> animationControllerTickPre(
		id: ResourceLocation,
		controller: IEntityAnimationController<T>,
		mapper: IEntityAnimationMapperProvider<T, *>
	): Boolean {
		return NeoForge.EVENT_BUS.post(AnimationControllerEvent.Tick.Pre(id, controller, mapper)).isCanceled
	}

	@JvmStatic
	fun <T : Entity> animationControllerTickPost(
		id: ResourceLocation,
		controller: IEntityAnimationController<T>,
		mapper: IEntityAnimationMapperProvider<T, *>
	) {
		NeoForge.EVENT_BUS.post(AnimationControllerEvent.Tick.Post(id, controller, mapper))
	}

	@JvmStatic
	fun <T : Entity> animationControllerTickHandlerPre(
		id: ResourceLocation,
		controller: IEntityAnimationController<T>,
		mapper: IEntityAnimationMapperProvider<T, *>
	): Boolean {
		return NeoForge.EVENT_BUS.post(AnimationControllerEvent.HandlerTick.Pre(id, controller, mapper)).isCanceled
	}

	@JvmStatic
	fun <T : Entity> animationControllerTickHandlerPost(
		id: ResourceLocation,
		controller: IEntityAnimationController<T>,
		mapper: IEntityAnimationMapperProvider<T, *>
	) {
		NeoForge.EVENT_BUS.post(AnimationControllerEvent.HandlerTick.Post(id, controller, mapper))
	}

	// ===== Animation Phase =====

	@JvmStatic
	fun animationPhaseStart(controller: IEntityAnimationController<*>, phase: AttackActionPhase) {
		NeoForge.EVENT_BUS.post(PhaseEvent.Start(controller, phase))
	}

	@JvmStatic
	fun animationPhaseEnd(controller: IEntityAnimationController<*>, phase: AttackActionPhase) {
		NeoForge.EVENT_BUS.post(PhaseEvent.End(controller, phase))
	}

	// ===== Particle =====

	@JvmStatic
	fun <T : Entity> animationParticlePre(
		controller: IEntityAnimationController<T>,
		locatorName: String,
		particleId: ResourceLocation,
		particle: Value<ParticleType<*>?>,
		rotate: Value<Vector3d>,
		pos: Value<Vector3d>
	): ParticleEvent.Pre<T> {
		return NeoForge.EVENT_BUS.post(
			ParticleEvent.Pre(
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
	fun <T : Entity> animationParticlePost(
		controller: IEntityAnimationController<T>,
		locatorName: String,
		particleId: ResourceLocation,
		particle: ParticleType<*>?,
		rotate: Vector3d,
		pos: Vector3d
	) {
		NeoForge.EVENT_BUS.post(ParticleEvent.Post(controller, locatorName, particleId, particle, rotate, pos))
	}

	// ===== Controller Register =====

	@JvmStatic
	fun <T : Entity> animationControllerRegister(): AnimationControllerRegisterEvent<T> {
		return NeoForge.EVENT_BUS.post(AnimationControllerRegisterEvent<T>())
	}

	@JvmStatic
	fun animationDefRegister(): Map<ResourceLocation, LazySupplier<AnimationDef>> {
		val event = AnimationDefRegisterEvent()
		KeyframeAnimationRegistry.findAll().forEach { (k, v) ->
			event.register(k, ::AnimationDef)
		}
		return FORGE_BUS.post(event).getAll()
	}
}
