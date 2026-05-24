package architecture.resonator_combat_framework.module.player_animation.init

import architecture.resonator_combat_framework.module.player_animation.config.ProxyBoneState

/// 在这注册骨骼状态
object ProxyBoneStateRegistry {
	private val defaultState = object : ProxyBoneState {}
	private val states = mutableMapOf<String, ProxyBoneState>()

	init {
		register("lock", object : ProxyBoneState {
			override fun lockVanilla() = true
		})
	}

	fun register(name: String, state: ProxyBoneState) {
		states[name] = state
	}

	fun get(name: String): ProxyBoneState = states[name] ?: defaultState
}