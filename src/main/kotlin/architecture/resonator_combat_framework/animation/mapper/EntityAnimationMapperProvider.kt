package architecture.resonator_combat_framework.animation.mapper

import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.data.BoneFlags
import architecture.resonator_combat_framework.animation.data.PlayConfig
import architecture.resonator_combat_framework.init.RcfAnimationControllers
import architecture.resonator_combat_framework.model.PoseData
import architecture.resonator_combat_framework.util.ModelPartApplier
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

/**
 * 实体动画映射器基类，管理控制器生命周期和骨骼变换应用。
 * 实现了 [IEntityAnimationMapperProvider] 接口的核心功能：
 * - 动画触发/停止/暂停/恢复
 * - 游戏刻推进和渲染帧处理
 * - 骨骼配置解析和代理骨骼→模型映射
 *
 * @param T 实体类型
 * @param M 实体模型类型
 * @property holder 所属实体
 * @property isClient 是否为客户端
 * @property animationControllerManager 动画控制器管理器
 */
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

	// ---- 触发 ----

	/**
	 * 触发动画：解析控制器 → 设置骨骼配置 → 触发控制器。
	 *
	 * @param controllerName 控制器名称，null 则使用主控制器
	 * @param animId 动画 ID
	 * @param playData 播放配置
	 */
	override fun trigger(controllerName: ResourceLocation?, animId: ResourceLocation, playData: PlayConfig) {
		(animationControllerManager.get(controllerName ?: RcfAnimationControllers.MAIN) ?: mainController).trigger(
			animId,
			playData
		)
	}

	// ---- 停止 ----

	/**
	 * 停止指定控制器的动画。
	 *
	 * @param controllerName 控制器名称
	 * @param fadeOutTime 淡出时长（秒），-1 表示立即停止
	 */
	override fun stop(controllerName: ResourceLocation, fadeOutTime: Float) {
		(animationControllerManager.get(controllerName) ?: mainController).stop(fadeOutTime)
	}

	/**
	 * 停止所有控制器的动画。
	 *
	 * @param fadeOutTime 淡出时长（秒）
	 */
	override fun stopAll(fadeOutTime: Float) {
		animationControllerManager.getAll().forEach { it.stop(fadeOutTime) }
	}

	// ---- 暂停/恢复 ----

	/**
	 * 暂停指定控制器的动画。
	 *
	 * @param controllerName 控制器名称
	 */
	override fun pause(controllerName: ResourceLocation) {
		(animationControllerManager.get(controllerName) ?: mainController).pause()
	}

	/**
	 * 恢复指定控制器的动画。
	 *
	 * @param controllerName 控制器名称
	 */
	override fun resume(controllerName: ResourceLocation) {
		(animationControllerManager.get(controllerName) ?: mainController).resume()
	}

	/** 暂停所有控制器的动画 */
	override fun pauseAll() = animationControllerManager.getAll().forEach { it.pause() }

	/** 恢复所有控制器的动画 */
	override fun resumeAll() = animationControllerManager.getAll().forEach { it.resume() }

	// ---- 状态查询 ----

	/**
	 * 是否有任意控制器活跃。
	 *
	 * @return 是否有活跃控制器
	 */
	override fun isActive(): Boolean = animationControllerManager.isAnyActive()

	/**
	 * 获取指定控制器（不存在返回主控制器）。
	 *
	 * @param controllerName 控制器名称
	 * @return 控制器实例
	 */
	override fun getController(controllerName: ResourceLocation?): IEntityAnimationController<T>? =
		animationControllerManager.get(controllerName ?: RcfAnimationControllers.MAIN) ?: mainController

	/**
	 * 获取所有控制器。
	 *
	 * @return 控制器列表
	 */
	override fun controllers(): List<IEntityAnimationController<T>> = animationControllerManager.getAll()

	/**
	 * 是否存在指定控制器。
	 *
	 * @param controllerName 控制器名称
	 * @return 是否存在
	 */
	override fun hasController(controllerName: ResourceLocation): Boolean =
		animationControllerManager.has(controllerName)

	// ---- 控制器管理 ----

	/**
	 * 添加控制器（禁止覆盖 MAIN）。
	 *
	 * @param name 控制器名称
	 * @param controller 控制器实例
	 */
	protected fun addController(name: ResourceLocation, controller: IEntityAnimationController<T>) {
		if (name == RcfAnimationControllers.MAIN) return
		animationControllerManager.add(name, controller)
	}

	/**
	 * 移除控制器（禁止移除 MAIN）。
	 *
	 * @param name 控制器名称
	 */
	protected fun removeController(name: ResourceLocation) {
		if (name == RcfAnimationControllers.MAIN) return
		animationControllerManager.remove(name)
	}

	// ---- 高级查询 ----

	/**
	 * 获取所有活跃控制器（按优先级排序）。
	 *
	 * @return 活跃控制器列表
	 */
	override fun getActiveControllersSorted(): List<IEntityAnimationController<T>> =
		animationControllerManager.getSortedActive()

	/**
	 * 获取可渲染的控制器列表。
	 *
	 * @return 可渲染控制器列表
	 */
	override fun getRenderableControllers(): List<IEntityAnimationController<T>> =
		animationControllerManager.getRenderable()

	/**
	 * 查找阻塞指定控制器的高优先级控制器。
	 *
	 * @param controller 目标控制器
	 * @return 阻塞的控制器列表
	 */
	override fun findBlockingControllers(controller: IEntityAnimationController<T>): List<IEntityAnimationController<T>> =
		animationControllerManager.findBlocking(controller)

	// ---- 游戏刻推进 ----

	/**
	 * 游戏刻推进，委托给动画控制器管理器。
	 */
	override fun tick() = animationControllerManager.tick()

	// ---- 骨骼应用 ----

	/**
	 * 由 Mixin 每帧调用：更新过渡状态 → 重新合并 → 渲染到模型。
	 *
	 * @param model 实体模型
	 * @param partialTick 渲染帧插值系数
	 * @param poseStack 姿态栈
	 */
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

	/**
	 * 将代理骨骼数据映射到 Minecraft 实体模型。
	 * 子类需根据具体的模型类型实现骨骼映射逻辑。
	 *
	 * @param poseData 代理骨骼姿态数据
	 * @param model 目标实体模型
	 * @param flags 骨骼标志映射
	 */
	abstract fun applyProxyToModel(
		poseData: PoseData, model: M,
		flags: Map<String, BoneFlags>
	)

	/**
	 * 将 root 骨骼变换应用到 PoseStack。
	 *
	 * @param poseData 代理骨骼姿态数据
	 * @param poseStack 姿态栈
	 * @param flags 骨骼标志映射
	 */
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
