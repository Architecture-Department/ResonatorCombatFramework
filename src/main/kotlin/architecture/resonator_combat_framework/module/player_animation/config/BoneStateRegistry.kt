package architecture.resonator_combat_framework.module.player_animation.config

/// 在这注册骨骼状态
object BoneStateRegistry {
	private val defaultState = object : BoneState {}
	private val states = mutableMapOf<String, BoneState>()

	init {
		register("lock", object : BoneState {
			override fun lockVanilla() = true
		})
	}

	fun register(name: String, state: BoneState) {
		states[name] = state
	}

	fun get(name: String): BoneState = states[name] ?: defaultState
}
