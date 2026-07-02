package architecture.resonator_combat_framework.module.entity_animation.animation.controller

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationDef
import architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation.KeyframeAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.data.BoneConfig
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayConfig
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.animation.model.GeometryData
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

/** 动画控制器接口——定义实体动画控制器的完整行为契约。 */
@AllOpe
interface IEntityAnimationController<T : Entity> {
	/** 控制器状态机 */
	enum class State {
		/** 空闲状态 */
		IDLE,

		/** 过渡状态（淡入/淡出） */
		FADING_OUT,

		/** 动画间过渡状态（跨动画混合） */
		CROSSFADING,

		/** 播放状态 */
		PLAYING,

		/** 暂停状态 */
		PAUSED,
	}

	/** 控制器管理器 */
	val manager: AnimationControllerManager<T>

	/** 当前骨骼配置（由动画定义决定） */
	val currentBoneConfig: BoneConfig

	/** 当前烘培动画数据 */
	val currentBakingAnim: KeyframeAnimation?

	/** 额外模型数据（动画期间动态添加） */
	val extraModel: GeometryData?

	/** 控制器唯一标识 */
	val id: ResourceLocation

	/** 当前 MoLang 数据上下文 */
	val currentData: MolangData
		get() = manager.mapperProvider.molangData

	/** 控制器当前状态（外部只读） */
	val state: State

	/** 过渡源骨骼快照，用于 crossfade 混合 */
	val transitionSource: PoseData?

	/** 当前动画播放配置 */
	val currentConfig: PlayConfig

	/** 控制器局部骨骼配置（持久，外部设置后不会自动改变） */
	var localBoneConfig: BoneConfig

	/** 当前生效的骨骼配置（由 localBoneConfig + 动画配置合并，含镜像） */
	val activeBoneConfig: BoneConfig

	/** 当前混合因子 0~1 */
	val fadeProgress: Float

	/** 混合目标值（外部只读） */
	val fadeTarget: Float

	/** 过渡持续 tick 数（外部只读） */
	val currentTransitionTicks: Int

	/** 是否覆盖模式（true=覆盖低优先级，false=叠加） */
	val isOverriding: Boolean

	/** 播放速度倍率 */
	var speedMultiplier: Float

	/** 受影响的骨骼集合 */
	val affectedBones: Set<String>


	// ===== 状态查询 =====

	/** 控制器是否活跃（非 IDLE） */
	fun isActive(): Boolean

	/** 有效权重（过渡期间恒为 1，正常播放为 blendFactor） */
	val mergeWeight: Float

	/** 是否正在淡出（TRANSITIONING 状态） */
	val isFadingOut: Boolean

	/** 是否正在淡入（CROSSFADING 状态） */
	val isFadingIn: Boolean

	/** 当前动画播放时间（秒），含过渡混合时间 */
	val currentAnimTime: Float

	/** 当前播放的动画实例（外部只读） */
	val currentAnim: AnimationDef?

	/** 当前动画的资源 ID */
	val currentAnimId: ResourceLocation? get() = currentAnim?.animationId

	// ===== 触发/停止 =====

	/** 触发动画播放 */
	fun trigger(animId: ResourceLocation, config: PlayConfig = PlayConfig.EMPTY)

	/**
	 * 简易触发——使用常用参数快速播放动画。
	 *
	 * @param animId 动画资源 ID
	 * @param speedMultiplier 播放速度倍率
	 * @param fadeInTicks 淡入 tick 数（-1 使用默认值）
	 * @param fadeOutTicks 淡出 tick 数（-1 使用默认值）
	 */
	fun trigger(
		animId: ResourceLocation,
		speedMultiplier: Float = 1f,
		fadeInTicks: Int = -1,
		fadeOutTicks: Int = -1
	) = trigger(
		animId,
		PlayConfig(
			speedMultiplier = speedMultiplier,
			fadeInTicks = fadeInTicks,
			fadeOutTicks = fadeOutTicks
		)
	)

	/** 使用已解析的 [AnimationDef] 实例直接触发 */
	fun trigger(anim: AnimationDef, config: PlayConfig = PlayConfig.EMPTY)

	/** 判断当前播放的动画是否是指定 ID */
	fun equalsCurrentAnimId(id: ResourceLocation): Boolean {
		return currentAnimId == id
	}

	/** 停止动画（可指定淡出 tick 数） */
	fun stop(fadeOutTicks: Int = -1)

	/** 暂停动画 */
	fun pause()

	/** 恢复动画播放 */
	fun resume()

	/** 游戏刻推进（20tps） */
	fun tick()

	/** 合并后 tick */
	fun tickAdvance()

	/** 渲染帧更新过渡状态 */
	fun tickRender(deltaSec: Float)
}
