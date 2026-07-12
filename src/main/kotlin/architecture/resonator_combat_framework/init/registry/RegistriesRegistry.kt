package architecture.resonator_combat_framework.init.registry

import architecture.resonator_combat_framework.init.RcfRegistries
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.registries.NewRegistryEvent

/**
 * 自定义 Registry 注册 —— 将 RCF 的自定义注册表注册到 NeoForge 的 NewRegistryEvent。
 */
@EventBusSubscriber(modid = RcfUtil.ID)
object RegistriesRegistry {
	@SubscribeEvent
	fun register(event: NewRegistryEvent) {
		RcfRegistries.register(event)
	}
}