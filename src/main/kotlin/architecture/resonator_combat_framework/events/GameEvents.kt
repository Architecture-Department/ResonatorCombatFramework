package architecture.resonator_combat_framework.events

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.GeckoLibCacheServer
import architecture.resonator_combat_framework.module.player_animation.config.RcfBoneConfigLoader
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.AddReloadListenerEvent

@EventBusSubscriber(modid = Rcf.ID)
object GameEvents {
	@SubscribeEvent
	fun onAddReloadListener(event: AddReloadListenerEvent) {
		event.addListener(RcfBoneConfigLoader.getInstance(false))
		event.addListener(PreparableReloadListener(GeckoLibCacheServer::reload))
	}
}