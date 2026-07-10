package architecture.resonator_combat_framework.event

import net.neoforged.bus.api.Event
import java.util.*

/**
 * GeckoLib 缓存路径添加事件 —— 在 GeckoLib 缓存初始化时触发。
 * 用于注册需要 GeckoLib 处理的额外模型/动画资源路径。
 */
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
