package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.module.combat.ActionProperty
import architecture.resonator_combat_framework.module.combat.BooleanStateProperty
import architecture.resonator_combat_framework.module.combat.FloatStateProperty
import java.util.*

/**
 * 攻击阶段 —— 定义攻击动作中造成伤害的时间窗口及相关碰撞体绑定。
 * 状态修饰通过 [ActionProperty] 键存储在 [properties] 中，
 * 在阶段开始时自动应用到 [EntityStateHolder]，结束时恢复。
 *
 * @param startTime 阶段起始时间（秒）
 * @param endTime 阶段结束时间（秒）
 * @param colliders 本阶段中用于碰撞检测的骨骼-碰撞体绑定列表
 */
data class AttackActionPhase
@JvmOverloads
constructor(
	val startTime: Float,
	val endTime: Float,
	val colliders: List<JointColliderPair> = emptyList()
) {
	/** 阶段属性映射表 */
	private val properties = mutableMapOf<ActionProperty<*>, Any>()

	/**
	 * 链式添加属性。
	 * @param key 属性键
	 * @param value 属性值
	 * @return 自身，支持链式调用
	 */
	@Suppress("UNCHECKED_CAST")
	fun <T : Any> addProperty(key: ActionProperty<T>, value: T): AttackActionPhase {
		properties[key] = value as Any
		return this
	}

	/**
	 * 获取指定键的属性。
	 */
	@Suppress("UNCHECKED_CAST")
	fun <T : Any> getProperty(key: ActionProperty<T>): Optional<T> =
		Optional.ofNullable((properties[key] as? T))

	/** 获取内部属性映射的只读视图 */
	fun getAllProperties(): Map<ActionProperty<*>, Any> = properties.toMap()

	/** 获取所有布尔状态修饰 */
	fun getStateModifiers(): Map<BooleanStateProperty, Boolean> =
		properties.filterKeys { it is BooleanStateProperty }
			.mapKeys { it.key as BooleanStateProperty }
			.mapValues { it.value as Boolean }

	/** 获取所有浮点状态修饰 */
	fun getFloatModifiers(): Map<FloatStateProperty, Float> =
		properties.filterKeys { it is FloatStateProperty }
			.mapKeys { it.key as FloatStateProperty }
			.mapValues { it.value as Float }
}