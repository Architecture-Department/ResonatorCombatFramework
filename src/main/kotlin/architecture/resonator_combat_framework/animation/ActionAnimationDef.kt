package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationDef
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import net.minecraft.resources.ResourceLocation

class ActionAnimationDef
@JvmOverloads
constructor(
	id: ResourceLocation,
	animationId: ResourceLocation,
	/** 动画播放期间期望的实体状态（key=状态ID, value=目标布尔值） */
	val stateModifiers: Map<ResourceLocation, Boolean> = mapOf(
		EntityStateHolder.CAN_SWITCH_ITEM to false
	),
) : AnimationDef(id, animationId) {

	@JvmOverloads
	constructor(
		id: ResourceLocation,
		stateModifiers: Map<ResourceLocation, Boolean> = mapOf(
			EntityStateHolder.CAN_SWITCH_ITEM to false
		)
	) : this(id, id, stateModifiers)
}
