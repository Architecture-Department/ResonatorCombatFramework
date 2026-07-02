package architecture.resonator_combat_framework.module.entity_animation.animation.mapper

import architecture.resonator_combat_framework.module.entity_animation.animation.controller.AnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.BoneFlags
import net.minecraft.client.model.EntityModel
import net.minecraft.world.entity.LivingEntity

/** 收集所有活跃可渲染控制器的骨骼标志，用于外部按控制器解析标志 */
abstract class LivingEntityAnimationMapperProvider<T : LivingEntity, M : EntityModel<T>>(
	livingEntity: T,
	isClient: Boolean,
	animationControllerManager: AnimationControllerManager<T>
) : EntityAnimationMapperProvider<T, M>(livingEntity, isClient, animationControllerManager) {
	constructor(holder: T) : this(holder, holder.level().isClientSide, AnimationControllerManager(holder))

	/** 收集所有活跃可渲染控制器的骨骼标志 */
	fun resolveBoneFlags(animTime: Float): Map<String, BoneFlags> {
		val flags = mutableMapOf<String, BoneFlags>()
		for (ctrl in animationControllerManager.getRenderable()) {
			flags.putAll((ctrl as AnimationController).activeBoneConfig.resolveBoneFlags(animTime))
		}
		return flags
	}
}
