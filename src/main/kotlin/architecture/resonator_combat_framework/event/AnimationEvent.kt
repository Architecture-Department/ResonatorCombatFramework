package architecture.resonator_combat_framework.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.data.PlayConfig
import architecture.resonator_combat_framework.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.animation.mapper.IEntityAnimationMapperProvider
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event

/**
 * 动画事件基类，所有与实体动画相关的事件的父类。
 * 提供获取当前动画数据、控制器管理器、动画映射器以及持有实体的便捷方法。
 */
@AllOpe
abstract class AnimationEvent<T : Entity>(
	val controller: IEntityAnimationController<T>,
) : Event() {
	/**
	 * 获取当前正在播放的动画配置数据。
	 * @return 当前 [PlayConfig] 实例
	 */
	fun getAnimationData(): PlayConfig {
		return controller.currentConfig
	}

	/**
	 * 获取当前动画所属的动画控制器管理器。
	 * @return [AnimationControllerManager] 实例
	 */
	fun getManager(): AnimationControllerManager<T> {
		return controller.manager
	}

	/**
	 * 获取当前动画对应的实体动画映射器提供者。
	 * @return [IEntityAnimationMapperProvider] 实例
	 */
	fun getMapperProvider(): IEntityAnimationMapperProvider<T, *> {
		return controller.manager.mapperProvider
	}

	/**
	 * 获取当前动画控制器的持有实体（如玩家、生物等）。
	 * @return 持有动画的 [Entity] 实例
	 */
	fun getHolder(): Entity {
		return controller.manager.mapperProvider.holder
	}

	/**
	 * 动画完成事件 —— 动画自然播放完毕时触发（非手动停止）。
	 */
	@AllOpe
	class Complete<T : Entity>(controller: IEntityAnimationController<T>) : AnimationEvent<T>(controller)
}
