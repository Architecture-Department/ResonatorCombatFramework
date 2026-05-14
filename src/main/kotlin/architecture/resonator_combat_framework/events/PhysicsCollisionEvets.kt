package architecture.resonator_combat_framework.events

import architecture.goldenboughs_lib.core.Lib
import architecture.resonator_combat_framework.api.AppurtenanceHost
import cn.solarmoon.spark_core.event.BoneUpdateEvent
import cn.solarmoon.spark_core.event.PhysicsEntityTickEvent
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber

@EventBusSubscriber(modid = Lib.ID)
object PhysicsCollisionEvets {
	@SubscribeEvent
	fun onCollision(event: BoneUpdateEvent) {
		val bonePose = event.bonePose
		val model = event.model
		val animatable = model.animatable
		val newTransform = event.newTransform
		val oldTransform = event.oldTransform
		val originNewTransform = event.originNewTransform
		if (animatable is AppurtenanceHost) {
			animatable.allOnBoneUpdate(event)
		}
	}

	@SubscribeEvent
	fun physicsEntityTick(event: PhysicsEntityTickEvent) {
		event.entity.allPhysTick()
	}
}
