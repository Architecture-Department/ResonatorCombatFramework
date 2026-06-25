package architecture.resonator_combat_framework.module.entity_animation.animation.molang

import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.mixed.IAnimationProxyProvider
import architecture.resonator_combat_framework.module.entity_animation.mixed.IAnimationProxyProvider.Companion.getAnimationTransformer
import net.minecraft.world.entity.Entity

interface MolangValue {
	fun eval(context: MolangData? = null): Double

	fun eval(entity: Entity): Double {
		if (entity !is IAnimationProxyProvider) return 0.0
		return eval(entity as IAnimationProxyProvider)
	}

	fun eval(proxyProvider: IAnimationProxyProvider): Double {
		return eval(MolangData.of(proxyProvider.getAnimationTransformer().holder))
	}

	fun eval(controller: IEntityAnimationController<*>): Double {
		return eval(controller.currentData)
	}

	fun eval(controllerManager: AnimationControllerManager<*>): Double {
		return eval(controllerManager.mapperProvider.holder)
	}

	fun isMutable(): Boolean = true
}