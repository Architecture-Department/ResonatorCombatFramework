package architecture.resonator_combat_framework.module.entity_animation.engine.molang

import architecture.resonator_combat_framework.module.entity_animation.controller.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.mixed.IAnimationProxyProvider
import architecture.resonator_combat_framework.module.entity_animation.mixed.IAnimationProxyProvider.Companion.getAnimationTransformer
import net.minecraft.world.entity.Entity

interface MolangValue {
	fun get(context: MolangData? = null): Double

	fun get(entity: Entity): Double {
		if (entity !is IAnimationProxyProvider) return 0.0
		return get(entity as IAnimationProxyProvider)
	}

	fun get(proxyProvider: IAnimationProxyProvider): Double {
		return get(MolangData.of(proxyProvider.getAnimationTransformer().holder))
	}

	fun get(controller: IEntityAnimationController<*>): Double {
		return get(controller.currentData)
	}

	fun get(controllerManager: AnimationControllerManager<*>): Double {
		return get(controllerManager.mapper.holder)
	}

	fun isMutable(): Boolean = true
}