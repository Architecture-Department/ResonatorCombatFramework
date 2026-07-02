package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.module.entity_state_machine.combat.AttackActionProperty
import org.jetbrains.annotations.NotNull

/**
 * 动作属性定义 —— 持有 [AttackActionProperty] 实例，
 * 供 Action 系统在运行时读取和修改行为属性。
 */
object AnimationProperties {
	@JvmField
	val DAMAGE_MULTIPLIER = AttackActionProperty<@NotNull Float>(
		"damage_multiplier"
	)
}
