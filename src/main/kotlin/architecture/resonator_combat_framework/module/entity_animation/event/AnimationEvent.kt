package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayConfig
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapperProvider
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event

/**
 * 动画事件基类
 */
@AllOpe
abstract class AnimationEvent(
	val animationController: IEntityAnimationController<*>,
) : Event() {
	/**
	 * 获取当前动画数据
	 */
	fun getAnimationData(): PlayConfig {
		return animationController.currentConfig
	}

	/**
	 * 获取动画控制器管理器
	 */
	fun getManager(): AnimationControllerManager<out Entity> {
		return animationController.manager
	}

	/**
	 * 获取动画映射器
	 */
	fun getMapper(): IEntityAnimationMapperProvider<out Entity, *> {
		return animationController.manager.mapperProvider
	}

	/**
	 * 获取动画控制器持有者
	 */
	fun getHolder(): Entity {
		return animationController.manager.mapperProvider.holder
	}
}