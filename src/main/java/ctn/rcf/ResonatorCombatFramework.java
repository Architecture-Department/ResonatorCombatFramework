package ctn.rcf;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ResonatorCombatFramework.MODID)
public class ResonatorCombatFramework {
    public static final String MODID = "resonator_combat_framework";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ResonatorCombatFramework(IEventBus modEventBus, ModContainer modContainer) {
    }
}
