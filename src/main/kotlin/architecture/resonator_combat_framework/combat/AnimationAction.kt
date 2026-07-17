package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.animation.AnimationDef
import architecture.resonator_combat_framework.animation.IAnimationProvider
import architecture.resonator_combat_framework.animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.data.PlayConfig
import architecture.resonator_combat_framework.registry.AnimationDefRegistry
import architecture.resonator_combat_framework.state.EntityStateHolder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

// TODO weight独立出去，controllerId独立出去
class AnimationAction
@JvmOverloads
constructor(
	id: ResourceLocation,
	/**
	 * 动画定义的延迟提供者
	 */
	val animationId: ResourceLocation,
	/**
	 * 目标动画控制器的 ID
	 */
	val controllerId: ResourceLocation?,
	/**
	 * 淡入过渡时间（秒）
	 */
	val fadeInTimeLength: Float = 1f / 20f,
	/**
	 * 动画持续时间（秒）
	 */
	val animationTimeLength: Float,
	/**
	 * 淡出过渡时间（秒）
	 */
	val fadeOutTimeLength: Float = 1f / 20f,
	interruptData: InterruptData = InterruptData.DEFAULT,
	/**
	 * 动作权重
	 */
	override val weight: Int = 2500,
) : Action(
	id,
	fadeInTimeLength + animationTimeLength + fadeOutTimeLength,
	interruptData,
	weight
) {

	fun isFadeIn(time: Float): Boolean {
		return 0f < time && time <= durationTimeLength
	}

	fun isAnimationTime(time: Float): Boolean {
		return durationTimeLength < time && time <= fadeInTimeLength + animationTimeLength
	}

	fun isFadeOut(time: Float): Boolean {
		return fadeInTimeLength + animationTimeLength < time && time <= fadeInTimeLength + animationTimeLength + fadeOutTimeLength
	}

	override fun getState(time: Float, entity: LivingEntity): ActionState {
		return when {
			time < 0f -> ActionState.IDLE
			time < fadeInTimeLength -> ActionState.WINDUP
			time < fadeInTimeLength + animationTimeLength -> ActionState.ACTIVE
			time < fadeInTimeLength + animationTimeLength + fadeOutTimeLength -> ActionState.RECOVERY
			else -> ActionState.IDLE
		}
	}

	/**
	 * 获取动画定义实例。
	 */
	fun getAnimation(): AnimationDef {
		val anim = AnimationDefRegistry.get(animationId)
			?: throw IllegalArgumentException("AnimationAction: Animation not found: ${this.animationId}")
		return anim.get()!!
	}

	override fun onStart(entity: LivingEntity, actionController: ActionController, actionSequence: ActionSequence?) {
		super.onStart(entity, actionController, actionSequence)
		applyModifiers(entity)
		if (entity !is IAnimationProvider) return
		val combatSpeedMultiplier = actionController.combatSpeedMultiplier
		getAnimationController(entity)?.trigger(
			animationId, PlayConfig(
				fadeInTime = fadeInTimeLength / combatSpeedMultiplier,
				fadeOutTime = fadeOutTimeLength / combatSpeedMultiplier
			)
		)
	}

	override fun onEnd(entity: LivingEntity, actionController: ActionController, actionSequence: ActionSequence?) {
		super.onEnd(entity, actionController, actionSequence)
		clearModifiers(entity)
		if (entity is IAnimationProvider) {
			getAnimationController(entity)?.stop()
		}
	}

	override fun onSpeedModify(
		entity: LivingEntity,
		actionController: ActionController,
		actionSequence: ActionSequence?,
		oldValue: Float,
		newValue: Float
	) {
		super.onSpeedModify(entity, actionController, actionSequence, oldValue, newValue)
		if (entity is IAnimationProvider) {
			getAnimationController(entity)?.speedMultiplier = newValue
		}
	}

	/**
	 * 强制结束动作（无过渡停止动画）。
	 */
	fun onForcedEnd(entity: LivingEntity, actionSequence: ActionSequence?) {
		clearModifiers(entity)
		if (entity is IAnimationProvider) {
			getAnimationController(entity)?.stop(0f)
		}
	}

	@Suppress("UNCHECKED_CAST")
	private fun applyModifiers(entity: LivingEntity) {
		if (!EntityStateHolder.has(entity)) return
		val stateHolder = EntityStateHolder.of(entity)

		val boolMods = properties.filterKeys { it is BooleanStateProperty }
		if (boolMods.isNotEmpty()) {
			stateHolder.applyStateModifiers(boolMods.mapKeys { it.key.id }.mapValues { it.value as Boolean })
		}

		val floatMods = properties.filterKeys { it is FloatStateProperty }
		if (floatMods.isNotEmpty()) {
			stateHolder.applyFloatModifiers(floatMods.mapKeys { it.key.id }.mapValues { it.value as Float })
		}
	}

	/**
	 * 清理由本动作应用的状态修饰，恢复默认值。
	 */
	@Suppress("UNCHECKED_CAST")
	private fun clearModifiers(entity: LivingEntity) {
		if (!EntityStateHolder.has(entity)) return
		val stateHolder = EntityStateHolder.of(entity)

		val boolKeys = properties.keys.filterIsInstance<BooleanStateProperty>()
		if (boolKeys.isNotEmpty()) {
			stateHolder.applyStateModifiers(boolKeys.associate { it.id to false })
		}
		val floatKeys = properties.keys.filterIsInstance<FloatStateProperty>()
		if (floatKeys.isNotEmpty()) {
			stateHolder.applyFloatModifiers(floatKeys.associate { it.id to 0f })
		}
	}

	/**
	 * 获取与当前动作关联的动画控制器。
	 */
	protected fun getAnimationController(entity: IAnimationProvider): IEntityAnimationController<out Entity>? {
		return entity.getMapperProvider().getController(controllerId)
	}

	override fun toString(): String {
		return "AnimationAction(id=$id, animationId=$animationId)"
	}
}
