package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.goldenboughs_lib.api.AllOpe
import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationDef
import architecture.resonator_combat_framework.module.entity_animation.event.AnimationDefRegistryEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

/**
 * 动画定义注册表，通过资源重载监听器收集所有 [AnimationDef]。
 * 在数据包重载时通过事件系统从各个模组收集动画定义，并提供懒加载能力。
 */
@AllOpe
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

	override fun prepare(
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	): Map<ResourceLocation, LazySupplier<AnimationDef>> {
		return FORGE_BUS.post(AnimationDefRegistryEvent()).getAll()
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
