package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.animation.AttackAnimationDef
import architecture.resonator_combat_framework.init.AnimationProperties
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionState
import architecture.resonator_combat_framework.module.entity_state_machine.combat.InterruptData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import java.util.function.Supplier

/**
 * 攻击动画动作。
 *
 * 扩展 [AnimationAction]，为攻击动画提供前摇（WINDUP）、执行（ACTIVE）、后摇（RECOVERY）三个阶段的时间划分，
 * 并在获取动画实例时自动注入伤害倍率属性。
 *
 * @param T 攻击动画定义的具体类型
 * @property windupTick 总前摇 tick 数（淡入 + 前摇）
 * @property activeTick 执行阶段 tick 数
 * @property recoveryTick 总后摇 tick 数（后摇 + 淡出）
 * @property damageMultiplier 伤害倍率，在获取动画时注入到动画属性中
 */
class AttackAnimationAction<T : AttackAnimationDef>(
	id: ResourceLocation,
	animation: Supplier<T?>,
	controllerId: ResourceLocation?,
	fadeInTick: Int = 1,
	windupTick: Int = 0,
	val activeTick: Int = 4,
	recoveryTick: Int = 2,
	fadeOutTick: Int = 1,
	interruptData: InterruptData = InterruptData(),
	weight: Int = 2500,
	val damageMultiplier: Float = 1.0f
) : AnimationAction<T>(
	id,
	animation,
	controllerId,
	fadeInTick,
	windupTick + activeTick + recoveryTick,
	fadeOutTick,
	interruptData,
	weight
) {
	/**
	 * 前摇 tick 数（包含 [fadeInTick]）。
	 */
	val windupTick: Int = fadeInTick + windupTick

	/**
	 * 后摇 tick 数（包含 [fadeOutTick]）。
	 */
	val recoveryTick: Int = recoveryTick + fadeOutTick

	override fun getState(time: Float, entity: LivingEntity): ActionState {
		return when {
			time < 0f -> ActionState.IDLE
			time < windupTick / 20f -> ActionState.WINDUP
			time < windupTick / 20f + activeTick / 20f -> ActionState.ACTIVE
			time < timeLength / 20f -> ActionState.RECOVERY
			else -> ActionState.IDLE
		}
	}

	override fun getAnimation(): T {
		val animation = super.getAnimation()
		animation.addProperty(AnimationProperties.DAMAGE_MULTIPLIER, damageMultiplier)
		return animation
	}
}
