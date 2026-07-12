package architecture.resonator_combat_framework.client.input

import architecture.resonator_combat_framework.payload.tosc.AttackPayload

/**
 * 每 client tick 检测攻击键状态，追踪按下时长并区分短按/长按。
 *
 * 时间阈值：
 * - 短按（SHORT）：按下后在 200ms 内释放
 * - 长按（LONG）：持续按住超过 300ms（仅触发一次）
 * - 兜底（fallback）：超过 500ms 仍未释放时自动补发 LONG
 */
class AttackInputTracker {

	/** 当前攻击键是否按住 */
	var isDown: Boolean = false
		private set

	/** 本次按下的起始时间戳（[System.nanoTime]），仅在 [isDown] 为 true 时有意义 */
	var pressStartNanos: Long = 0L
		private set

	/** 是否为当前此次按下已发出过长按包 */
	var longPressSent: Boolean = false
		private set

	/**
	 * 每 tick 调用一次。
	 *
	 * @param attackKeyDown 当前 tick 的攻击键按下状态，通常来自
	 *   [net.minecraft.client.Minecraft.getInstance().options.keyAttack.isDown]
	 * @param sendPacket 回调函数，应当将 [PressType] 包装成网络包发往服务端
	 */
	fun tick(attackKeyDown: Boolean, sendPacket: (AttackPayload.PressType) -> Unit) {
		val now = System.nanoTime()

		if (attackKeyDown) {
			if (!isDown) {
				// -- 刚按下 --
				isDown = true
				pressStartNanos = now
				longPressSent = false
				return
			}

			// -- 持续按住 --
			val elapsedMs = (now - pressStartNanos) / 1_000_000L

			// 超过 300ms → 发 LONG（仅一次）
			if (elapsedMs >= 300L && !longPressSent) {
				sendPacket(AttackPayload.PressType.LONG)
				longPressSent = true
			}

			// 超过 500ms 兜底：防止因某些原因 LONG 未发出的极端情况
			@Suppress("KotlinConstantConditions")
			if (elapsedMs >= 500L && !longPressSent) {
				sendPacket(AttackPayload.PressType.LONG)
				longPressSent = true
			}
			return
		}

		if (isDown) {
			// -- 释放 --
			if (!longPressSent) {
				// 没有发过 LONG → 认为是短按
				sendPacket(AttackPayload.PressType.SHORT)
			}
			// 如果已经发过 LONG，释放时不再重复发 SHORT

			isDown = false
		}
	}

	/** 重置所有状态到初始值 */
	fun reset() {
		isDown = false
		pressStartNanos = 0L
		longPressSent = false
	}
}
