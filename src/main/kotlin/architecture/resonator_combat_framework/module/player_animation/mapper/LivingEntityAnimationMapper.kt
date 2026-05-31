package architecture.resonator_combat_framework.module.player_animation.mapper

// 生物实体动画映射器

import architecture.resonator_combat_framework.module.player_animation.controller.BaseAnimationController
import architecture.resonator_combat_framework.module.player_animation.flags.ProxyBoneFlags
import net.minecraft.client.model.EntityModel
import net.minecraft.world.entity.LivingEntity

/** + 骨骼标志解析 + 动画时间追踪 */
abstract class LivingEntityAnimationMapper<T : LivingEntity, M : EntityModel<T>>(
	livingEntity: T
) : EntityAnimationMapper<T, M>(livingEntity) {

	/** 动画已运行时间，用于 timeline 求值 */
	// TODO 确保是否需要
	protected var animTimeTracker = 0f

	/** 收集所有活跃可渲染控制器的骨骼标志 */
	fun resolveBoneFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val flags = mutableMapOf<String, ProxyBoneFlags>()
		for (ctrl in controllerManager.getRenderable()) {
			flags.putAll((ctrl as BaseAnimationController).resolveBoneFlags(animTime))
		}
		return flags
	}
}
