package architecture.resonator_combat_framework.animation

/**
 * 攻击阶段 —— 定义攻击动画中造成伤害的时间窗口及相关属性。
 *
 * @param startTime 阶段起始时间（秒）
 * @param endTime 阶段结束时间（秒）
 * @param damageMultiplier 伤害倍率
 * @param impact 冲击强度（影响击退/失衡）
 * @param stun 眩晕强度（影响目标僵直时长）
 * @param colliders 本阶段中用于碰撞检测的骨骼-碰撞体绑定列表
 */
data class AttackPhase
@JvmOverloads
constructor(
	val startTime: Float,
	val endTime: Float,
	val damageMultiplier: Float = 1f,
	val impact: Float = 0f,
	val stun: Float = 0f,
	val colliders: List<JointColliderPair> = emptyList(),
)
