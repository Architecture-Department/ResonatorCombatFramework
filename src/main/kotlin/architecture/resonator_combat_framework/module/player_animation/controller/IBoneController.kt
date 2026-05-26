package architecture.resonator_combat_framework.module.player_animation.controller

import architecture.goldenboughs_lib.api.AllOpe

/** 骨骼控制器 — 从原始动画数据写入 ProxyModel */
@AllOpe
interface IBoneController<in TRawData, in TProxyModel> {
	/** 遍历原始数据, 逐骨骼写入代理模型 */
	fun writeToProxy(data: TRawData, proxyModel: TProxyModel)
}
