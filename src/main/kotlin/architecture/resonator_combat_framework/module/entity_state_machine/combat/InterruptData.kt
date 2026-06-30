package architecture.resonator_combat_framework.module.entity_state_machine.combat

import architecture.goldenboughs_lib.api.AllOpe
import net.minecraft.world.entity.LivingEntity


/**
 * 打断配置 —— 定义攻击段各部分的可打断性及外部规则。
 *
 * 如果定义为-1则不可能被打断
 */
@AllOpe
data class InterruptData(
	val interruptions: Map<ActionState, Int> = DEFAULT,
	val rules: Array<out InterruptFunction> = emptyArray(),
) {
	constructor(
		windup: Int = 1000,
		attack: Int = 10000,
		recovery: Int = 1000,
		excessive: Int = 100,
		idle: Int = 0,
		vararg rules: InterruptFunction
	) : this(
		mapOf(
			ActionState.WINDUP to windup,
			ActionState.ATTACK to attack,
			ActionState.RECOVERY to recovery,
			ActionState.EXCESSIVE to excessive,
			ActionState.IDLE to idle,
		),
		rules
	)

	companion object {
		@JvmField
		val DEFAULT = mapOf(
			ActionState.WINDUP to 5000,
			ActionState.ATTACK to 10000,
			ActionState.RECOVERY to 1000,
			ActionState.EXCESSIVE to 100,
			ActionState.IDLE to 0,
		)
	}

	fun getInterruptWeight(actionState: ActionState): Int {
		return interruptions[actionState] ?: 0
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is InterruptData) return false

		if (interruptions != other.interruptions) return false
		if (!rules.contentEquals(other.rules)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = interruptions.hashCode()
		result = 31 * result + rules.contentHashCode()
		return result
	}
}

@AllOpe
interface InterruptFunction {
	// TODO
	fun canInterrupt(source: Action, target: Action, sourceEntity: LivingEntity): Int
}