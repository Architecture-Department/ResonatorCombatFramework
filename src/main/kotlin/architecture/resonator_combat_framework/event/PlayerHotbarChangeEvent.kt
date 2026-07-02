package architecture.resonator_combat_framework.event

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent

/**
 * 玩家热键栏槽位变化事件 —— 在玩家切换热键栏槽位（滚轮、数字键）前触发。
 *
 * 此事件 [可取消][ICancellableEvent]，取消后槽位不会改变，物品不切换。
 * 可通过修改 [toSlot] 改变目标槽位。
 *
 * 在 [net.minecraft.world.entity.player.Inventory.setSelectedSlot] 的 Mixin 中触发，
 * 覆盖滚轮切换和数字键 1-9 切换。
 *
 * @param player   触发切换的玩家
 * @param fromSlot 当前槽位索引
 * @param toSlot   目标槽位索引（可修改以实现重定向）
 * @param fromStack 当前槽位的物品
 * @param toStack   目标槽位的物品
 */
class PlayerHotbarChangeEvent(
	val player: Player,
	val fromSlot: Int,
	var toSlot: Int,
	val fromStack: ItemStack,
	val toStack: ItemStack,
) : Event(), ICancellableEvent
