package architecture.resonator_combat_framework.module.entity_animation.animation

import net.minecraft.world.entity.LivingEntity

/**
 * 动画属性键。
 * 用于通过 [StaticAnimation.addProperty] 链式配置动画的运行时行为。
 *
 * @param T 属性值的类型
 * @param name 唯一名称（调试用）
 * @param default 默认值，获取未设置的属性时返回此值
 */
class AnimationPropertyKey<T>(
	val name: String,
	val default: T
) {
	companion object {
		/** 播放速度修改器。签名：(StaticAnimation, LivingEntity, 当前速度) -> 新速度 */
		@JvmField
		val PLAY_SPEED_MODIFIER = AnimationPropertyKey<((StaticAnimation, LivingEntity, Float) -> Float)?>(
			"play_speed_modifier", null
		)
	}
}
