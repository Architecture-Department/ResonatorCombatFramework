package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.util.RcfUtil.modRl
import net.neoforged.neoforge.capabilities.ItemCapability

class RcfCapabilitys {
	object Item {
		@JvmField
		val ITEM_ADDITIONAL_PHYSICS_BODY: ItemCapability<Void?, Void?> =
			ItemCapability.createVoid(modRl("sparkcore_item_model"), Void::class.java)
	}
}
