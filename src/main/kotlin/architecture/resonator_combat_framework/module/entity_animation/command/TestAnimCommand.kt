package architecture.resonator_combat_framework.module.entity_animation.command

import architecture.goldenboughs_lib.util.CommandContextUtil.getArguments
import architecture.resonator_combat_framework.events.registry.AnimationControllers
import architecture.resonator_combat_framework.module.entity_animation.animation.data.PlayConfig
import architecture.resonator_combat_framework.module.entity_animation.helper.PlayerAnimationHelper.pauseAnima
import architecture.resonator_combat_framework.module.entity_animation.helper.PlayerAnimationHelper.resumeAnima
import architecture.resonator_combat_framework.module.entity_animation.helper.PlayerAnimationHelper.stopAnima
import architecture.resonator_combat_framework.module.entity_animation.helper.PlayerAnimationHelper.triggerPlayerAnima
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

/**
 * ```
 * /test_anim <target> play <animId> [speed] [fadeIn]
 * /test_anim <target> stop [fadeOut]
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
						.then(
							Commands.literal("play")
								.then(
									Commands.argument("anim_id", ResourceLocationArgument.id())
										.suggests(IdArgumentProvider)
										.executes { handlePlay(it) }
										.then(
											Commands.argument("speed", FloatArgumentType.floatArg(Float.MIN_VALUE))
												.executes { handlePlay(it) }
												.then(
													Commands.argument("fade_in", IntegerArgumentType.integer(-1))
														.executes { handlePlay(it) }
														.then(
															Commands.argument(
																"fade_out",
																IntegerArgumentType.integer(-1)
															)
																.executes { handlePlay(it) }
														)
												)
										)
								)
						)
						.then(
							Commands.literal("stop")
								.executes { action(it) { stopAnima(AnimationControllers.COMMAND) } }
								.then(
									Commands.argument("fade_out", IntegerArgumentType.integer(-1))
										.executes {
											val target = getPlayer(it)
											target.stopAnima(AnimationControllers.COMMAND)
											it.source.sendSuccess({
												Component.literal("Stopped animation on ${target.name.string}")
											}, true)
											1
										}
								)
						)
						.then(
							Commands.literal("pause")
								.executes { action(it) { pauseAnima(AnimationControllers.COMMAND) } }
						)
						.then(
							Commands.literal("resume")
								.executes { action(it) { resumeAnima(AnimationControllers.COMMAND) } }
						)
				)
		)
	}

	private fun handlePlay(ctx: CommandContext<CommandSourceStack>): Int {
		val arguments = ctx.getArguments() ?: return 0

		val target = getPlayer(ctx)
		val animId = ResourceLocationArgument.getId(ctx, "anim_id")
		val speed = if (arguments.contains("speed")) FloatArgumentType.getFloat(ctx, "speed") else 1f
		val fadeIn = if (arguments.contains("fade_in")) IntegerArgumentType.getInteger(
			ctx,
			"fade_in"
		) else -1

		val fadeOut = if (arguments.contains("fade_out")) IntegerArgumentType.getInteger(
			ctx,
			"fade_out"
		) else -1

		val config = PlayConfig(
			speedMultiplier = speed,
			fadeInTicks = fadeIn,
			fadeOutTicks = fadeOut
		)


		target.triggerPlayerAnima(animId, config)
		ctx.source.sendSuccess({ Component.literal("Playing $animId on ${target.name.string}") }, true)
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
