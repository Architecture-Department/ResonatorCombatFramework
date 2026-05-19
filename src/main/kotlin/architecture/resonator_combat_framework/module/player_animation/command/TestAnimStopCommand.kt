package architecture.resonator_combat_framework.module.player_animation.command

import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper
import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument

// /test_anim_stop <target> — 停止玩家动画
object TestAnimStopCommand {
	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			Commands.literal("test_anim_stop")
				.requires { it.hasPermission(2) }
				.then(
					Commands.argument("target", EntityArgument.player())
						.executes { ctx ->
							val target = EntityArgument.getPlayer(ctx, "target")
							PlayerAnimationHelper.stopPlayerAnimation(target)
							1
						}
				)
		)
	}
}
