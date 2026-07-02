package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationDef
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import net.minecraft.resources.ResourceLocation

/**
 * 动作动画定义。
 *
 * 用于描述实体执行非攻击类动作（如物品切换、交互等）时的动画播放定义。
 * 在动画播放期间默认禁用物品切换状态，以保持动画动作的完整性。
 *
 * @property stateModifiers 动画播放期间期望的实体状态映射（key=状态ID, value=目标布尔值）
 */
class ActionAnimationDef
@JvmOverloads
constructor(
	id: ResourceLocation,
	animationId: ResourceLocation,
	/** 动画播放期间期望的实体状态（key=状态ID, value=目标布尔值） */
	val stateModifiers: Map<ResourceLocation, Boolean> = mapOf(
		EntityStateHolder.CAN_SWITCH_ITEM to false
	),
	/** 动画播放期间期望的浮点状态（速度倍率、视角速度等） */
	val floatModifiers: Map<ResourceLocation, Float> = emptyMap(),
) : AnimationDef(id, animationId) {

	/**
	 * 使用与动画定义 ID 相同的 ID 同时作为动画资源 ID 的便捷构造方法。
	 */
	@JvmOverloads
	constructor(
		id: ResourceLocation,
		stateModifiers: Map<ResourceLocation, Boolean> = mapOf(
			EntityStateHolder.CAN_SWITCH_ITEM to false
		),
		floatModifiers: Map<ResourceLocation, Float> = emptyMap()
	) : this(id, id, stateModifiers, floatModifiers)
}

