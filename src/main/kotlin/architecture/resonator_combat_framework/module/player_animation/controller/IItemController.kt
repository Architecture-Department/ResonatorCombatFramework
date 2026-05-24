package architecture.resonator_combat_framework.module.player_animation.controller

/** 物品控制器 — 从原始数据提取 right_item/left_item 定位器写入手臂骨骼 */
interface IItemController<in TRawData, in TProxyBone> {
	fun writeToProxy(data: TRawData, leftArm: TProxyBone, rightArm: TProxyBone)
}
