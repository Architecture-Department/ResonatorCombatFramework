package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.goldenboughs_lib.api.AllOpe
import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.module.entity_animation.event.StaticAnimationRegistryEvent
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

@AllOpe
class StaticAnimationRegistry :
	SimplePreparableReloadListener<Map<ResourceLocation, LazySupplier<StaticAnimation>>>() {
	companion object {
		private val INSTANCE: StaticAnimationRegistry = StaticAnimationRegistry()

		@JvmStatic
		fun getInstance(): StaticAnimationRegistry {
			return INSTANCE
		}

		@JvmStatic
		fun get(id: ResourceLocation): LazySupplier<StaticAnimation>? {
			return getInstance().get(id)
		}

		@JvmStatic
		fun get(id: String): LazySupplier<StaticAnimation>? {
			return get(RcfUtil.modRl(id))
		}
	}

	private val staticAnimations = mutableMapOf<ResourceLocation, LazySupplier<StaticAnimation>>()

	fun get(id: ResourceLocation): LazySupplier<StaticAnimation>? = staticAnimations[id]

	override fun prepare(
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	): Map<ResourceLocation, LazySupplier<StaticAnimation>> {
		return FORGE_BUS.post(StaticAnimationRegistryEvent()).getAll()
	}

	override fun apply(
		map: Map<ResourceLocation, LazySupplier<StaticAnimation>>,
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	) {
		staticAnimations.clear()
		staticAnimations.putAll(map)
		staticAnimations.forEach {
			it.value.init()
		}
	}
}