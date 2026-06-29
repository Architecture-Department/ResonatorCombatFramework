package architecture.resonator_combat_framework.animation

import architecture.goldenboughs_lib.util.PoseStack
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.module.collision.CollisionEntry
import architecture.resonator_combat_framework.module.collision.CollisionSystem
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
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
	fun onHurtEntity(attacker: LivingEntity, target: LivingEntity, phase: AttackPhase) {}

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

	/** 上一 tick 活跃阶段的 startTime 集合，用于检测阶段切换 */
	private var prevActivePhaseStarts: Set<Float> = emptySet()
	// ===== 生命周期 =====

	override fun onEnd(entity: Entity) {
		val data = CollisionSystem.getData(entity)
		data.removeGroup(id)
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
		val phases = getActivePhases(animTime)
		if (phases.isEmpty()) {
			prevActivePhaseStarts = emptySet(); return
		}

		val currentStarts = phases.map { it.startTime }.toSet()
		val started = phases.filter { it.startTime !in prevActivePhaseStarts }
		val ended = prevActivePhaseStarts - currentStarts
		started.forEach { RcfEventHooks.AnimationPhaseStart(controller, it) }
		phases.filter { it.startTime in ended }.forEach { RcfEventHooks.AnimationPhaseEnd(controller, it) }
		prevActivePhaseStarts = currentStarts

		collider(entity, animTime, proxyModel, brModel, mergedProxy, controller, phases)
	}

	fun collider(
		entity: Entity,
		animTime: Float,
		proxyModel: ProxyModel,
		brModel: BrModel,
		mergedProxy: ProxyModel,
		controller: IEntityAnimationController<*>,
		phases: List<AttackPhase>
	) {
		RcfEventHooks.AnimationColliderPre(controller, entity, animTime, proxyModel, brModel, mergedProxy)

		onColliderUpdate(entity, animTime, proxyModel, brModel, mergedProxy, controller)

		val data = CollisionSystem.getData(entity)
		data.removeColliders(id)

		val entityPos = entity.position()
		val bodyRot = -entity.getPreciseBodyRotation(1.0f) * (Math.PI.toFloat() / 180f)

		for (phase in phases) {
			for (pair in phase.colliders) {
				if (mergedProxy.getBone(pair.boneName) == null) continue

				val worldMatrix = Matrix4f()
					.translate(entityPos.x.toFloat(), entityPos.y.toFloat(), entityPos.z.toFloat())
					.rotateY(bodyRot)
					.mul(brModel.computeBoneGlobalMatrix(pair.boneName, mergedProxy, PoseStack(), true).last().pose)

				data.addCollider(
					CollisionEntry(
						id = id,
						shape = pair.collider,
						worldMatrix = worldMatrix,
					)
				)
			}
		}

		RcfEventHooks.AnimationColliderPost(controller, entity, animTime, proxyModel, brModel, mergedProxy)
	}
}
