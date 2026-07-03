package architecture.resonator_combat_framework.module.animation.molang

import architecture.resonator_combat_framework.module.animation.IAnimationProvider
import architecture.resonator_combat_framework.module.animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.animation.mapper.AnimationControllerManager
import net.minecraft.world.entity.Entity

interface MolangValue {
	fun eval(context: MolangData? = null): Double

	fun eval(entity: Entity): Double {
		if (entity !is IAnimationProvider) return 0.0
		return eval(entity as IAnimationProvider)
	}

	fun eval(proxyProvider: IAnimationProvider): Double {
		return eval(MolangData.of(proxyProvider.getMapperProvider().holder))
	}

	fun eval(controller: IEntityAnimationController<*>): Double {
		return eval(controller.currentData)
	}

	fun eval(controllerManager: AnimationControllerManager<*>): Double {
		return eval(controllerManager.mapperProvider.holder)
	}

	fun isMutable(): Boolean = true
}