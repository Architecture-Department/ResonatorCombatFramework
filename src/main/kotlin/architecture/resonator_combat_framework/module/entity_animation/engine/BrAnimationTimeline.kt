package architecture.resonator_combat_framework.module.entity_animation.engine

import architecture.resonator_combat_framework.core.RcfConstants
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathParser
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import com.google.gson.JsonObject
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.StringUtil
import net.minecraft.world.entity.Entity

data class BrAnimationTimeline(
	val time: Float,
	val molangs: List<MolangValue> = emptyList(),
	val commands: List<String> = emptyList(),
	val entityEvents: List<String> = emptyList()
) {
	fun apply(entity: Entity, context: MolangData? = null) {
		molangs.forEach { it.get(context) }

		entityEvents.forEach { event -> processEntityEvent(entity, event) }

		if (entity.level() !is ServerLevel) return
		val level = entity.level() as ServerLevel
		val server = level.server
		if (commands.isEmpty()) return

		@Suppress("TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
		val source = CommandSourceStack(
			entity, entity.position(), entity.rotationVector,
			level, 2, entity.name.string, entity.displayName, server, entity
		)
		commands.forEach { cmd ->
			if (StringUtil.isNullOrEmpty(cmd)) return@forEach
			try {
				server.commands.performPrefixedCommand(source, cmd)
			} catch (e: Exception) {
				RcfConstants.LOGGER.warn("[Timeline] Command failed: {} {} {} ", cmd, entity, e)
			}
		}
	}

	private fun processEntityEvent(entity: Entity, event: String) {
		val trimmed = event.trim()
		when {
			trimmed.startsWith("@s ") || trimmed == "@s" -> {
				val eventName = trimmed.removePrefix("@s").trim()
				if (eventName.isNotEmpty()) {
					RcfConstants.LOGGER.debug("[Timeline] Entity event: {} {}", eventName, entity)
				}
			}

			else -> {
				RcfConstants.LOGGER.warn("[Timeline] Unsupported entity event: {}", trimmed)
			}
		}
	}

	companion object {
		fun parses(timelinesJson: JsonObject): List<BrAnimationTimeline> {
			val list = mutableListOf<BrAnimationTimeline>()
			timelinesJson.asMap().forEach { (key, value) ->
				val molangs = mutableListOf<MolangValue>()
				val commands = mutableListOf<String>()
				val entityEvents = mutableListOf<String>()
				value.asString.split(";").forEach {
					val trimmed = it.trim()
					when {
						trimmed.startsWith("/") -> commands.add(it)
						trimmed.startsWith("@") -> entityEvents.add(it)
						else -> molangs.add(MathParser.compileMolang(it))
					}
				}
				list.add(BrAnimationTimeline(key.toFloat(), molangs, commands, entityEvents))
			}
			return list
		}
	}
}