package architecture.resonator_combat_framework.module.state_machine.registry

import architecture.goldenboughs_lib.api.AllOpe
import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.combat.Action
import architecture.resonator_combat_framework.module.state_machine.event.ActionRegistryEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

/**
 * 动作注册表 —— 管理所有 [Action] 实例的注册与生命周期。
 * 继承 [SimplePreparableReloadListener]，在资源重载时通过 [ActionRegistryEvent] 收集动作定义。
 */
@AllOpe
object ActionRegistry : SimplePreparableReloadListener<Map<ResourceLocation, LazySupplier<Action>>>() {
	private val actions = mutableMapOf<ResourceLocation, LazySupplier<Action>>()

	fun get(id: ResourceLocation): LazySupplier<Action>? = actions[id]

	override fun prepare(
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	): Map<ResourceLocation, LazySupplier<Action>> {
		return FORGE_BUS.post(ActionRegistryEvent()).getAll()
	}

	override fun apply(
		map: Map<ResourceLocation, LazySupplier<Action>>,
		resourceManager: ResourceManager,
		profiler: ProfilerFiller
	) {
		actions.clear()
		actions.putAll(map)
		actions.forEach {
			it.value.init()
		}
	}
}
