package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.module.entity_animation.IAnimationProvider
import architecture.resonator_combat_framework.module.entity_animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationDef
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayConfig
import architecture.resonator_combat_framework.module.entity_state_machine.combat.Action
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionSequence
import architecture.resonator_combat_framework.module.entity_state_machine.combat.ActionState
import architecture.resonator_combat_framework.module.entity_state_machine.combat.InterruptData
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.util.function.Supplier

/**
 * 动画动作。
 *
 * 将动画定义（[AnimationDef]）包装为状态机可调度的 [Action]，
 * 在动作启动/结束/速度变化时自动控制对应动画控制器的播放、停止与速度同步。
 *
 * 动作阶段划分为：WINDUP（淡入）→ ACTIVE（动画播放）→ RECOVERY（淡出）。
 *
 * @param T 动画定义的具体类型
 * @property animation 动画定义的延迟提供者
 * @property controllerId 目标动画控制器的 ID
 * @property fadeInTick 淡入过渡的 tick 数
 * @property animationTick 动画持续 tick 数
 * @property fadeOutTick 淡出过渡的 tick 数
 * @property weight 动作权重
 */
class AnimationAction<T : AnimationDef>
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

	/**
	 * 获取动画定义实例。
	 *
	 * @return 动画定义实例
	 * @throws IllegalArgumentException 当动画提供者返回 null 时
	 */
	fun getAnimation(): T {
		val animation = animation.get()
		animation ?: throw IllegalArgumentException("AnimationAction: AttackAnimation not found: ${this.animation}")
		return animation
	}

	override fun onStart(entity: LivingEntity, actionSequence: ActionSequence?) {
		super.onStart(entity, actionSequence)
		if (entity is IAnimationProvider) {
			getController(entity)?.trigger(
				getAnimation(), PlayConfig(
					fadeInTicks = fadeInTick,
					fadeOutTicks = fadeOutTick
				)
			)
		}
	}

	override fun onEnd(entity: LivingEntity, actionSequence: ActionSequence?) {
		super.onEnd(entity, actionSequence)
		if (entity is IAnimationProvider) {
			getController(entity)?.stop()
		}
	}

	override fun onSpeedModify(entity: LivingEntity, actionSequence: ActionSequence?, oldValue: Float, newValue: Float) {
		super.onSpeedModify(entity, actionSequence, oldValue, newValue)
		if (entity is IAnimationProvider) {
			getController(entity)?.speedMultiplier = newValue
		}
	}

	/**
	 * 获取与当前动作关联的动画控制器。
	 *
	 * @param entity 实现了 [IAnimationProvider] 的实体
	 * @return 对应的动画控制器，可能为 null
	 */
	protected fun getController(entity: IAnimationProvider): IEntityAnimationController<out Entity>? {
		return entity.getMapperProvider().getController(controllerId)
	}
}
