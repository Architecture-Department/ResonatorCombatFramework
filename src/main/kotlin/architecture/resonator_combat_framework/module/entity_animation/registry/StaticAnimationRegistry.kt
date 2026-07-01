package architecture.resonator_combat_framework.module.entity_animation.registry

import architecture.goldenboughs_lib.api.AllOpe
import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.entity_animation.animation.AnimationDef
import architecture.resonator_combat_framework.module.entity_animation.event.StaticAnimationRegistryEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

@AllOpe
object StaticAnimationRegistry :
	SimplePreparableReloadListener<Map<ResourceLocation, LazySupplier<AnimationDef>>>() {
	private val animationsDef = mutableMapOf<ResourceLocation, LazySupplier<AnimationDef>>()

	fun get(id: ResourceLocation): LazySupplier<AnimationDef>? = animationsDef[id]

	override fun prepare(
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	): Map<ResourceLocation, LazySupplier<AnimationDef>> {
		return FORGE_BUS.post(StaticAnimationRegistryEvent()).getAll()
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