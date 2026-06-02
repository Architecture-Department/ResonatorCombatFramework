package architecture.resonator_combat_framework.module.entity_animation.mapper

import architecture.resonator_combat_framework.events.registry.AnimationControllerRegistry
import architecture.resonator_combat_framework.module.entity_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.entity_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.controller.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.controller.BaseAnimationController
import architecture.resonator_combat_framework.module.entity_animation.controller.IAnimationController
import architecture.resonator_combat_framework.module.entity_animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneFlags
import architecture.resonator_combat_framework.module.entity_animation.registry.ProxyBoneConfigRegistry
import architecture.resonator_combat_framework.module.entity_animation.util.BoneTransformUtil
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

abstract class EntityAnimationMapper<T : Entity, M : EntityModel<T>>
@JvmOverloads
constructor(
	val entity: T,
	override val isClient: Boolean = entity.level().isClientSide
) : IAnimationMapper {

	override val animationControllerManager = AnimationControllerManager()
	override val configLoader: ProxyBoneConfigRegistry
		get() = ProxyBoneConfigRegistry.getInstance(isClient)

	// ---- 触发 ----

	override fun trigger(playData: AnimationPlayData) {
		val controller = animationControllerManager.get(playData.controllerName) ?: mainController
		val loaded = configLoader.getConfig(playData.animId)
		val used = playData.boneConfig ?: loaded
		val bac = controller as BaseAnimationController
		bac.resolvedBoneConfig = used
		bac.boneConfigs = used
		controller.trigger(playData)
	}

	// ---- 停止 ----

	override fun stop(controllerName: ResourceLocation, fadeOutTicks: Int) {
		(animationControllerManager.get(controllerName) ?: mainController).stop(fadeOutTicks)
	}

	override fun stopAll(fadeOutTicks: Int) {
		animationControllerManager.getAll().forEach { it.stop(fadeOutTicks) }
	}

	// ---- 暂停/恢复 ----

	override fun pause(controllerName: ResourceLocation) {
		(animationControllerManager.get(controllerName) ?: mainController).pause()
	}

	override fun resume(controllerName: ResourceLocation) {
		(animationControllerManager.get(controllerName) ?: mainController).resume()
	}

	override fun pauseAll() = animationControllerManager.getAll().forEach { it.pause() }

	override fun resumeAll() = animationControllerManager.getAll().forEach { it.resume() }

	// ---- 状态查询 ----

	override fun isActive(): Boolean = animationControllerManager.isAnyActive()

	override fun getController(controllerName: ResourceLocation): IAnimationController =
		animationControllerManager.get(controllerName) ?: mainController

	override fun controllers(): List<IAnimationController> = animationControllerManager.getAll()

	override fun hasController(controllerName: ResourceLocation): Boolean =
		animationControllerManager.has(controllerName)

	// ---- 控制器管理 ----

	override fun addController(name: ResourceLocation, controller: IAnimationController) {
		if (name == AnimationControllerRegistry.MAIN) return
		animationControllerManager.add(name, controller)
	}

	override fun removeController(name: ResourceLocation) {
		if (name == AnimationControllerRegistry.MAIN) return
		animationControllerManager.remove(name)
	}

	// ---- 高级查询 ----

	override fun getActiveControllersSorted(): List<IAnimationController> =
		animationControllerManager.getSortedActive()

	override fun getRenderableControllers(): List<IAnimationController> =
		animationControllerManager.getRenderable()

	override fun findBlockingControllers(controller: IAnimationController): List<IAnimationController> =
		animationControllerManager.findBlocking(controller)

	// ---- Tick ----

	override fun tickAnimations() = animationControllerManager.tickAnimations(this)

	// ---- 配置 ----

	override fun resolveConfig(animId: String): ProxyBoneConfigData = configLoader.getConfig(animId)

	// ---- 骨骼应用 ----

	abstract fun applyProxyToModel(
		proxyModel: ProxyModel, model: M,
		flags: Map<String, ProxyBoneFlags>, weight: Float
	)

	fun applyRootTransform(
		proxyModel: ProxyModel, poseStack: PoseStack,
		flags: Map<String, ProxyBoneFlags>, weight: Float
	) {
		if (!isClient) return
		if (weight <= 0f) return
		val bone = proxyModel.getBone("root") ?: return
		val t = BoneTransformUtil.computeForPoseStack(bone, flags["root"], weight, flipY = true)
		BoneTransformUtil.applyTo(poseStack, t)
	}
}
