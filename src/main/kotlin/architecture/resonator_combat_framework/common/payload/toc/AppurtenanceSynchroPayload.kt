package architecture.resonator_combat_framework.common.payload.toc

import architecture.goldenboughs_lib.common.payload.api.ToClientPayload

abstract class AppurtenanceSynchroPayload(
	val entityId: Int,
	val executeType: Byte
) : ToClientPayload
