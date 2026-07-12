package architecture.resonator_combat_framework.event

import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.animation.AnimationDef
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.Event

/**
 * [AnimationDef] 注册事件，用于将动画定义注册到动画系统中。
 * 支持懒加载（[LazySupplier]），动画定义仅在首次使用时才被初始化。
 */
class AnimationDefRegisterEvent : Event() {
	private val map = linkedMapOf<ResourceLocation, LazySupplier<AnimationDef>>()

	/**
	 * 注册一个动画定义。
	 * @param id 动画定义唯一标识
	 * @param function 接受 ID 并返回 [AnimationDef] 实例的工厂函数
	 * @return 动画定义的懒加载包装，可通过 [LazySupplier.get] 获取实际实例
	 */
	fun <T : AnimationDef> register(
		id: ResourceLocation,
		function: (id: ResourceLocation) -> T
	): LazySupplier<T> {
		@Suppress("RemoveExplicitTypeArguments") val supplier = LazySupplier<T>(id) { function(id) }
		@Suppress("UNCHECKED_CAST")
		map[id] = supplier as LazySupplier<AnimationDef>
		return supplier
	}

	/**
	 * 获取所有已注册的动画定义。
	 * @return 注册表映射的不可变快照（[ResourceLocation] -> [LazySupplier]）
	 */
	fun getAll(): Map<ResourceLocation, LazySupplier<AnimationDef>> = map
}
