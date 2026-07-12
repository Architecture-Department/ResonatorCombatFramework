package architecture.resonator_combat_framework.init.registry

import architecture.resonator_combat_framework.registry.BoneConfigRegistry
import architecture.resonator_combat_framework.registry.GeometryModelRegistry
import architecture.resonator_combat_framework.registry.KeyframeAnimationRegistry
import architecture.resonator_combat_framework.util.GeckoLibCacheServer
import architecture.resonator_combat_framework.util.RcfUtil
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.AddReloadListenerEvent

/**
 * 服务端资源重载注册 —— 在数据包/资源重载时注册需要重新加载的监听器。
 * 包括骨骼配置、关键帧动画、几何模型、动作和动作序列等。
 */
@EventBusSubscriber(modid = RcfUtil.ID)
object ResourceReloadRegistry {
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	fun registry(event: AddReloadListenerEvent) {
		event.addListener(GeckoLibCacheServer::reload)
		event.addListener(BoneConfigRegistry.getInstance(false))
		event.addListener(KeyframeAnimationRegistry.getInstance(false))
		event.addListener(GeometryModelRegistry.getInstance(false))
	}
}



