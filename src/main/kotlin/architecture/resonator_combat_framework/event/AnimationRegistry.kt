package architecture.resonator_combat_framework.event

import net.neoforged.bus.api.Event

class AnimationRegistry : Event() {
	companion object {
		@JvmStatic
		internal val CLIENTS = mutableListOf<() -> Unit>()

		@JvmStatic
		internal val SERVERS = mutableListOf<() -> Unit>()
	}

	@JvmOverloads
	fun registerReloadListener(
		isClient: Boolean? = null,
		function: () -> Unit
	) {
		when (isClient) {
			true -> {
				CLIENTS.add(function)
			}

			false -> {
				SERVERS.add(function)
			}

			else -> {
				CLIENTS.add(function)
				SERVERS.add(function)
			}
		}
	}
}