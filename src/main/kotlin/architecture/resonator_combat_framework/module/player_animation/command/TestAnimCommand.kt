package architecture.resonator_combat_framework.module.player_animation.command

import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.pausePlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.resumePlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.stopPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.stopPlayerAnimationImmediate
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.triggerPlayerAnimation
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * /test_anim <target> play <animId> [speed] [fadeIn]
 * /test_anim <target> stop
 * /test_anim <target> stop_immediate
 * /test_anim <target> pause
 * /test_anim <target> resume
 */
object TestAnimCommand {
	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			Commands.literal("test_anim").requires { it.hasPermission(2) }
				.then(
					Commands.argument("target", EntityArgument.player()).then(
						Commands.literal("play").then(
							Commands.argument("anim_id", StringArgumentType.word()).suggests(AnimationIdArgumentProvider)
								.executes { play(it, "anim_id", 1f, -1) }.then(
									Commands.argument("speed", FloatArgumentType.floatArg(0.01f))
										.executes { play(it, "anim_id", FloatArgumentType.getFloat(it, "speed"), -1) }.then(
											Commands.argument("fade_in", IntegerArgumentType.integer(0)).executes {
												val spd = FloatArgumentType.getFloat(it, "speed")
												val fade = IntegerArgumentType.getInteger(it, "fade_in")
												play(it, "anim_id", spd, fade)
											})
								)
						)
					).then(
						Commands.literal("stop")
							.executes { action(it) { stopPlayerAnimation() } })
						.then(
							Commands.literal("stop_immediate")
								.executes { action(it) { stopPlayerAnimationImmediate() } })
						.then(
							Commands.literal("pause")
								.executes { action(it) { pausePlayerAnimation() } })
						.then(
							Commands.literal("resume")
								.executes { action(it) { resumePlayerAnimation() } })
				)
		)
	}

	private fun getPlayer(ctx: CommandContext<CommandSourceStack>) = try {
		EntityArgument.getPlayer(ctx, "target")
	} catch (e: CommandSyntaxException) {
		throw RuntimeException(e)
	}

	private fun action(
		ctx: CommandContext<CommandSourceStack>, fn: Player.() -> Unit
	): Int {
		fn(getPlayer(ctx))
		return 1
	}

	private fun play(
		ctx: CommandContext<CommandSourceStack>, argName: String, speed: Float, fadeIn: Int
	): Int {
		val target = getPlayer(ctx)
		val anim = StringArgumentType.getString(ctx, argName)
		val config = AnimationPlayConfig.builder(anim).speed(speed)
		if (fadeIn >= 0) config.fadeIn(fadeIn)
		target.triggerPlayerAnimation(config.build())
		ctx.source.sendSuccess({
			Component.literal("Playing " + anim + " on " + target.name.string)
		}, true)
		return 1
	}
}
