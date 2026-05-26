package architecture.resonator_combat_framework.module.player_animation.command

import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.pausePlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.resumePlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.stopPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.stopPlayerAnimationImmediate
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component

// /test_anim_stop <target>           — fade-out stop
// /test_anim_stop_immediate <target> — instant stop
// /test_anim_pause <target>          — pause
// /test_anim_resume <target>         — resume
object TestAnimStopCommand {
	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		val targetArg = Commands.argument("target", EntityArgument.player())

		// /test_anim_stop <target>
		dispatcher.register(
			Commands.literal("test_anim_stop")
				.requires { it.hasPermission(2) }
				.then(targetArg.executes { ctx ->
					val target = getPlayer(ctx)
					target.stopPlayerAnimation()
					ctx.source.sendSuccess({
						Component.literal("Stopped animation for " + target.name.string)
					}, true)
					1
				})
		)

		// /test_anim_stop_immediate <target>
		dispatcher.register(
			Commands.literal("test_anim_stop_immediate")
				.requires { it.hasPermission(2) }
				.then(targetArg.executes { ctx ->
					val target = getPlayer(ctx)
					target.stopPlayerAnimationImmediate()
					ctx.source.sendSuccess({
						Component.literal("Immediately stopped animation for " + target.name.string)
					}, true)
					1
				})
		)

		// /test_anim_pause <target>
		dispatcher.register(
			Commands.literal("test_anim_pause")
				.requires { it.hasPermission(2) }
				.then(targetArg.executes { ctx ->
					val target = getPlayer(ctx)
					target.pausePlayerAnimation()
					ctx.source.sendSuccess({
						Component.literal("Paused animation for " + target.name.string)
					}, true)
					1
				})
		)

		// /test_anim_resume <target>
		dispatcher.register(
			Commands.literal("test_anim_resume")
				.requires { it.hasPermission(2) }
				.then(targetArg.executes { ctx ->
					val target = getPlayer(ctx)
					target.resumePlayerAnimation()
					ctx.source.sendSuccess({
						Component.literal("Resumed animation for " + target.name.string)
					}, true)
					1
				})
		)
	}

	private fun getPlayer(ctx: com.mojang.brigadier.context.CommandContext<CommandSourceStack>): net.minecraft.world.entity.player.Player {
		return try {
			EntityArgument.getPlayer(ctx, "target")
		} catch (e: CommandSyntaxException) {
			throw RuntimeException(e)
		}
	}
}
