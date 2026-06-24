package architecture.resonator_combat_framework.animation

import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity

/**
 * 动作动画 —— 具有与实体状态交互能力的动画基类。
 *
 * 相比 [architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation]，ActionAnimation 增加了：
 * 1. [stateModifiers]：动画播放期间期望的实体状态，由外部（控制器）负责同步
 * 2. 生命周期钩子：[onBegin] / [onTick] / [onEnd]，供子类深度修改实体行为
 * 3. 骨骼后处理：[modifyPose] 在 [computeAndWrite] 后调用，允许代码二次修改骨骼变换
 */
class ActionAnimation(
	id: ResourceLocation,
	animationId: String,
	/** 动画播放期间期望的实体状态（key=状态ID, value=目标布尔值） */
	val stateModifiers: Map<ResourceLocation, Boolean> = emptyMap(),
) : StaticAnimation(id, animationId) {

	constructor(
		id: ResourceLocation,
		stateModifiers: Map<ResourceLocation, Boolean> = emptyMap()
	) : this(id, id.namespace + "." + id.path, stateModifiers)

	constructor(
		animationId: String,
		stateModifiers: Map<ResourceLocation, Boolean> = emptyMap()
	) : this(RcfUtil.modRl(animationId), animationId, stateModifiers)

	// ===== 生命周期钩子 =====

	/**
	 * 动画开始时调用。
	 * 可用于取消物品使用、初始化坐标、设置实体标记等。
	 */
	fun onBegin(entity: LivingEntity) {}

	/**
	 * 每 tick 调用。
	 * 可用于修改实体运动、检测条件、动态调整动画参数等。
	 */
	fun onTick(entity: LivingEntity, animTime: Float, deltaTime: Float) {}

	/**
	 * 动画结束时调用。
	 * 可用于清理标记、触发额外逻辑等。
	 */
	fun onEnd(entity: LivingEntity) {}

	/**
	 * 骨骼后处理钩子。
	 * 在 [computeAndWrite] 将 BakingBrAnimation 骨骼写入 [ProxyModel] 之后调用，
	 * 允许代码对骨骼变换进行二次修改（如武器指向目标、IK 校正等）。
	 */
	fun modifyPose(proxyModel: ProxyModel, time: Float) {}
}
