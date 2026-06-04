package architecture.resonator_combat_framework.module.entity_animation.mapper

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.api.IAnimationMapper
import architecture.resonator_combat_framework.module.entity_animation.api.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.controller.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.controller.BedrockAnimationController
import architecture.resonator_combat_framework.module.entity_animation.controller.IAnimationController
import architecture.resonator_combat_framework.module.entity_animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.entity_animation.data.ProxyBoneFlags
import architecture.resonator_combat_framework.module.entity_animation.registry.BedrockAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.ProxyBoneConfigRegistry
import architecture.resonator_combat_framework.module.entity_animation.util.BoneTransformUtil
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

/** 实体动画映射器基类——管理控制器生命周期、触发/停止/暂停/恢复 */
abstract class EntityAnimationMapper<T : Entity, M : EntityModel<T>>
@JvmOverloads
constructor(
	val entity: T,
	override val isClient: Boolean = entity.level().isClientSide
) : IAnimationMapper {

	val configLoader: ProxyBoneConfigRegistry = ProxyBoneConfigRegistry.getInstance(isClient)

	val animationLoader: BedrockAnimationRegistry = BedrockAnimationRegistry.getInstance(isClient)

	override val animationControllerManager = AnimationControllerManager()

	// ---- 触发 ----

	/** 触发动画：解析控制器 → 设置骨骼配置 → 触发控制器 */
	override fun trigger(playData: AnimationPlayData) {
		if (animationLoader.get(playData.animId) == null) {
			RcfConstants.LOGGER.warn("[AnimDebug] Animation not found: " + playData.animId)
			return
		}
		val controller = animationControllerManager.get(playData.controllerName) ?: mainController
		val loaded = configLoader.getConfig(playData.animId)
		val used = playData.boneConfig ?: loaded
		val bac = controller as BedrockAnimationController
		bac.resolvedBoneConfig = used
		controller.trigger(playData)
	}

	// ---- 停止 ----

	/** 停止指定控制器的动画 */
	override fun stop(controllerName: ResourceLocation, fadeOutTicks: Int) {
		(animationControllerManager.get(controllerName) ?: mainController).stop(fadeOutTicks)
	}

	/** 停止所有控制器的动画 */
	override fun stopAll(fadeOutTicks: Int) {
		animationControllerManager.getAll().forEach { it.stop(fadeOutTicks) }
	}

	// ---- 暂停/恢复 ----

	/** 暂停指定控制器的动画 */
	override fun pause(controllerName: ResourceLocation) {
		(animationControllerManager.get(controllerName) ?: mainController).pause()
	}

	/** 恢复指定控制器的动画 */
	override fun resume(controllerName: ResourceLocation) {
		(animationControllerManager.get(controllerName) ?: mainController).resume()
	}

	/** 暂停所有控制器的动画 */
	override fun pauseAll() = animationControllerManager.getAll().forEach { it.pause() }

	/** 恢复所有控制器的动画 */
	override fun resumeAll() = animationControllerManager.getAll().forEach { it.resume() }

	// ---- 状态查询 ----

	/** 是否有任意控制器活跃 */
	override fun isActive(): Boolean = animationControllerManager.isAnyActive()

	/** 获取指定控制器（不存在返回主控制器） */
	override fun getController(controllerName: ResourceLocation): IAnimationController? =
		animationControllerManager.get(controllerName) ?: mainController

	/** 获取所有控制器 */
	override fun controllers(): List<IAnimationController> = animationControllerManager.getAll()

	/** 是否存在指定控制器 */
	override fun hasController(controllerName: ResourceLocation): Boolean =
		animationControllerManager.has(controllerName)

	// ---- 控制器管理 ----

	/** 添加控制器（禁止覆盖 MAIN） */
	protected fun addController(name: ResourceLocation, controller: IAnimationController) {
		if (name == AnimationControllers.MAIN) return
		animationControllerManager.add(name, controller)
	}

	/** 移除控制器（禁止移除 MAIN） */
	protected fun removeController(name: ResourceLocation) {
		if (name == AnimationControllers.MAIN) return
		animationControllerManager.remove(name)
	}

	// ---- 高级查询 ----

	/** 获取所有活跃控制器（按优先级排序） */
	override fun getActiveControllersSorted(): List<IAnimationController> =
		animationControllerManager.getSortedActive()

	/** 获取可渲染的控制器列表 */
	override fun getRenderableControllers(): List<IAnimationController> =
		animationControllerManager.getRenderable()

	/** 查找阻塞指定控制器的高优先级控制器 */
	override fun findBlockingControllers(controller: IAnimationController): List<IAnimationController> =
		animationControllerManager.findBlocking(controller)

	// ---- Tick ----

	/** 游戏刻推进 */
	override fun tickAnimations() = animationControllerManager.tickAnimations(this)

	// ---- 配置 ----

	/** 获取动画的骨骼配置 */
	override fun resolveConfig(animId: String): ProxyBoneConfigData = configLoader.getConfig(animId)

	// ---- 骨骼应用 ----

	/** 将代理骨骼数据映射到体 model */
	abstract fun applyProxyToModel(
		proxyModel: ProxyModel, model: M,
		flags: Map<String, ProxyBoneFlags>
	)

	/** 将 root 骨骼变换应用到 PoseStack */
	fun applyRootTransform(
		proxyModel: ProxyModel, poseStack: PoseStack,
		flags: Map<String, ProxyBoneFlags>
	) {
		if (!isClient) return
		val bone = proxyModel.getBone("root") ?: return
		val t = BoneTransformUtil.computeForPoseStack(bone, flags["root"], 1f, flipY = true)
		BoneTransformUtil.applyTo(poseStack, t)
	}
}
