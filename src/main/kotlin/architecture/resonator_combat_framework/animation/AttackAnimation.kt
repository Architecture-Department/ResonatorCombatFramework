package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

class AttackAnimation
@JvmOverloads
constructor(
	id: ResourceLocation,
	animationId: String,
	stateModifiers: Map<ResourceLocation, Boolean> = emptyMap(),
	/** 攻击阶段列表 */
	val phases: List<AttackPhase> = emptyList(),
) : ActionAnimation(id, animationId, stateModifiers) {

	constructor(
		id: ResourceLocation,
		phases: List<AttackPhase> = emptyList()
	) : this(id, id.namespace + "." + id.path, emptyMap(), phases)

	constructor(
		animationId: String,
		phases: List<AttackPhase> = emptyList()
	) : this(RcfUtil.modRl(animationId), animationId, emptyMap(), phases)

	constructor(
		id: ResourceLocation,
		vararg phases: AttackPhase = emptyArray()
	) : this(id, phases.toList())

	constructor(
		animationId: String,
		vararg phases: AttackPhase = emptyArray()
	) : this(animationId, phases.toList())

	// ===== 阶段查询 =====

	/**
	 * 获取当前动画时间下活跃的攻击阶段。
	 */
	fun getActivePhases(animTime: Float): List<AttackPhase> =
		phases.filter { animTime >= it.startTime && animTime < it.endTime }

	// ===== 碰撞回调 =====

	/**
	 * 命中实体时的回调。
	 * 由 [CollisionBridge] 在检测到碰撞时自动调用。
	 */
	fun onHurtEntity(attacker: LivingEntity, target: LivingEntity, phase: AttackPhase) {}
}
