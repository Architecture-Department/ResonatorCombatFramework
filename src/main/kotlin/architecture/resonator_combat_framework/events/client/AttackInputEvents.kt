package architecture.resonator_combat_framework.events.client

import architecture.resonator_combat_framework.client.input.AttackInputTracker
import architecture.resonator_combat_framework.init.RcfCapabilitys
import architecture.resonator_combat_framework.payload.tosc.AttackPayload
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.network.PacketDistributor

/**
 * 客户端攻击输入事件监听器。RCF 全权接管战斗输入。
 *
 * - 左键（isAttack）= 主手攻击
 * - 右键（isUseItem）= 副手攻击（仅双持模式下）
 * - 非双持模式下右键行为不受影响
 */
@EventBusSubscriber(modid = RcfUtil.ID, value = [Dist.CLIENT])
object AttackInputEvents {
	private val mainHandTracker = AttackInputTracker()
	private val offHandTracker = AttackInputTracker()

	// ===== 事件拦截 =====

	/**
	 * 拦截交互按键事件。左键被 RCF 能力物品拦截用于攻击，右键在双持模式下拦截用于副手攻击。
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	fun onInteractionKeyMappingTriggered(event: InputEvent.InteractionKeyMappingTriggered) {
		val instance = Minecraft.getInstance()
		if (instance.screen != null) return
		val player = instance.player ?: return

		if (event.isAttack) {
			// 左键拦截：只有主手持有 RCF 能力才拦截
			if (!hasRcfCapability(player.mainHandItem)) return
			event.setSwingHand(false)
			event.setCanceled(true)
			return
		}

		if (event.isUseItem && isDualWielding(player)) {
			// 右键拦截：仅双持模式下，副手持有能力才拦截
			if (!hasRcfCapability(player.offhandItem)) return
			event.setSwingHand(false)
			event.setCanceled(true)
		}
	}

	// ===== 按键追踪 =====

	/**
	 * 客户端 tick 事件：追踪攻击按键状态，在按下/释放时发送 [AttackPayload]。
	 */
	@SubscribeEvent
	fun onClientTickPre(event: ClientTickEvent.Pre) {
		val minecraft = Minecraft.getInstance()
		if (minecraft.screen != null) return
		val player = minecraft.player ?: return

		val options = minecraft.options
		val isDual = isDualWielding(player)

		// 主手：左键
		mainHandTracker.tick(options.keyAttack.isDown) { pressType ->
			PacketDistributor.sendToServer(AttackPayload(InteractionHand.MAIN_HAND, pressType))
		}

		// 副手：右键（仅双持时追踪）
		if (isDual) {
			offHandTracker.tick(options.keyUse.isDown) { pressType ->
				PacketDistributor.sendToServer(AttackPayload(InteractionHand.OFF_HAND, pressType))
			}
		}
	}

	// ===== 判断方法 =====

	/**
	 * 检查物品栈是否持有 RCF 战斗能力。
	 *
	 * @param itemStack 要检查的物品栈
	 * @return 是否持有 RCF 能力
	 */
	private fun hasRcfCapability(itemStack: ItemStack): Boolean =
		itemStack.getCapability(RcfCapabilitys.ITEM_ABILITY) != null

	/**
	 * 判断玩家是否处于双持战斗模式。
	 * 双持时右键将用于副手攻击而非使用物品。
	 * TODO: 按实际条件实现（如双持均持有 WeaponCapability、或特定游戏模式/状态）
	 */
	private fun isDualWielding(player: Player): Boolean {
		return false
	}
}
