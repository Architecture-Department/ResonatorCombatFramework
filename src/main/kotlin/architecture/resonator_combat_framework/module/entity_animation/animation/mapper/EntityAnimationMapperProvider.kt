package architecture.resonator_combat_framework.module.entity_animation.animation.mapper

import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayConfig
import architecture.resonator_combat_framework.module.entity_animation.animation.data.BoneConfig
import architecture.resonator_combat_framework.module.entity_animation.animation.data.BoneFlags
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.module.entity_animation.registry.BoneConfigRegistry
import architecture.resonator_combat_framework.module.entity_animation.registry.KeyframeAnimationRegistry
import architecture.resonator_combat_framework.module.entity_animation.util.ModelPartApplier
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

/** 实体动画映射器基类——管理控制器生命周期、触发/停止/暂停/恢复 */
abstract class EntityAnimationMapperProvider<T : Entity, M : EntityModel<T>>(
	override val holder: T,
	override val isClient: Boolean = holder.level().isClientSide,
	override val animationControllerManager: AnimationControllerManager<T> = AnimationControllerManager(holder)
) : IEntityAnimationMapperProvider<T, M> {

	constructor(holder: T) : this(holder, holder.level().isClientSide)

	/** 上一渲染帧的 tickSec，用于计算 deltaSec */
	protected var lastRenderTick = 0f

	/** 当前渲染帧的 partialTick，供 applyItemTransform 使用 */
	protected var currentPartialTick = 0f

	val configLoader: BoneConfigRegistry = BoneConfigRegistry.getInstance(isClient)

	val animationLoader: KeyframeAnimationRegistry = KeyframeAnimationRegistry.getInstance(isClient)


	// ---- 触发 ----

	/** 触发动画：解析控制器 → 设置骨骼配置 → 触发控制器 */
	override fun trigger(controllerName: ResourceLocation?, animId: ResourceLocation, playData: PlayConfig) {
		(animationControllerManager.get(controllerName ?: AnimationControllers.MAIN) ?: mainController).trigger(
			animId,
			playData
		)
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
	override fun getController(controllerName: ResourceLocation?): IEntityAnimationController<T>? =
		animationControllerManager.get(controllerName ?: AnimationControllers.MAIN) ?: mainController

	/** 获取所有控制器 */
	override fun controllers(): List<IEntityAnimationController<T>> = animationControllerManager.getAll()

	/** 是否存在指定控制器 */
	override fun hasController(controllerName: ResourceLocation): Boolean =
		animationControllerManager.has(controllerName)

	// ---- 控制器管理 ----

	/** 添加控制器（禁止覆盖 MAIN） */
	protected fun addController(name: ResourceLocation, controller: IEntityAnimationController<T>) {
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
	override fun getActiveControllersSorted(): List<IEntityAnimationController<T>> =
		animationControllerManager.getSortedActive()

	/** 获取可渲染的控制器列表 */
	override fun getRenderableControllers(): List<IEntityAnimationController<T>> =
		animationControllerManager.getRenderable()

	/** 查找阻塞指定控制器的高优先级控制器 */
	override fun findBlockingControllers(controller: IEntityAnimationController<T>): List<IEntityAnimationController<T>> =
		animationControllerManager.findBlocking(controller)

	// ---- 游戏刻推进 ----

	/** 游戏刻推进 */
	override fun tick() = animationControllerManager.tick()

	// ---- 配置 ----

	/** 获取动画的骨骼配置 */
	override fun resolveConfig(animId: ResourceLocation): BoneConfig =
		configLoader.get(animId) ?: BoneConfig.EMPTY

	// ---- 骨骼应用 ----

	/** 由 Mixin 每帧调用：更新过渡状态 → 重新合并 → 渲染到模型 */
	override fun tickAndRender(model: M, partialTick: Float, poseStack: PoseStack) {
		if (!isClient || !isActive()) return
		val tickSec = (holder.tickCount + partialTick) / 20f
		val deltaSec = if (lastRenderTick == 0f) 0f else tickSec - lastRenderTick
		lastRenderTick = tickSec
		currentPartialTick = partialTick

		for (ctrl in animationControllerManager.getAll()) {
			ctrl.tickRender(deltaSec)
		}
		// 更新 ParticleStorm 发射器的骨骼追踪
		animationControllerManager.updateEmitterTransforms(partialTick)
		val renderable = animationControllerManager.getRenderable()
		if (renderable.isEmpty()) return
		val interpolatedProxyModel = animationControllerManager.getInterpolatedProxy(partialTick)
		applyRootTransform(interpolatedProxyModel, poseStack, animationControllerManager.mergedBoneFlags)

		applyProxyToModel(interpolatedProxyModel, model, animationControllerManager.mergedBoneFlags)
	}

	/** 将代理骨骼数据映射到体 model */
	abstract fun applyProxyToModel(
		poseData: PoseData, model: M,
		flags: Map<String, BoneFlags>
	)

	/** 将 root 骨骼变换应用到 PoseStack */
	fun applyRootTransform(
		poseData: PoseData, poseStack: PoseStack,
		flags: Map<String, BoneFlags>
	) {
		if (!isClient) return
		val bone = poseData.getBone("root") ?: return
		val boneFlags = flags["root"]
		val t = ModelPartApplier.computeFor(bone, boneFlags, flipPY = true, except = true)
		ModelPartApplier.applyTo(poseStack, t, boneFlags, 1f)
	}
}
