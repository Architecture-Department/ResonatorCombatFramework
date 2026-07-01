package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.animation.AttackAnimationDef
import architecture.resonator_combat_framework.init.AnimationPropertys
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionState
import architecture.resonator_combat_framework.module.entity_state_machine.combat.InterruptData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import java.util.function.Supplier

/**
 * 攻击动画动作
 *
 * @param T
 * @constructor
 * @param windupTick 前摇 Tick
 * @property activeTick 执行 Tick
 * @param recoveryTick 后摇 Tick
 * @property damageMultiplier 伤害倍率
 * @param id 动作id
 * @param animation 动画
 * @param controllerId 动画控制器id
 * @param fadeInTick 淡入 Tick
 * @param fadeOutTick 淡出 Tick
 * @param interruptData 打断数据
 * @param weight 权重
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
	 * 前摇 Tick
	 */
	val windupTick: Int = fadeInTick + windupTick

	/**
	 * 后摇 Tick
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
		animation.addProperty(AnimationPropertys.DAMAGE_MULTIPLIER, damageMultiplier)
		return animation
	}
}