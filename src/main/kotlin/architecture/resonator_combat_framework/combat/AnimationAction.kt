package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.init.RcfAttachmentTypes
import architecture.resonator_combat_framework.module.animation.AnimationDef
import architecture.resonator_combat_framework.module.animation.IAnimationProvider
import architecture.resonator_combat_framework.module.animation.IAnimationProvider.Companion.getMapperProvider
import architecture.resonator_combat_framework.module.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.animation.data.PlayConfig
import architecture.resonator_combat_framework.module.combat.*
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.util.function.Supplier

/**
 * 动画动作。
 *
 * 将动画定义（[AnimationDef]）包装为状态机可调度的 [architecture.resonator_combat_framework.module.combat.Action]，
 * 在动作启动/结束/速度变化时自动控制对应动画控制器的播放、停止与速度同步。
 * 状态修饰通过 [addProperty] 存入 [architecture.resonator_combat_framework.module.combat.Action.properties]，
 * 以 [architecture.resonator_combat_framework.module.combat.BooleanStateProperty] 或 [architecture.resonator_combat_framework.module.combat.FloatStateProperty] 为键，运行时自动应用。
 *
 * @property animation 动画定义的延迟提供者
 * @property controllerId 目标动画控制器的 ID
 * @property fadeInTime 淡入过渡时间（秒）
 * @property animationTime 动画持续时间（秒）
 * @property fadeOutTime 淡出过渡时间（秒）
 * @property weight 动作权重
 */
class AnimationAction
@JvmOverloads
constructor(
	id: ResourceLocation,
	val animation: Supplier<out AnimationDef?>,
	val controllerId: ResourceLocation?,
	val fadeInTime: Float = 1f / 20f,
	val animationTime: Float,
	val fadeOutTime: Float = 1f / 20f,
	interruptData: InterruptData = InterruptData.DEFAULT,
	override val weight: Int = 2500,
) : Action(
	id,
	fadeInTime + animationTime + fadeOutTime,
	interruptData,
	weight
) {
	val activeTime = fadeInTime + animationTime

	override val durationTime = fadeInTime + animationTime + fadeOutTime

	override fun getState(time: Float, entity: LivingEntity): ActionState {
		return when {
			time < 0f -> ActionState.IDLE
			time < fadeInTime -> ActionState.WINDUP
			time < activeTime -> ActionState.ACTIVE
			time < durationTime -> ActionState.RECOVERY
			else -> ActionState.IDLE
		}
	}

	/**
	 * 获取动画定义实例。
	 */
	fun getAnimation(): AnimationDef {
		val anim = animation.get()
		anim ?: throw IllegalArgumentException("AnimationAction: Animation not found: ${this.animation}")
		return anim
	}

	override fun onStart(entity: LivingEntity, actionController: ActionController, actionSequence: ActionSequence?) {
		super.onStart(entity, actionController, actionSequence)
		applyModifiers(entity)
		if (entity !is IAnimationProvider) return
		val combatSpeedMultiplier = actionController.combatSpeedMultiplier
		getAnimationController(entity)?.trigger(
			getAnimation(), PlayConfig(
				fadeInTime = fadeInTime / combatSpeedMultiplier,
				fadeOutTime = fadeOutTime / combatSpeedMultiplier
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

	/**
	 * 从 [Action.properties] 中读取 [architecture.resonator_combat_framework.module.combat.BooleanStateProperty] 和 [architecture.resonator_combat_framework.module.combat.FloatStateProperty] 键，
	 * 应用到 [EntityStateHolder]。
	 */
	@Suppress("UNCHECKED_CAST")
	private fun applyModifiers(entity: LivingEntity) {
		val stateHolder = entity.getExistingData(RcfAttachmentTypes.STATE_HOLDER).orElse(null) ?: return
		val boolMods = properties.filterKeys { it is BooleanStateProperty }
		if (boolMods.isNotEmpty()) {
			stateHolder.applyStateModifiers(
				boolMods.mapKeys { RcfUtil.modRl(it.key.name) }.mapValues { it.value as Boolean }
			)
		}
		val floatMods = properties.filterKeys { it is FloatStateProperty }
		if (floatMods.isNotEmpty()) {
			stateHolder.applyFloatModifiers(
				floatMods.mapKeys { RcfUtil.modRl(it.key.name) }.mapValues { it.value as Float }
			)
		}
	}

	/**
	 * 清理由本动作应用的状态修饰，恢复默认值。
	 */
	@Suppress("UNCHECKED_CAST")
	private fun clearModifiers(entity: LivingEntity) {
		val stateHolder = entity.getExistingData(RcfAttachmentTypes.STATE_HOLDER).orElse(null) ?: return
		val boolKeys = properties.keys.filterIsInstance<BooleanStateProperty>()
		if (boolKeys.isNotEmpty()) {
			stateHolder.applyStateModifiers(
				boolKeys.associate { RcfUtil.modRl(it.name) to false }
			)
		}
		val floatKeys = properties.keys.filterIsInstance<FloatStateProperty>()
		if (floatKeys.isNotEmpty()) {
			stateHolder.applyFloatModifiers(
				floatKeys.associate { RcfUtil.modRl(it.name) to 0f }
			)
		}
	}

	/**
	 * 获取与当前动作关联的动画控制器。
	 */
	protected fun getAnimationController(entity: IAnimationProvider): IEntityAnimationController<out Entity>? {
		return entity.getMapperProvider().getController(controllerId)
	}
}
