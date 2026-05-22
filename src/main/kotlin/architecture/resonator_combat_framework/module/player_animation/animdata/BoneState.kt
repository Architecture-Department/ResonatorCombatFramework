package architecture.resonator_combat_framework.module.player_animation.animdata

interface BoneState {
	fun lockVanilla(): Boolean = false
	fun blendSpeedOverride(): Float? = null
}

object LockState : BoneState {
	override fun lockVanilla() = true
}

object NoFadeInState : BoneState {
	override fun blendSpeedOverride() = 1f
}

object NoFadeOutState : BoneState {
	override fun blendSpeedOverride() = 1f
}

object BoneStateRegistry {
	private val states = mutableMapOf<String, BoneState>()

	init {
		register("lock", LockState)
		register("no_fade_in", NoFadeInState)
		register("no_fade_out", NoFadeOutState)
	}

	fun register(name: String, state: BoneState) {
		states[name] = state
	}

	fun get(name: String): BoneState? = states[name]

	fun isLockState(flagName: String): Boolean {
		val state = states[flagName] ?: return false
		return state.lockVanilla()
	}

	fun getBlendSpeedOverride(flagNames: Set<String>): Float? {
		for (name in flagNames) {
			val speed = states[name]?.blendSpeedOverride()
			if (speed != null) return speed
		}
		return null
	}
}
