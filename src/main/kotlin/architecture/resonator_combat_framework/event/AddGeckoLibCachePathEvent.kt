package architecture.resonator_combat_framework.event

import net.neoforged.bus.api.Event
import java.util.*

class AddGeckoLibCachePathEvent : Event() {
	private val modelPaths: MutableList<String> = arrayListOf()
	private val animationPaths: MutableList<String> = arrayListOf()

	fun addModelPath(path: String) {
		modelPaths.add(path)
	}

	fun getModelPaths(): MutableList<String> {
		return Collections.synchronizedList(modelPaths)
	}

	fun addAnimationPath(path: String) {
		animationPaths.add(path)
	}

	fun getAnimationPaths(): MutableList<String> {
		return Collections.synchronizedList(animationPaths)
	}
}
