package architecture.resonator_combat_framework.combat

import architecture.goldenboughs_lib.api.AllOpen
import architecture.resonator_combat_framework.state.EntityStateHolder
import net.minecraft.world.entity.LivingEntity
import java.util.*

/**
 * 攻击阶段 —— 定义攻击动作中造成伤害的时间窗口及相关碰撞体绑定。
 * 状态修饰通过 [ActionProperty] 键存储在 [properties] 中，
 * 在阶段开始时自动应用到 [EntityStateHolder]，结束时恢复。
 */
@AllOpen
data class AttackActionPhase(
	/**
	 * 阶段起始时间（秒）
	 */
	val startTime: Float,
	/**
	 * 阶段结束时间（秒）
	 */
	val endTime: Float,
	/**
	 * 每次攻击最多命中实体数
	 */
	val maxStrikes: Int = 3,
	/**
	 * 伤害倍率
	 */
	val damageMultiplier: Float = 1.0f,
	/**
	 * 碰撞体数量
	 */
	val colliderCount: Int = 3,
	/**
	 *本阶段中用于碰撞检测的骨骼-碰撞体绑定列表
	 */
	val colliders: Array<JointColliderPair>
) {
	/** 阶段属性映射表 */
	private val properties = mutableMapOf<ActionProperty<*>, Pair<Any, Any>>()

	fun duringTheStage(time: Float): Boolean {
		return time > startTime && time <= endTime
	}

	/**
	 * 链式添加属性。
	 * @param key 属性键
	 * @param value 属性值
	 * @return 自身，支持链式调用
	 */
	@Suppress("UNCHECKED_CAST")
	fun <T : Any> addProperty(key: ActionProperty<T>, value: T, default: T): AttackActionPhase {
		properties[key] = value to default
		return this
	}

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> getProperty(key: ActionProperty<T>): Optional<T> =
		Optional.ofNullable((properties[key]?.first as? T))

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> getDefaultProperty(key: ActionProperty<T>): Optional<T> =
		Optional.ofNullable((properties[key]?.second as? T))

	/** 获取内部属性映射的只读视图 */
	fun getAllProperties(): Map<ActionProperty<*>, Any> = properties.toMap()

	/** 获取所有布尔状态修饰 */
	@Suppress("UNCHECKED_CAST")
	fun getStateModifiers(): Map<BooleanStateProperty, Pair<Boolean, Boolean>> =
		properties.filterKeys { it is BooleanStateProperty }.mapKeys { it.key as BooleanStateProperty }
			.mapValues { it.value as Pair<Boolean, Boolean> }

	/** 获取所有浮点状态修饰 */
	@Suppress("UNCHECKED_CAST")
	fun getFloatModifiers(): Map<FloatStateProperty, Pair<Float, Float>> =
		properties.filterKeys { it is FloatStateProperty }.mapKeys { it.key as FloatStateProperty }
			.mapValues { it.value as Pair<Float, Float> }

	@Suppress("DuplicatedCode")
	fun resetState(entity: LivingEntity) {
		if (!EntityStateHolder.has(entity)) return
		val stateHolder = EntityStateHolder.of(entity)
		val boolMods = getStateModifiers()
		if (boolMods.isNotEmpty()) {
			stateHolder.applyStateModifiers(boolMods.mapKeys { it.key.id }
				.mapValues { it.value.second })
		}
		val floatMods = getFloatModifiers()
		if (floatMods.isNotEmpty()) {
			stateHolder.applyFloatModifiers(floatMods.mapKeys { it.key.id }
				.mapValues { it.value.second })
		}
	}

	@Suppress("DuplicatedCode")
	fun applyState(entity: LivingEntity) {
		if (!EntityStateHolder.has(entity)) return
		val stateHolder = EntityStateHolder.of(entity)
		val boolMods = getStateModifiers()
		if (boolMods.isNotEmpty()) {
			stateHolder.applyStateModifiers(boolMods.mapKeys { it.key.id }
				.mapValues { it.value.second })
		}
		val floatMods = getFloatModifiers()
		if (floatMods.isNotEmpty()) {
			stateHolder.applyFloatModifiers(floatMods.mapKeys { it.key.id }
				.mapValues { it.value.second })
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is AttackActionPhase) return false

		if (startTime != other.startTime) return false
		if (endTime != other.endTime) return false
		if (maxStrikes != other.maxStrikes) return false
		if (damageMultiplier != other.damageMultiplier) return false
		if (colliderCount != other.colliderCount) return false
		if (!colliders.contentEquals(other.colliders)) return false
		if (properties != other.properties) return false

		return true
	}

	override fun hashCode(): Int {
		var result = startTime.hashCode()
		result = 31 * result + endTime.hashCode()
		result = 31 * result + maxStrikes
		result = 31 * result + damageMultiplier.hashCode()
		result = 31 * result + colliderCount
		result = 31 * result + colliders.contentHashCode()
		result = 31 * result + properties.hashCode()
		return result
	}
}