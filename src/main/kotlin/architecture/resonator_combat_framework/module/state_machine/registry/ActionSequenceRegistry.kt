package architecture.resonator_combat_framework.module.state_machine.registry

import architecture.goldenboughs_lib.api.AllOpe
import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.combat.ActionSequence
import architecture.resonator_combat_framework.module.state_machine.event.ActionSequenceRegistryEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

/**
 * 动作序列注册表 —— 管理所有 [ActionSequence] 实例的注册与生命周期。
 * 继承 [SimplePreparableReloadListener]，在资源重载时通过 [ActionSequenceRegistryEvent] 收集序列定义。
 */
@AllOpe
object ActionSequenceRegistry : SimplePreparableReloadListener<Map<ResourceLocation, LazySupplier<ActionSequence>>>() {
	private val actionSequences = mutableMapOf<ResourceLocation, LazySupplier<ActionSequence>>()

	fun get(id: ResourceLocation): LazySupplier<ActionSequence>? = actionSequences[id]

	override fun prepare(
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	): Map<ResourceLocation, LazySupplier<ActionSequence>> {
		return FORGE_BUS.post(ActionSequenceRegistryEvent()).getAll()
	}

	override fun apply(
		map: Map<ResourceLocation, LazySupplier<ActionSequence>>,
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	) {
		actionSequences.clear()
		actionSequences.putAll(map)
		actionSequences.forEach {
			it.value.init()
		}
	}
}
