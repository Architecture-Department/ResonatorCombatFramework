package architecture.resonator_combat_framework.module.player_animation.command

import architecture.resonator_combat_framework.core.Rcf
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.triggerPlayerAnimation
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import architecture.resonator_combat_framework.core.RcfConstants

// /test_anim <target> <anim_id> — 触发玩家动画（Tab 补全动画 ID）
object TestAnimCommand {
	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			Commands.literal("test_anim")
				.requires { it.hasPermission(2) }
				.then(
					Commands.argument("target", EntityArgument.player())
						.then(
							Commands.argument("anim_id", StringArgumentType.word())
								.suggests(AnimationIdArgumentProvider)
								.executes {
									val target = try {
										EntityArgument.getPlayer(it, "target")
									} catch (exception: CommandSyntaxException) {
										RcfConstants.LOGGER.error("Failed to get player", exception)
										return@executes 0
									}
									val anim = StringArgumentType.getString(it, "anim_id")
									target.triggerPlayerAnimation(anim)
									it.getSource().sendSuccess({
										Component.translatable("${RcfConstants.ID}.command.play_anim", target.name, anim)
									}, true)
									1
								}
						)
				)
		)
	}
}
