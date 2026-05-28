package architecture.resonator_combat_framework.module.player_animation.command

import architecture.goldenboughs_lib.util.CommandContextUtil.getArguments
import architecture.resonator_combat_framework.module.player_animation.config.AnimationPlayConfig
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.pausePlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.resumePlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.stopPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.stopPlayerAnimationImmediate
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.triggerPlayerAnimation
import architecture.resonator_combat_framework.module.player_animation.helper.PlayerAnimationHelper.triggerPlayerAnimationImmediate
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * ```
 * /test_anim <target> play <animId> [speed] [fadeIn]
 * /test_anim <target> play_immediate <animId> [speed]
 * /test_anim <target> stop [fadeOut]
 * /test_anim <target> stop_immediate
 * /test_anim <target> pause
 * /test_anim <target> resume
 * ```
 */
object TestAnimCommand {
	fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			Commands.literal("test_anim")
				.requires { it.hasPermission(2) }
				.then(
					Commands.argument("target", EntityArgument.player())
						.then(playCommand())
						.then(playImmediateCommand())
						.then(
							Commands.literal("stop")
								.executes { action(it) { stopPlayerAnimation() } }
								.then(
									Commands.argument("fade_out", IntegerArgumentType.integer(0))
										.executes {
											val target = getPlayer(it)
											target.stopPlayerAnimation()
											it.source.sendSuccess({
												Component.literal("Stopped animation on ${target.name.string}")
											}, true)
											1
										}
								)
						)
						.then(
							Commands.literal("stop_immediate")
								.executes { action(it) { stopPlayerAnimationImmediate() } }
						)
						.then(
							Commands.literal("pause")
								.executes { action(it) { pausePlayerAnimation() } }
						)
						.then(
							Commands.literal("resume")
								.executes { action(it) { resumePlayerAnimation() } }
						)
				)
		)
	}

	private fun playCommand() = Commands.literal("play").then(
		Commands.argument("anim_id", StringArgumentType.word())
			.suggests(AnimationIdArgumentProvider)
			.executes { handlePlay(it) }
			.then(
				Commands.argument("speed", FloatArgumentType.floatArg(0.01f))
					.executes { handlePlay(it) }
					.then(
						Commands.argument("fade_in", IntegerArgumentType.integer(0))
							.executes { handlePlay(it) }
							.then(
								Commands.argument("fade_out", IntegerArgumentType.integer(0))
									.executes { handlePlay(it) }
							)
					)
			)
	)

	private fun playImmediateCommand() = Commands.literal("play_immediate").then(
		Commands.argument("anim_id", StringArgumentType.word())
			.suggests(AnimationIdArgumentProvider)
			.executes { handlePlayImmediate(it) }
			.then(
				Commands.argument("speed", FloatArgumentType.floatArg(0.01f))
					.executes { handlePlayImmediate(it) }
			)
	)

	private fun handlePlay(ctx: CommandContext<CommandSourceStack>): Int {
		val arguments = ctx.getArguments() ?: return 0

		val target = getPlayer(ctx)
		val animId = StringArgumentType.getString(ctx, "anim_id")
		val speed = if (arguments.contains("speed")) FloatArgumentType.getFloat(ctx, "speed") else 1f
		val fadeIn = if (arguments.contains("fade_in")) IntegerArgumentType.getInteger(
			ctx,
			"fade_in"
		) else -1

		val fadeOut = if (arguments.contains("fade_out")) IntegerArgumentType.getInteger(
			ctx,
			"fade_out"
		) else -1

		val config = AnimationPlayConfig.builder(animId).speed(speed).also {
			if (fadeIn >= 0) it.fadeIn(fadeIn)
			if (fadeOut >= 0) it.fadeOut(fadeOut)
		}.build()

		target.triggerPlayerAnimation(config)
		ctx.source.sendSuccess({ Component.literal("Playing $animId on ${target.name.string}") }, true)
		return 1
	}

	private fun handlePlayImmediate(ctx: CommandContext<CommandSourceStack>): Int {
		val accessor = ctx.getArguments() ?: return 0

		val target = getPlayer(ctx)
		val animId = StringArgumentType.getString(ctx, "anim_id")
		val speed = if (accessor.contains("speed")) FloatArgumentType.getFloat(ctx, "speed") else 1f
		target.triggerPlayerAnimationImmediate(animId, speed)
		ctx.source.sendSuccess({ Component.literal("Playing $animId immediate on ${target.name.string}") }, true)
		return 1
	}

	private fun getPlayer(ctx: CommandContext<CommandSourceStack>): Player {
		return try {
			EntityArgument.getPlayer(ctx, "target")
		} catch (e: Exception) {
			throw RuntimeException(e)
		}
	}

	private fun action(ctx: CommandContext<CommandSourceStack>, fn: Player.() -> Unit): Int {
		fn(getPlayer(ctx))
		return 1
	}
}