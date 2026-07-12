package architecture.resonator_combat_framework.payload.toc

import architecture.goldenboughs_lib.api.payload.ToClientPayload

/**
 * 饰品同步数据包的抽象基类。
 *
 * 用于服务端向客户端同步饰品实体（如召唤物、抛射物等）的状态变更。
 * 不同的执行类型（[executeType]）对应不同的同步操作。
 *
 * @property entityId 目标饰品实体的网络 ID
 * @property executeType 同步操作类型标识
 */
abstract class AppurtenanceSynchroPayload(
	val entityId: Int,
	val executeType: Byte
) : ToClientPayload
