package architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation

import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MoLangParser
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue
import architecture.resonator_combat_framework.util.RcfUtil
import com.google.gson.JsonObject
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.StringUtil
import net.minecraft.world.entity.Entity

data class BakingBrAnimationTimeline
@JvmOverloads constructor(
    val time: Float,
    val molangs: List<MolangValue> = emptyList(),
    val commands: List<String> = emptyList(),
    val entityEvents: List<String> = emptyList()
) {
	fun run(entity: Entity, context: MolangData? = null) {
		molangs.forEach { it.eval(context) }

		entityEvents.forEach { event -> processEntityEvent(entity, event) }

		if (entity.level() !is ServerLevel) return
		val level = entity.level() as ServerLevel
		val server = level.server
		if (commands.isEmpty()) return

		@Suppress("TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") val source = CommandSourceStack(
            entity,
            entity.position(),
            entity.rotationVector,
            level,
            2,
            entity.name.string,
            entity.displayName,
            server,
            entity
        )
		commands.forEach { cmd ->
			if (StringUtil.isNullOrEmpty(cmd)) return@forEach
			try {
				server.commands.performPrefixedCommand(source, cmd)
			} catch (e: Exception) {
				RcfUtil.LOGGER.warn("[Timeline] Command failed: {} {} {} ", cmd, entity, e)
			}
		}
	}

	private fun processEntityEvent(entity: Entity, event: String) {
		val trimmed = event.trim()
		when {
			trimmed.startsWith("@s ") || trimmed == "@s" -> {
				val eventName = trimmed.removePrefix("@s").trim()
				if (eventName.isNotEmpty()) {
					RcfUtil.LOGGER.debug("[Timeline] Entity event: {} {}", eventName, entity)
				}
			}

			else -> {
				RcfUtil.LOGGER.warn("[Timeline] Unsupported entity event: {}", trimmed)
			}
		}
	}

	companion object {
		@JvmStatic
		fun parses(timelinesJson: JsonObject): List<BakingBrAnimationTimeline> {
			val list = mutableListOf<BakingBrAnimationTimeline>()
			timelinesJson.asMap().forEach { (key, value) ->
				val molangs = mutableListOf<MolangValue>()
				val commands = mutableListOf<String>()
				val entityEvents = mutableListOf<String>()
				val parts = if (value.isJsonArray) {
					value.asJsonArray.map { it.asString }
				} else {
					value.asString.split(";").filter { it.isNotBlank() }
				}
				parts.forEach { raw ->
					val trimmed = raw.trim()
					when {
						trimmed.startsWith("/") -> commands.add(trimmed)
						trimmed.startsWith("@") -> entityEvents.add(trimmed)
						else -> molangs.add(MoLangParser.compileMolang(trimmed))
					}
				}
				list.add(BakingBrAnimationTimeline(key.toFloat(), molangs, commands, entityEvents))
			}
			return list
		}
	}
}