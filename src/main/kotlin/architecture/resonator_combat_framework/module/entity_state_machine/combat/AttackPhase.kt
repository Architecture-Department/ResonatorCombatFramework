package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.resonator_combat_framework.animation.JointColliderPair

/**
 * 攻击阶段 —— 定义攻击动作中造成伤害的时间窗口及相关碰撞体绑定。
 *
 * @param startTime 阶段起始时间（秒）
 * @param endTime 阶段结束时间（秒）
 * @param colliders 本阶段中用于碰撞检测的骨骼-碰撞体绑定列表
 */
data class AttackPhase
@JvmOverloads
constructor(
	val startTime: Float,
	val endTime: Float,
	val colliders: List<JointColliderPair> = emptyList()
)
