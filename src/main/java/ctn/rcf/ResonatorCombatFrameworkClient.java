package ctn.rcf;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@Mod(value = ResonatorCombatFramework.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ResonatorCombatFramework.MODID, value = Dist.CLIENT)
public class ResonatorCombatFrameworkClient {
    public ResonatorCombatFrameworkClient(ModContainer container) {
    }
}
