package architecture.resonator_combat_framework.module.entity_animation.animation.controller

import architecture.goldenboughs_lib.api.AllOpe
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

@AllOpe
/** 动画控制器接口 */
interface IEntityAnimationController<T : Entity> {
	val manager: AnimationControllerManager<T>

	/** 控制器唯一标识 */
	val id: ResourceLocation

	val currentData: MolangData
		get() = manager.mapper.molangData

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

	/** 控制器是否活跃（非 IDLE） */
	fun isActive(): Boolean

	/** 有效权重（含过渡状态） */
	val effectiveWeight: Float

	/** 是否正在淡出 */
	val isFadingOut: Boolean

	/** 是否正在淡入（TRANSITIONING 状态） */
	val isFadingIn: Boolean

	/** 当前动画播放时间（秒） */
	val currentAnimTime: Float

	/** 触发动画播放 */
	fun trigger(config: AnimationPlayData)

	fun trigger(
		animId: String,
		speedMultiplier: Float = 1f,
		fadeInTicks: Int = -1,
		fadeOutTicks: Int = -1
	) = trigger(
		AnimationPlayData(
			animId = animId,
			speedMultiplier = speedMultiplier,
			fadeInTicks = fadeInTicks,
			fadeOutTicks = fadeOutTicks
		)
	)

	fun equalsCurrentAnimId(id: String): Boolean {
		return currentAnimId == id
	}

	/** 停止动画 */
	fun stop(fadeOutTicks: Int = -1)

	/** 暂停动画 */
	fun pause()

	/** 恢复动画 */
	fun resume()

	/** 游戏刻推进（20tps） */
	fun tickAdvance()

	/** 渲染帧更新过渡状态 */
	fun tickRender(deltaSec: Float)
}
