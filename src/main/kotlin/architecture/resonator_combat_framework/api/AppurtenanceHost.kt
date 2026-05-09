package architecture.resonator_combat_framework.api

import architecture.resonator_combat_framework.api.appurtenance.AnimatedAccessoryInfo
import architecture.resonator_combat_framework.api.appurtenance.AppurtenanceInfo
import cn.solarmoon.spark_core.event.BoneUpdateEvent

interface AppurtenanceHost {
	val appurtenanceInfoMap: MutableMap<String, AppurtenanceInfo<*>>

	fun allTick() {
		appurtenanceInfoMap.values.forEach {
			it.tick()
		}
	}

	fun allAnimTick() {
		appurtenanceInfoMap.values.forEach {
			if (it !is AnimatedAccessoryInfo<*, *>) return
			it.animController.tick()
		}
	}

	fun allStopAllAnimation() {
		appurtenanceInfoMap.values.forEach {
			if (it !is AnimatedAccessoryInfo<*, *>) return
			it.animController.stopAllAnimation()
		}
	}

	fun allOnBoneUpdate(event: BoneUpdateEvent) {
		appurtenanceInfoMap.values.forEach {
			if (it !is AnimatedAccessoryInfo<*, *>) return
			it.onBoneUpdate(event)
		}
	}

	fun allPhysTick() {
		appurtenanceInfoMap.values.forEach {
			if (it !is AnimatedAccessoryInfo<*, *>) return
			it.animController.physTick()
		}
	}
}