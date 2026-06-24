package architecture.resonator_combat_framework.module.entity_animation.animation.controller

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.animation.data.ProxyBoneConfigData
import architecture.resonator_combat_framework.module.entity_animation.animation.mapper.AnimationControllerManager
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

/** 动画控制器接口 */
@AllOpe
interface IEntityAnimationController<T : Entity> {
	/** 控制器状态机 */
	enum class State {
		/** 空闲状态 */
		IDLE,

		/** 过渡状态（淡入/淡出） */
		TRANSITIONING,

		/** 动画间过渡状态（跨动画混合） */
		ANIMATION_TRANSITIONING,

		/** 播放状态 */
		PLAYING,

		/** 暂停状态 */
		PAUSED,
	}

	/** 控制器管理器 */
	val manager: AnimationControllerManager<T>

	/** 控制器唯一标识 */
	val id: ResourceLocation

	/** 当前 MoLang 数据上下文 */
	val currentData: MolangData
		get() = manager.mapper.molangData

	/** 控制器当前状态 */
	var state: State

	/** 过渡源骨骼快照，用于 crossfade 混合 */
	var transitionSource: ProxyModel?

	/** 当前动画播放配置 */
	var currentConfig: AnimationPlayData

	/** 外部注入的骨骼配置（一次性，trigger 消费后清空） */
	var resolvedBoneConfig: ProxyBoneConfigData?

	/** 当前生效的骨骼配置 */
	var activeBoneConfig: ProxyBoneConfigData

	/** 骨骼配置覆盖（合并到 activeBoneConfig） */
	var boneConfigs: ProxyBoneConfigData?

	/** 当前混合因子 0~1 */
	var blendFactor: Float

	/** 混合目标值 */
	var blendTarget: Float

	/** 过渡持续 tick 数 */
	var currentTransitionTicks: Int

	/** 播放速度倍率 */
	var speedMultiplier: Float

	/** 是否覆盖模式（true=覆盖低优先级，false=叠加） */
	val isOverriding: Boolean

	/** 当前播放的动画 ID */
	var currentAnimId: String?

	/** 受影响的骨骼集合 */
	var affectedBones: Set<String>

	/** 当前活跃的 ActionAnimation 状态修改器 */
	val activeStateModifiers: Map<ResourceLocation, Boolean>

	// ===== 状态查询 =====

	/** 控制器是否活跃（非 IDLE） */
	fun isActive(): Boolean

	/** 有效权重（过渡期间恒为 1，正常播放为 blendFactor） */
	val effectiveWeight: Float

	/** 是否正在淡出（TRANSITIONING 状态） */
	val isFadingOut: Boolean

	/** 是否正在淡入（ANIMATION_TRANSITIONING 状态） */
	val isFadingIn: Boolean

	/** 当前动画播放时间（秒），含过渡混合时间 */
	val currentAnimTime: Float

	// ===== 触发/停止 =====

	/** 触发动画播放 */
	fun trigger(animId: String, config: AnimationPlayData)

	/** 简易触发 */
	fun trigger(
		animId: String,
		speedMultiplier: Float = 1f,
		fadeInTicks: Int = -1,
		fadeOutTicks: Int = -1
	) = trigger(
		animId, AnimationPlayData(
			speedMultiplier = speedMultiplier,
			fadeInTicks = fadeInTicks,
			fadeOutTicks = fadeOutTicks
		)
	)

	/** 使用已解析的 [StaticAnimation] 实例直接触发 */
	fun triggerWithAnimation(anim: StaticAnimation, animId: String, config: AnimationPlayData)

	/** 判断当前播放的动画是否是指定 ID */
	fun equalsCurrentAnimId(id: String): Boolean {
		return currentAnimId == id
	}

	/** 停止动画（可指定淡出 tick 数） */
	fun stop(fadeOutTicks: Int = -1)

	/** 暂停动画 */
	fun pause()

	/** 恢复动画播放 */
	fun resume()

	/** 游戏刻推进（20tps） */
	fun tickAdvance()

	/** 渲染帧更新过渡状态 */
	fun tickRender(deltaSec: Float)
}
