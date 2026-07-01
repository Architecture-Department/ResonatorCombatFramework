package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.module.entity_animation.IProxyAnimationProvider
import architecture.resonator_combat_framework.module.entity_animation.IProxyAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.AnimationPlayData
import architecture.resonator_combat_framework.module.entity_state_machine.combat.Action
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionSequence
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionState
import architecture.resonator_combat_framework.module.entity_state_machine.combat.InterruptData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.util.function.Supplier

/**
 * 动画动作
 *
 * @param T
 * @constructor
 * @property animation 动画
 * @property controllerId 动画控制器id
 * @property fadeInTick 淡入
 * @property animationTick 动画时长
 * @property fadeOutTick 淡出
 * @property weight 权重
 * @param id
 * @param interruptData 打断数据
 */
class AnimationAction<T : StaticAnimation>
@JvmOverloads
constructor(
	id: ResourceLocation,
	val animation: Supplier<T?>,
	val controllerId: ResourceLocation?,
	val fadeInTick: Int = 1,
	val animationTick: Int,
	val fadeOutTick: Int = 1,
	interruptData: InterruptData = InterruptData.DEFAULT,
	override val weight: Int = 2500,
) : Action(
	id,
	fadeInTick + animationTick + fadeOutTick,
	interruptData,
	weight
) {
	override fun getState(time: Float, entity: LivingEntity): ActionState {
		return when {
			time < 0f -> ActionState.IDLE
			time < fadeInTick / 20f -> ActionState.WINDUP
			time < fadeInTick / 20f + animationTick / 20f -> ActionState.ACTIVE
			time < timeLength -> ActionState.RECOVERY
			else -> ActionState.IDLE
		}
	}

	fun getAnimation(): T {
		val animation = animation.get()
		animation ?: throw IllegalArgumentException("AnimationAction: AttackAnimation not found: ${this.animation}")
		return animation
	}

	override fun onStart(entity: LivingEntity, actionSequence: ActionSequence?) {
		super.onStart(entity, actionSequence)
		if (entity is IProxyAnimationProvider) {
			getController(entity)?.trigger(
				getAnimation(), AnimationPlayData(
					fadeInTicks = fadeInTick,
					fadeOutTicks = fadeOutTick
				)
			)
		}
	}

	override fun onEnd(entity: LivingEntity, actionSequence: ActionSequence?) {
		super.onEnd(entity, actionSequence)
		if (entity is IProxyAnimationProvider) {
			getController(entity)?.stop()
		}
	}

	override fun onSpeedModify(entity: LivingEntity, actionSequence: ActionSequence?, oldValue: Float, newValue: Float) {
		super.onSpeedModify(entity, actionSequence, oldValue, newValue)
		if (entity is IProxyAnimationProvider) {
			getController(entity)?.speedMultiplier = newValue
		}
	}

	protected fun getController(entity: IProxyAnimationProvider): IEntityAnimationController<out Entity>? {
		return entity.getMapperProvider().getController(controllerId)
	}
}
