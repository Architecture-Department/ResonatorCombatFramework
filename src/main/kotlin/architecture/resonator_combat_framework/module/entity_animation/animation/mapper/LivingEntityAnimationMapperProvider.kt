package architecture.resonator_combat_framework.module.entity_animation.animation.mapper

import architecture.resonator_combat_framework.module.entity_animation.animation.controller.AnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.BoneFlags
import net.minecraft.client.model.EntityModel
import net.minecraft.world.entity.LivingEntity

/**
 * 生物实体动画映射器抽象基类，在 [EntityAnimationMapperProvider] 基础上增加骨骼标志收集功能。
 * 提供从活跃可渲染控制器中合并骨骼标志的方法，供子类在渲染时使用。
 *
 * @param T 生物实体类型
 * @param M 实体模型类型
 */
abstract class LivingEntityAnimationMapperProvider<T : LivingEntity, M : EntityModel<T>>(
	livingEntity: T,
	isClient: Boolean,
	animationControllerManager: AnimationControllerManager<T>
) : EntityAnimationMapperProvider<T, M>(livingEntity, isClient, animationControllerManager) {
	constructor(holder: T) : this(holder, holder.level().isClientSide, AnimationControllerManager(holder))

	/**
	 * 收集所有活跃可渲染控制器的骨骼标志，用于外部按控制器解析标志。
	 *
	 * @param animTime 当前动画时间
	 * @return 骨骼名称到骨骼标志的映射
	 */
	fun resolveBoneFlags(animTime: Float): Map<String, BoneFlags> {
		val flags = mutableMapOf<String, BoneFlags>()
		for (ctrl in animationControllerManager.getRenderable()) {
			flags.putAll((ctrl as AnimationController).activeBoneConfig.resolveBoneFlags(animTime))
		}
		return flags
	}
}
