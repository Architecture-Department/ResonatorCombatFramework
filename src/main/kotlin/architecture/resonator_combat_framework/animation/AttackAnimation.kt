package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

/**
 * 攻击阶段 —— 定义攻击动画中造成伤害的时间窗口及相关属性。
 *
 * @param startTime 阶段起始时间（秒）
 * @param endTime 阶段结束时间（秒）
 * @param damageMultiplier 伤害倍率
 * @param impact 冲击强度（影响击退/失衡）
 * @param stun 眩晕强度（影响目标僵直时长）
 */
data class AttackPhase(
	val startTime: Float,
	val endTime: Float,
	val damageMultiplier: Float = 1f,
	val impact: Float = 0f,
	val stun: Float = 0f,
)

/**
 * 攻击动画 —— 具有阶段式伤害判定能力的 [ActionAnimation]。
 *
 * 继承 [ActionAnimation] 的所有能力（状态交互、生命周期钩子、骨骼后处理），
 * 额外增加 [phases] 阶段系统，用于定义攻击判定窗口。
 *
 * 阶段系统与碰撞体分离，碰撞检测由外部系统（或子类）在 [onTick] 中实现。
 */
class AttackAnimation(
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

	/**
	 * 获取当前动画时间下活跃的攻击阶段。
	 */
	fun getActivePhases(animTime: Float): List<AttackPhase> =
		phases.filter { animTime >= it.startTime && animTime < it.endTime }

	/**
	 * 命中实体时的回调。
	 * 由外部系统（或子类重写后的 [onTick]）在检测到碰撞时调用。
	 */
	fun onHurtEntity(attacker: LivingEntity, target: LivingEntity, phase: AttackPhase) {}
}
