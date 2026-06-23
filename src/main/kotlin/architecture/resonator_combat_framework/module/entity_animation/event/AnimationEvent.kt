package architecture.resonator_combat_framework.module.entity_animation.event

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.IEntityAnimationMapper
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event

@AllOpe
abstract class AnimationEvent(
	val animationController: IEntityAnimationController<*>,
) : Event() {
	/**
	 * 获取当前动画数据
	 */
	fun getAnimationData(): AnimationPlayData {
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
	fun getMapper(): IEntityAnimationMapper<out Entity, *> {
		return animationController.manager.mapper
	}

	/**
	 * 获取动画控制器持有者
	 */
	fun getHolder(): Entity {
		return animationController.manager.mapper.holder
	}
}