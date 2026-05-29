package architecture.resonator_combat_framework.module.player_animation.mapper

// 实体动画映射器。实体模型到动画骨骼的映射实现

import architecture.resonator_combat_framework.module.player_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.player_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneFlags
import architecture.resonator_combat_framework.module.player_animation.controller.BaseAnimationController
import architecture.resonator_combat_framework.module.player_animation.controller.IAnimationController
import architecture.resonator_combat_framework.module.player_animation.registry.ProxyBoneConfigRegistry
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.world.entity.Entity

abstract class EntityAnimationMapper<T : Entity, M : EntityModel<T>>(
	val entity: T,
	protected val isClient: Boolean = entity.level().isClientSide
) : IAnimationMapper {

	protected val configLoader = ProxyBoneConfigRegistry.getInstance(isClient)
	protected val boneConfigs = mutableMapOf<String, ProxyBoneConfigData>()

	abstract val controllers: LinkedHashMap<String, IAnimationController>
	abstract val defaultController: IAnimationController

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
		for (ctrl in controllers()) ctrl.tick(gameTime, deltaSec)
	}

	fun collectProxyModels(): List<ProxyModel> =
		getRenderableControllers().map { (it as BaseAnimationController).proxyModel }

	fun resolveMergedFlags(animTime: Float): Map<String, ProxyBoneFlags> {
		val flags = mutableMapOf<String, ProxyBoneFlags>()
		for ((_, config) in boneConfigs) flags.putAll(config.resolveBoneFlags(animTime))
		return flags
	}

	fun mergedWeight(): Float = defaultController.effectiveWeight

	override fun trigger(config: AnimationPlayConfig) {
		val controller = controllers[config.controllerName] ?: defaultController
		val loaded = configLoader.getConfig(config.animId)
		val used = config.boneConfig ?: loaded
		boneConfigs.clear()
		boneConfigs[config.animId] = used
		(controller as BaseAnimationController).resolvedBoneConfig = used
		controller.trigger(config)
	}

	override fun trigger(animId: String) = trigger(AnimationPlayConfig.of(animId))

	override fun trigger(controllerName: String, animId: String) {
		val cfg = resolveConfig(animId)
		val ctrl = controllers[controllerName] ?: defaultController
		val usedCfg = AnimationPlayConfig.of(animId).copy(
			controllerName = controllerName,
			fadeInTicks = cfg.transitionTicks,
			speedMultiplier = ctrl.speedMultiplier
		)
		boneConfigs.clear()
		boneConfigs[animId] = cfg
		ctrl.trigger(usedCfg)
	}

	override fun stop(controllerName: String) {
		(controllers[controllerName] ?: defaultController).stop()
	}

	override fun stopImmediate(controllerName: String) {
		(controllers[controllerName] ?: defaultController).stopImmediate()
	}

	override fun stopAll() = controllers().forEach { it.stop() }
	override fun stopAllImmediate() = controllers().forEach { it.stopImmediate() }
	override fun pause(controllerName: String) {
		(controllers[controllerName] ?: defaultController).pause()
	}

	override fun resume(controllerName: String) {
		(controllers[controllerName] ?: defaultController).resume()
	}

	override fun stopAnimation(animId: String) {
		boneConfigs.remove(animId)
		defaultController.stopAnimation(animId)
	}

	override fun stopAnimation(controllerName: String, animId: String) {
		boneConfigs.remove(animId)
		(controllers[controllerName] ?: defaultController).stopAnimation(animId)
	}

	override fun isActive(): Boolean = controllers.values.any { it.isActive() }
	override fun isControllerActive(): Boolean = defaultController.isActive()
	override fun isControllerActive(controllerName: String): Boolean =
		(controllers[controllerName] ?: defaultController).isActive()

	override fun getController(): IAnimationController = defaultController
	override fun getController(controllerName: String): IAnimationController =
		controllers[controllerName] ?: defaultController

	override fun hasController(): Boolean = true
	override fun controllers() = controllers.values
	override fun hasController(controllerName: String): Boolean = controllerName in controllers

	override fun addController(name: String, controller: IAnimationController) {
		if (name == IAnimationMapper.DEFAULT_CONTROLLER_NAME) return
		controllers[name] = controller
	}

	override fun removeController(name: String) {
		if (name == IAnimationMapper.DEFAULT_CONTROLLER_NAME) return
		controllers.remove(name)?.stop()
	}

	override fun getActiveControllersSorted(): List<IAnimationController> =
		controllers().filter { it.isActive() }.sortedByDescending { it.priority }

	override fun findBlockingControllers(controller: IAnimationController): List<IAnimationController> {
		if (!controller.isActive()) return emptyList()
		val sorted = getActiveControllersSorted()
		val myIndex = sorted.indexOf(controller)
		if (myIndex < 0) return emptyList()
		val blocking = mutableListOf<IAnimationController>()
		for (i in 0 until myIndex) {
			val higher = sorted[i]
			if (higher.isOverriding && hasBoneConflict(higher, controller)) blocking.add(higher)
		}
		return blocking
	}

	fun getRenderableControllers(): List<IAnimationController> {
		val sorted = getActiveControllersSorted()
		if (sorted.isEmpty()) return sorted
		val result = mutableListOf(sorted.first())
		val blockedBones = mutableSetOf<String>()
		if (sorted.first().isOverriding) blockedBones.addAll(sorted.first().affectedBones)
		for (i in 1 until sorted.size) {
			val ctrl = sorted[i]
			if (ctrl.affectedBones.isNotEmpty() && ctrl.affectedBones.any { it in blockedBones }) continue
			result.add(ctrl)
			if (ctrl.isOverriding) blockedBones.addAll(ctrl.affectedBones)
		}
		return result
	}
}

