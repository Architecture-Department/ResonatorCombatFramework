package architecture.resonator_combat_framework.init;

import architecture.resonator_combat_framework.core.Rcf;
import net.neoforged.neoforge.capabilities.ItemCapability;

public final class RcfCapabilitys {
	public static class Item {
		public static final ItemCapability<Void, Void> ITEM_ADDITIONAL_PHYSICS_BODY =
			ItemCapability.createVoid(Rcf.modRl("sparkcore_item_model"), Void.class);
	}
}
