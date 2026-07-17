package architecture.resonator_combat_framework.combat

import architecture.goldenboughs_lib.api.AllOpen
import net.minecraft.resources.ResourceLocation

@AllOpen
class ActionProperty<T>(val id: ResourceLocation) {
	override fun toString(): String = "ActionProperty($id)"
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ActionProperty<*>) return false
		if (id != other.id) return false
		return true
	}

	override fun hashCode(): Int {
		return id.hashCode()
	}
}

/**
 * 攻击动作属性包装类型。
 */
class AttackActionProperty<T>(id: ResourceLocation) : ActionProperty<T>(id)

/**
 * 布尔状态修饰属性——用于 [EntityStateHolder] 的布尔状态键。
 */
class BooleanStateProperty(id: ResourceLocation) : ActionProperty<Boolean>(id)

/**
 * 浮点状态修饰属性——用于 [EntityStateHolder] 的浮点状态键。
 */
class FloatStateProperty(id: ResourceLocation) : ActionProperty<Float>(id)