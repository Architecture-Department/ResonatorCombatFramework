package architecture.resonator_combat_framework.common.payload

import architecture.goldenboughs_lib.api.payload.ToServerAndClientPayload
import architecture.goldenboughs_lib.util.EnumStreamCodec
import architecture.goldenboughs_lib.util.LibUtil.INTERACTION_HAND_STREAM_CODEC
import architecture.resonator_combat_framework.common.item_property.WeaponProperty
import architecture.resonator_combat_framework.init.RcfCapabilitys
import architecture.resonator_combat_framework.util.RcfUtil
import io.netty.buffer.ByteBuf
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext

/**
 * 客户端→服务端：玩家攻击请求。
 *
 * @param hand 发起攻击的手（MAIN_HAND=左键 / OFF_HAND=右键）
 * @param pressType 短按/长按
 */
class AttackPayload(
	val hand: InteractionHand,
	val pressType: PressType,
) : ToServerAndClientPayload {
	/**
	 * 攻击类型：短按 / 长按
	 */
	enum class PressType {
		SHORT,
		LONG;

		companion object {
			@JvmField
			val STREAM_CODEC: StreamCodec<ByteBuf, PressType> = EnumStreamCodec.create(PressType::class.java)
		}
	}

	override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

	override fun toServer(context: IPayloadContext, player: ServerPlayer) {
		execute(player)
		// 广播给所有追踪此实体的客户端（含自己），保证客户端同步执行
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, this)
	}

	override fun toClient(context: IPayloadContext, player: AbstractClientPlayer) {
		execute(player)
	}

	private fun execute(player: net.minecraft.world.entity.player.Player) {
		val itemStack = player.getItemInHand(hand)
		val ability = itemStack.getCapability(RcfCapabilitys.ITEM_ABILITY)
		(ability as? WeaponProperty)?.onAttack(itemStack, player, hand, pressType)
	}

	companion object {
		@JvmField
		val TYPE = CustomPacketPayload.Type<AttackPayload>(RcfUtil.modRl("attack"))

		@JvmField
		val STREAM_CODEC: StreamCodec<ByteBuf, AttackPayload> = StreamCodec.composite(
			INTERACTION_HAND_STREAM_CODEC, AttackPayload::hand,
			PressType.STREAM_CODEC, AttackPayload::pressType,
			::AttackPayload
		)
	}
}
