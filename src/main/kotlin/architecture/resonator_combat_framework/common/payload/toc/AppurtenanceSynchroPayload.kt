package architecture.resonator_combat_framework.common.payload.toc

import architecture.goldenboughs_lib.api.payload.ToClientPayload

abstract class AppurtenanceSynchroPayload(
	val entityId: Int,
	val executeType: Byte
) : ToClientPayload
