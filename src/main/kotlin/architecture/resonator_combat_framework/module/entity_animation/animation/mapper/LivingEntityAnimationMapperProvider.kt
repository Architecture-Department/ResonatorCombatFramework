package architecture.resonator_combat_framework.module.entity_animation.animation.mapper

import architecture.resonator_combat_framework.module.entity_animation.animation.controller.BedrockAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.ProxyBoneFlags
import net.minecraft.client.model.EntityModel
import net.minecraft.world.entity.LivingEntity

/** 收集所有活跃可渲染控制器的骨骼标志，用于外部按控制器解析标志 */
abstract class LivingEntityAnimationMapperProvider<T : LivingEntity, M : EntityModel<T>>
@JvmOverloads
constructor(
	livingEntity: T,
	isClient: Boolean = livingEntity.level().isClientSide,
	animationControllerManager: AnimationControllerManager<T> = AnimationControllerManager(livingEntity)
) : EntityAnimationMapperProvider<T, M>(livingEntity, isClient, animationControllerManager) {

	/** 收集所有活跃可渲染控制器的骨骼标志 */
	fun resolveBoneFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val flags = mutableMapOf<String, ProxyBoneFlags>()
		for (ctrl in animationControllerManager.getRenderable()) {
			flags.putAll((ctrl as BedrockAnimationController).resolveBoneFlags(animTime))
		}
		return flags
	}
}
