package architecture.resonator_combat_framework.module.player_animation.command

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.stopPlayerAnimation
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import architecture.resonator_combat_framework.core.RcfConstants

// /test_anim_stop <target> — 停止玩家动画
object TestAnimStopCommand {
	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			Commands.literal("test_anim_stop")
				.requires { it.hasPermission(2) }
				.then(
					Commands.argument("target", EntityArgument.player())
						.executes {
							val target = try {
								EntityArgument.getPlayer(it, "target")
							} catch (exception: CommandSyntaxException) {
								RcfConstants.LOGGER.error("Failed to get player", exception)
								return@executes 0
							}
							target.stopPlayerAnimation()
							it.getSource().sendSuccess({
								Component.translatable("${RcfConstants.ID}.command.stop_anim", target.name)
							}, true)
							1
						}
				)
		)
	}
}
