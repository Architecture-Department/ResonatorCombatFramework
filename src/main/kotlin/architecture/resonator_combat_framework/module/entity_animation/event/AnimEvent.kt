package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayConfig
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapperProvider
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event

/**
 * 动画事件基类，所有与实体动画相关的事件的父类。
 * 提供获取当前动画数据、控制器管理器、动画映射器以及持有实体的便捷方法。
 */
@AllOpe
abstract class AnimEvent(
	val animationController: IEntityAnimationController<*>,
) : Event() {
	/**
	 * 获取当前正在播放的动画配置数据。
	 * @return 当前 [PlayConfig] 实例
	 */
	fun getAnimationData(): PlayConfig {
		return animationController.currentConfig
	}

	/**
	 * 获取当前动画所属的动画控制器管理器。
	 * @return [AnimationControllerManager] 实例
	 */
	fun getManager(): AnimationControllerManager<out Entity> {
		return animationController.manager
	}

	/**
	 * 获取当前动画对应的实体动画映射器提供者。
	 * @return [IEntityAnimationMapperProvider] 实例
	 */
	fun getMapper(): IEntityAnimationMapperProvider<out Entity, *> {
		return animationController.manager.mapperProvider
	}

	/**
	 * 获取当前动画控制器的持有实体（如玩家、生物等）。
	 * @return 持有动画的 [Entity] 实例
	 */
	fun getHolder(): Entity {
		return animationController.manager.mapperProvider.holder
	}
}
