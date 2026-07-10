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
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext

/**
 * 攻击网络包。
 *
 * 客户端→服务端的攻击请求，同时由服务端广播回所有追踪该实体的客户端以保证同步。
 * 服务端和客户端均会执行实际逻辑（通过 [WeaponProperty.onAttack]），
 * 但由于服务端是权威端，伤害计算以服务端结果为准。
 *
 * @property hand 发起攻击的手（MAIN_HAND=左键攻击 / OFF_HAND=右键攻击）
 * @property pressType 攻击类型：短按（SHORT）/ 长按（LONG）
 */
class AttackPayload(
	val hand: InteractionHand,
	val pressType: PressType,
) : ToServerAndClientPayload {

	/**
	 * 按压类型枚举。
	 *
	 * 区分玩家的短按单击与长按蓄力操作，用于触发不同的攻击行为。
	 */
	enum class PressType {
		/** 短按（单击） */
		SHORT,

		/** 长按（按住） */
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

	/**
	 * 执行攻击逻辑：获取当前手持物品的 [WeaponProperty] 并调用其 [WeaponProperty.onAttack] 方法。
	 */
	private fun execute(player: Player) {
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
