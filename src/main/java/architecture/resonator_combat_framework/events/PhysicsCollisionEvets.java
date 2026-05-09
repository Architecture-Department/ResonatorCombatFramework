package architecture.resonator_combat_framework.events;

import architecture.goldenboughs_lib.core.GoldenBoughsLib;
import architecture.resonator_combat_framework.api.AppurtenanceHost;
import cn.solarmoon.spark_core.animation.IAnimatable;
import cn.solarmoon.spark_core.animation.anim.KeyAnimData;
import cn.solarmoon.spark_core.animation.model.BonePose;
import cn.solarmoon.spark_core.animation.model.ModelInstance;
import cn.solarmoon.spark_core.event.BoneUpdateEvent;
import cn.solarmoon.spark_core.event.PhysicsEntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = GoldenBoughsLib.ID)
public final class PhysicsCollisionEvets {
	@SubscribeEvent
	public static void onCollision(BoneUpdateEvent event) {
		BonePose bonePose = event.getBonePose();
		ModelInstance model = event.getModel();
		IAnimatable<?> animatable = model.getAnimatable();
		KeyAnimData newTransform = event.getNewTransform();
		KeyAnimData oldTransform = event.getOldTransform();
		KeyAnimData originNewTransform = event.getOriginNewTransform();
		if (animatable instanceof AppurtenanceHost appurtenanceHost) {
			appurtenanceHost.allOnBoneUpdate(event);
		}
	}

	@SubscribeEvent
	public static void physicsEntityTick(PhysicsEntityTickEvent event) {
		event.getEntity().allPhysTick();
	}
}
