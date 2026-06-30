package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationProperty
import architecture.resonator_combat_framework.module.entity_animation.animation.TimedEvent
import java.util.*

/**
 * 攻击阶段 —— 定义攻击动画中造成伤害的时间窗口及相关属性。
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
) {
	private val properties = mutableMapOf<AnimationProperty<*>, Any>()
	private val timedEvents = mutableListOf<TimedEvent>() // TODO

	fun <T : Any> addProperty(key: AnimationProperty<T>, value: T): AttackPhase {
		properties[key] = value as Any
		return this
	}

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> getProperty(key: AnimationProperty<T>): Optional<T> {
		val value = properties[key]
		return if (value != null) Optional.of(value as T) else Optional.empty()
	}

	fun addEvent(event: TimedEvent): AttackPhase {
		timedEvents.add(event)

		return this
	}

	fun getTimedEvents(): List<TimedEvent> = timedEvents
}
