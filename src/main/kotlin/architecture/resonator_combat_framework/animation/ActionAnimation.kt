package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.module.entity_state_machine.holder.EntityStateHolder
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation

/**
 * 动作动画 —— 具有与实体状态交互能力的动画基类。
 *
 * 相比 [StaticAnimation]，ActionAnimation 增加了：
 * 1. [stateModifiers]：动画播放期间期望的实体状态，由外部（控制器）负责同步
 * 2. 生命周期钩子：[onBegin] / [onTick] / [onEnd]，供子类深度修改实体行为
 * 3. 骨骼后处理：[modifyPose] 在 [computeAndWrite] 后调用，允许代码二次修改骨骼变换
 */
open class ActionAnimation(
	id: ResourceLocation,
	animationId: String,
	/** 动画播放期间期望的实体状态（key=状态ID, value=目标布尔值） */
	val stateModifiers: Map<ResourceLocation, Boolean> = mapOf(
		EntityStateHolder.CAN_SWITCH_ITEM to false
	),
) : StaticAnimation(id, animationId) {

	constructor(
		id: ResourceLocation,
		stateModifiers: Map<ResourceLocation, Boolean> = mapOf(
			EntityStateHolder.CAN_SWITCH_ITEM to false
		)
	) : this(id, id.namespace + "." + id.path, stateModifiers)

	constructor(
		animationId: String,
		stateModifiers: Map<ResourceLocation, Boolean> = mapOf(
			EntityStateHolder.CAN_SWITCH_ITEM to false
		)
	) : this(RcfUtil.modRl(animationId), animationId, stateModifiers)

	// ===== 生命周期钩子 =====

	/** 动画开始时调用。 */
	open fun onBegin(entity: LivingEntity) {}

	/** 每 tick 调用。 */
	open fun onTick(entity: LivingEntity, animTime: Float, deltaTime: Float) {}

	/** 动画结束时调用。 */
	open fun onEnd(entity: LivingEntity) {}

	/** 骨骼后处理钩子。 */
	open fun modifyPose(proxyModel: ProxyModel, time: Float) {}
}
