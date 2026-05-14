package architecture.resonator_combat_framework.module.player_animation.command

import architecture.goldenboughs_lib.util.CommandUtil
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.selector.EntitySelector
import software.bernie.geckolib.GeckoLibServices

object GeoAnimatableCommand {
	fun registry(dispatcher: CommandDispatcher<CommandSourceStack>) {
		dispatcher.register(
			Commands.literal("geo_animatable")
				.requires { it.hasPermission(2) }
				.then(
					Commands.literal("play")
						.then(
							Commands.argument("controller", StringArgumentType.word())
								.then(
									Commands.argument("anim", StringArgumentType.word())
										.then(execute(false))
								).then(execute(false, isAnimName = false))
						).then(
							Commands.argument("anim", StringArgumentType.word())
								.then(execute(false, isController = false))
						).then(execute(false, false, false))
				).then(
					Commands.literal("stop")
						.then(
							Commands.argument("controller", StringArgumentType.word())
								.then(
									Commands.argument("anim", StringArgumentType.word())
										.then(execute(true))
								).then(execute(true, isAnimName = false))
						).then(
							Commands.argument("anim", StringArgumentType.word())
								.then(execute(true, isController = false))
						).then(execute(true, false, false))
				)
		)
	}

	private fun execute(
		isStop: Boolean,
		isController: Boolean = true,
		isAnimName: Boolean = true
	): RequiredArgumentBuilder<CommandSourceStack, EntitySelector> =
		Commands.argument("target", EntityArgument.player()).executes {
			val controllerName = if (isController) StringArgumentType.getString(it, "controller") else null
			val animName = if (isAnimName) StringArgumentType.getString(it, "anim") else null
			val entity = CommandUtil.getTargetPlayer(it)
			if (isStop) {
				GeckoLibServices.NETWORK.stopTriggeredEntityAnim(entity, false, controllerName, animName)
				1
			} else {
				GeckoLibServices.NETWORK.triggerEntityAnim(entity, false, controllerName, animName)
				1
			}
		}
}