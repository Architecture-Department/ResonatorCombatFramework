package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.init.RcfAttachmentTypes
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
 * @param T 动画定义的具体类型
 * @property animation 动画定义的延迟提供者
 * @property controllerId 目标动画控制器的 ID
 * @property fadeInTick 淡入过渡的 tick 数
 * @property animationTick 动画持续 tick 数
 * @property fadeOutTick 淡出过渡的 tick 数
 * @property stateModifiers 动画播放期间应用的实体布尔状态修饰（如 CAN_MOVE、CAN_LOOK_AROUND）
 * @property floatModifiers 动画播放期间应用的实体浮点状态修饰（如 SPEED_MODIFIER、MAX_LOOK_SPEED）
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
    /** 动画播放期间应用的实体布尔状态修饰（如 CAN_MOVE、CAN_LOOK_AROUND） */
    val stateModifiers: Map<ResourceLocation, Boolean> = emptyMap(),
    /** 动画播放期间应用的实体浮点状态修饰（如 SPEED_MODIFIER、MAX_LOOK_SPEED） */
    val floatModifiers: Map<ResourceLocation, Float> = emptyMap(),
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
        // 应用状态修饰
        applyModifiers(entity)
        // 触发动画
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
        // 清理状态修饰
        clearModifiers(entity)
        if (entity is IAnimationProvider) {
            getController(entity)?.stop()
        }
    }

    override fun onSpeedModify(
        entity: LivingEntity,
        actionSequence: ActionSequence?,
        oldValue: Float,
        newValue: Float
    ) {
        super.onSpeedModify(entity, actionSequence, oldValue, newValue)
        if (entity is IAnimationProvider) {
            getController(entity)?.speedMultiplier = newValue
        }
    }

    /**
     * 强制结束动作（无过渡停止动画）。
     * 用于强制切物品等需要立即中断的场景。
     */
    fun onForcedEnd(entity: LivingEntity, actionSequence: ActionSequence?) {
        clearModifiers(entity)
        if (entity is IAnimationProvider) {
            getController(entity)?.stop(0)
        }
    }

    /**
     * 将状态修饰应用到 [EntityStateHolder]。
     */
    private fun applyModifiers(entity: LivingEntity) {
        val stateHolder = entity.getExistingData(RcfAttachmentTypes.STATE_HOLDER).orElse(null) ?: return
        if (stateModifiers.isNotEmpty()) stateHolder.applyStateModifiers(stateModifiers)
        if (floatModifiers.isNotEmpty()) stateHolder.applyFloatModifiers(floatModifiers)
    }

    /**
     * 清理由本动作应用的状态修饰。
     * 将已设置的修饰恢复到默认值。
     */
    private fun clearModifiers(entity: LivingEntity) {
        val stateHolder = entity.getExistingData(RcfAttachmentTypes.STATE_HOLDER).orElse(null) ?: return
        // 布尔状态恢复默认 false
        val reverseStates = stateModifiers.keys.associateWith { false }
        if (reverseStates.isNotEmpty()) stateHolder.applyStateModifiers(reverseStates)
        // 浮点状态恢复默认 0f
        val reverseFloats = floatModifiers.keys.associateWith { 0f }
        if (reverseFloats.isNotEmpty()) stateHolder.applyFloatModifiers(reverseFloats)
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
