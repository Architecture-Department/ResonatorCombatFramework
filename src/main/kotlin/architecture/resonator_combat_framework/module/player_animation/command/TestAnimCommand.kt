package architecture.resonator_combat_framework.module.player_animation.command

import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument

// /test_anim <target> <anim_id> — 触发玩家动画（Tab 补全动画 ID）
object TestAnimCommand {
	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			Commands.literal("test_anim")
				.requires { it.hasPermission(2) }
				.then(
					Commands.argument("target", EntityArgument.player())
					.then(
						Commands.argument("anim", StringArgumentType.word())
							.suggests(AnimationIdArgumentProvider)
							.executes { ctx ->
								val target = EntityArgument.getPlayer(ctx, "target")
								val anim = StringArgumentType.getString(ctx, "anim")
								PlayerAnimationHelper.requestPlayerAnimation(target, anim)
								1
							}
					)
				)
		)
	}
}
