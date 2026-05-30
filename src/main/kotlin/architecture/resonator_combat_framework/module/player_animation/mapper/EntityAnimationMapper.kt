package architecture.resonator_combat_framework.module.player_animation.mapper

import architecture.resonator_combat_framework.events.registry.AnimationControllerRegistry
import architecture.resonator_combat_framework.module.player_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneFlags
import architecture.resonator_combat_framework.module.player_animation.controller.BaseAnimationController
import architecture.resonator_combat_framework.module.player_animation.controller.ControllerManager
import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController
import architecture.resonator_combat_framework.module.player_animation.registry.ProxyBoneConfigRegistry
import architecture.resonator_combat_framework.module.player_animation.util.BoneTransformUtil
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

abstract class EntityAnimationMapper<T : Entity, M : EntityModel<T>>(
	val entity: T,
	protected val isClient: Boolean = entity.level().isClientSide
) : IAnimationMapper {

	protected val configLoader = ProxyBoneConfigRegistry.getInstance(isClient)

	/**
	 * 控制器管理器。
	 * 默认控制器由子类在 init 中添加，保证 getDefault() 可用。
	 */
	protected val controllerManager = ControllerManager()

	/** 默认控制器（管理器中的第一个） */
	protected val defaultController: IAnimationController
		get() = controllerManager.get(AnimationControllerRegistry.DEFAULT) ?: controllerManager.getDefault()
		?: error("Default controller not initialized")

	abstract fun applyProxyToModel(
		proxyModels: List<ProxyModel>, model: M,
		flags: Map<String, ProxyBoneFlags>, weight: Float
	)

	fun applyRootTransform(
		proxyModels: List<ProxyModel>, poseStack: PoseStack,
		flags: Map<String, ProxyBoneFlags>, weight: Float
	) {
		if (!isClient) return
		if (weight <= 0f) return
		for (proxy in proxyModels) {
			val bone = proxy.getBone("root") ?: continue
			val t = BoneTransformUtil.computeForPoseStack(bone, flags["root"], weight, flipY = true)
			BoneTransformUtil.applyTo(poseStack, t)
			return
		}
	}

	fun resolveConfig(animId: String): ProxyBoneConfigData = configLoader.getConfig(animId)

	fun tick(gameTime: Float, deltaSec: Float) {
		for (ctrl in controllerManager.getAll()) ctrl.tick(gameTime, deltaSec)
	}

	fun collectProxyModels(): List<ProxyModel> =
		controllerManager.getRenderable().map { (it as BaseAnimationController).proxyModel }


	fun mergedWeight(): Float =
		controllerManager.getRenderable().firstOrNull()?.effectiveWeight ?: defaultController.effectiveWeight

	override fun trigger(config: AnimationPlayConfig) {
		val controller = controllerManager.get(config.controllerName) ?: defaultController
		val loaded = configLoader.getConfig(config.animId)
		val used = config.boneConfig ?: loaded
		val bac = controller as BaseAnimationController
		bac.resolvedBoneConfig = used
		bac.boneConfigs = used
		controller.trigger(config)
	}

	override fun trigger(animId: String) = trigger(AnimationPlayConfig.of(animId))

	override fun trigger(controllerName: ResourceLocation, animId: String) {
		val cfg = resolveConfig(animId)
		val ctrl = controllerManager.get(controllerName) ?: defaultController
		val usedCfg = AnimationPlayConfig(
			animId = animId,
			controllerName = controllerName,
			fadeInTicks = cfg.transitionTicks,
			speedMultiplier = ctrl.speedMultiplier
		)
		(ctrl as BaseAnimationController).boneConfigs = cfg
		ctrl.trigger(usedCfg)
	}

	override fun stop(controllerName: ResourceLocation) {
		(controllerManager.get(controllerName) ?: defaultController).stop()
	}

	override fun stopImmediate(controllerName: ResourceLocation) {
		(controllerManager.get(controllerName) ?: defaultController).stopImmediate()
	}

	override fun stopAll() = controllerManager.getAll().forEach { it.stop() }
	override fun stopAllImmediate() = controllerManager.getAll().forEach { it.stopImmediate() }

	override fun pause(controllerName: ResourceLocation) {
		(controllerManager.get(controllerName) ?: defaultController).pause()
	}

	override fun resume(controllerName: ResourceLocation) {
		(controllerManager.get(controllerName) ?: defaultController).resume()
	}

	override fun stopAnimation(animId: String) {
		defaultController.stopAnimation(animId)
	}

	override fun stopAnimation(controllerName: ResourceLocation, animId: String) {
		(controllerManager.get(controllerName) ?: defaultController).stopAnimation(animId)
	}

	override fun isActive(): Boolean = controllerManager.isAnyActive()
	override fun isControllerActive(): Boolean = defaultController.isActive()
	override fun isControllerActive(controllerName: ResourceLocation): Boolean =
		(controllerManager.get(controllerName) ?: defaultController).isActive()

	override fun getController(): IAnimationController = defaultController
	override fun getController(controllerName: ResourceLocation): IAnimationController =
		controllerManager.get(controllerName) ?: defaultController

	override fun hasController(): Boolean = controllerManager.getDefault() != null
	override fun controllers() = controllerManager.getAll()
	override fun hasController(controllerName: ResourceLocation): Boolean = controllerManager.has(controllerName)

	override fun addController(name: ResourceLocation, controller: IAnimationController) {
		if (name == AnimationControllerRegistry.DEFAULT) return
		controllerManager.add(name, controller)
	}

	override fun removeController(name: ResourceLocation) {
		if (name == AnimationControllerRegistry.DEFAULT) return
		controllerManager.remove(name)
	}

	override fun getActiveControllersSorted(): List<IAnimationController> =
		controllerManager.getSortedActive()

	override fun findBlockingControllers(controller: IAnimationController): List<IAnimationController> =
		controllerManager.findBlocking(controller)

	fun getRenderableControllers(): List<IAnimationController> =
		controllerManager.getRenderable()
}





