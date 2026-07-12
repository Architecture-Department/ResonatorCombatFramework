package architecture.resonator_combat_framework.core

import architecture.goldenboughs_lib.util.LazySupplier
import architecture.goldenboughs_lib.util.Value
import architecture.resonator_combat_framework.animation.AnimationDef
import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.data.PlayConfig
import architecture.resonator_combat_framework.animation.mapper.IEntityAnimationMapperProvider
import architecture.resonator_combat_framework.combat.Action
import architecture.resonator_combat_framework.combat.ActionState
import architecture.resonator_combat_framework.combat.AttackActionPhase
import architecture.resonator_combat_framework.event.*
import architecture.resonator_combat_framework.event.ActionEvent.Changed.Type
import architecture.resonator_combat_framework.registry.KeyframeAnimationRegistry
import architecture.resonator_combat_framework.state_machine.holder.EntityStateHolder
import net.minecraft.core.particles.ParticleType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.neoforged.neoforge.common.NeoForge
import org.joml.Vector3d
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

/**
 * RCF 事件钩子 —— 集中转发战斗、动画、粒子等系统的事件到 NeoForge 总线。
 * 各模块通过此对象发射事件，外部监听者通过 [NeoForge.EVENT_BUS] 订阅。
 */
object RcfEventHooks {

	// ===== Combat (entity_state_machine) =====

	@JvmStatic
	fun combatActionStart(holder: EntityStateHolder<*>, entity: LivingEntity, action: Action) {
		NeoForge.EVENT_BUS.post(ActionEvent.Start(holder, entity, action))
	}

	@JvmStatic
	fun combatActionTickPre(holder: EntityStateHolder<*>, entity: LivingEntity, action: Action): Boolean {
		return NeoForge.EVENT_BUS.post(ActionEvent.Tick.Pre(holder, entity, action)).isCanceled
	}

	@JvmStatic
	fun combatActionTickPost(holder: EntityStateHolder<*>, entity: LivingEntity, action: Action) {
		NeoForge.EVENT_BUS.post(ActionEvent.Tick.Post(holder, entity, action))
	}

	@JvmStatic
	fun combatActionEnd(holder: EntityStateHolder<*>, entity: LivingEntity, action: Action) {
		NeoForge.EVENT_BUS.post(ActionEvent.End(holder, entity, action))
	}

	@JvmStatic
	fun combatActionInterruptible(
		holder: EntityStateHolder<*>, entity: LivingEntity, action: Action, target: Action, value: Boolean
	): Boolean {
		val event =
			NeoForge.EVENT_BUS.post(ActionEvent.Interruptible(holder, entity, action, target, value, value))
		return if (event.isCanceled) value else event.newValue
	}

	@JvmStatic
	fun combatActionChanged(
		holder: EntityStateHolder<*>, entity: LivingEntity, oldValue: Action?, newValue: Action?, type: Type
	): ActionEvent.Changed {
		return NeoForge.EVENT_BUS.post(ActionEvent.Changed(holder, entity, oldValue, newValue, type))
	}

	@JvmStatic
	fun combatActionStateChanged(
		holder: EntityStateHolder<*>,
		entity: LivingEntity,
		oldValue: ActionState,
		newValue: ActionState
	) {
		NeoForge.EVENT_BUS.post(ActionStateChangedEvent(holder, entity, oldValue, newValue))
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
	fun <T : Entity> animationControllerRegister(): ControllerRegisterEvent<T> {
		return NeoForge.EVENT_BUS.post(ControllerRegisterEvent<T>())
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
