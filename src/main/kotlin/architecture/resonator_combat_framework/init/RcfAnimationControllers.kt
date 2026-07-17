package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation

/**
 * 动画控制器 ID 定义 —— 集中管理所有动画控制器的注册键。
 */
object RcfAnimationControllers {
	@JvmField
	val BACKGROUND_ACTION: ResourceLocation = RcfUtil.modRl("background_action")

	@JvmField
	val ACTION: ResourceLocation = RcfUtil.modRl("action")

	@JvmField
	val ATTACK: ResourceLocation = RcfUtil.modRl("attack")

	@JvmField
	val MAIN: ResourceLocation = RcfUtil.modRl("main")

	@JvmField
	val COMMAND: ResourceLocation = RcfUtil.modRl("command")
}