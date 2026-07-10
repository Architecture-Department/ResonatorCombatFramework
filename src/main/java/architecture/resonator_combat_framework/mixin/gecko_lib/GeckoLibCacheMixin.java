/**
 * GeckoLib 缓存 Mixin —— 保留用于未来扩展 GeckoLib 缓存行为。
 * 当前为空实现，可在此添加缓存加载的拦截逻辑。
 */
package architecture.resonator_combat_framework.mixin.gecko_lib;

import org.spongepowered.asm.mixin.Mixin;
import software.bernie.geckolib.cache.GeckoLibCache;

@Mixin(GeckoLibCache.class)
public abstract class GeckoLibCacheMixin {
}
