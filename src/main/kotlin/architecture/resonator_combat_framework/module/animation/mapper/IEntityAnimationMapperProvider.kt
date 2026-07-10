package architecture.resonator_combat_framework.module.animation.mapper

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.animation.data.BoneConfig
import architecture.resonator_combat_framework.module.animation.data.PlayConfig
import architecture.resonator_combat_framework.module.animation.molang.MolangData
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

/**
 * 实体动画映射器接口，定义实体动画系统的核心操作契约。
 * 整合了动画触发、停止、暂停/恢复、状态查询和每帧渲染等核心功能。
 *
 * @param T 实体类型
 * @param M 实体模型类型
 */
@AllOpe
interface IEntityAnimationMapperProvider<T : Entity, M : EntityModel<T>> {
	/** 所属实体 */
	val holder: T

	/** 是否为客户端 */
	val isClient: Boolean

	/** 动画控制器管理器 */
	val animationControllerManager: AnimationControllerManager<T>

	/** 实体级 MoLang 数据 */
	val molangData: MolangData get() = MolangData.of(holder)

	/** 主控制器 */
	val mainController: IEntityAnimationController<T> get() = animationControllerManager.getMainController()

	/**
	 * 游戏刻推进。
	 */
	fun tick()

	/**
	 * 客户端每帧渲染（Mixin 调用）。
	 *
	 * @param model 实体模型
	 * @param partialTick 渲染帧插值系数
	 * @param poseStack 姿态栈
	 */
	fun tickAndRender(model: M, partialTick: Float, poseStack: PoseStack)

	/** 是否有控制器激活 */
	fun isActive(): Boolean

	/**
	 * 获取指定控制器。
	 *
	 * @param controllerName 控制器名称
	 * @return 控制器实例，不存在返回 null
	 */
	fun getController(controllerName: ResourceLocation?): IEntityAnimationController<T>?

	/**
	 * 获取所有控制器。
	 *
	 * @return 控制器列表
	 */
	fun controllers(): List<IEntityAnimationController<*>>

	/**
	 * 是否存在指定控制器。
	 *
	 * @param controllerName 控制器名称
	 * @return 是否存在
	 */
	fun hasController(controllerName: ResourceLocation): Boolean

	/**
	 * 获取所有激活的控制器（按优先级排序）。
	 *
	 * @return 活跃控制器列表
	 */
	fun getActiveControllersSorted(): List<IEntityAnimationController<T>>

	/**
	 * 获取所有可渲染的控制器。
	 *
	 * @return 可渲染控制器列表
	 */
	fun getRenderableControllers(): List<IEntityAnimationController<T>>

	/**
	 * 获取阻塞指定控制器的更高优先级控制器。
	 *
	 * @param controller 目标控制器
	 * @return 阻塞的控制器列表
	 */
	fun findBlockingControllers(controller: IEntityAnimationController<T>): List<IEntityAnimationController<T>>

	/**
	 * 停止指定控制器的动画。
	 *
	 * @param controllerName 控制器名称
	 * @param fadeOutTime 淡出时长（秒），-1f 表示立即停止
	 */
	fun stop(controllerName: ResourceLocation, fadeOutTime: Float = -1f)

	/**
	 * 停止主控制器的动画。
	 *
	 * @param fadeOutTime 淡出时长（秒）
	 */
	fun stop(fadeOutTime: Float = -1f) {
		stop(AnimationControllers.MAIN, fadeOutTime)
	}

	/**
	 * 停止所有控制器的动画。
	 *
	 * @param fadeOutTime 淡出时长（秒）
	 */
	fun stopAll(fadeOutTime: Float = -1f)

	/**
	 * 暂停指定控制器的动画。
	 *
	 * @param controllerName 控制器名称
	 */
	fun pause(controllerName: ResourceLocation = AnimationControllers.MAIN)

	/**
	 * 恢复指定控制器的动画。
	 *
	 * @param controllerName 控制器名称
	 */
	fun resume(controllerName: ResourceLocation = AnimationControllers.MAIN)

	/** 暂停所有控制器的动画 */
	fun pauseAll()

	/** 恢复所有控制器的动画 */
	fun resumeAll()

	/**
	 * 触发指定控制器播放动画。
	 *
	 * @param controllerName 控制器名称
	 * @param animId 动画 ID
	 * @param playData 播放配置
	 */
	fun trigger(controllerName: ResourceLocation?, animId: ResourceLocation, playData: PlayConfig)

	/**
	 * 触发主控制器播放动画。
	 *
	 * @param animId 动画 ID
	 * @param playData 播放配置
	 */
	fun trigger(animId: ResourceLocation, playData: PlayConfig) {
		trigger(AnimationControllers.MAIN, animId, playData)
	}
}
