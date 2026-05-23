package architecture.resonator_combat_framework.module.player_animation.config

/// 在这注册骨骼状态
object BoneStateRegistry {
	private val defaultState = object : BoneState {}
	private val states = mutableMapOf<String, BoneState>()

	init {
		register("lock", object : BoneState {
			override fun lockVanilla() = true
		})
		register("no_fade_in", object : BoneState {
			override fun blendSpeedOverride() = 1f
		})
		register("no_fade_out", object : BoneState {
			override fun blendSpeedOverride() = 1f
		})
	}

	fun register(name: String, state: BoneState) {
		states[name] = state
	}

	fun get(name: String): BoneState = states[name] ?: defaultState
}
