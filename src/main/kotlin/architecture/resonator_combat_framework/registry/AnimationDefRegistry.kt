package architecture.resonator_combat_framework.registry

import architecture.goldenboughs_lib.api.AllOpen
import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.animation.AnimationDef
import architecture.resonator_combat_framework.core.RcfEventHooks
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller

/**
 * 动画定义注册表，通过资源重载监听器收集所有 [AnimationDef]。
 * 在数据包重载时通过事件系统从各个模组收集动画定义，并提供懒加载能力。
 */
@AllOpen
object AnimationDefRegistry :
	SimplePreparableReloadListener<Map<ResourceLocation, LazySupplier<AnimationDef>>>() {
	private val animationsDef = mutableMapOf<ResourceLocation, LazySupplier<AnimationDef>>()

	/**
	 * 获取指定 ID 的动画定义。
	 *
	 * @param id 动画定义 ID
	 * @return 动画定义的懒加载供应器，不存在时返回 null
	 */
	fun get(id: ResourceLocation): LazySupplier<AnimationDef>? = animationsDef[id]

	fun getAll(): Map<ResourceLocation, LazySupplier<AnimationDef>> = animationsDef

	/**
	 * 手动触发动画定义重建，不依赖 ResourceManager 重载周期。
	 * 在 [KeyframeAnimationRegistry] 更新（如同步数据包到达客户端）后调用，
	 * 重新从事件总线收集所有 [AnimationDef] 并初始化。
	 */
	fun rebuild() {
		val prepared = RcfEventHooks.animationDefRegister()
		animationsDef.clear()
		animationsDef.putAll(prepared)
		animationsDef.forEach { it.value.init() }
	}

	override fun prepare(
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	): Map<ResourceLocation, LazySupplier<AnimationDef>> {
		return RcfEventHooks.animationDefRegister()
	}

	override fun apply(
		map: Map<ResourceLocation, LazySupplier<AnimationDef>>,
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	) {
		animationsDef.clear()
		animationsDef.putAll(map)
		animationsDef.forEach {
			it.value.init()
		}
	}
}
