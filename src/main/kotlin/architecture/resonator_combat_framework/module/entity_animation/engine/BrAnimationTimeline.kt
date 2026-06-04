package architecture.resonator_combat_framework.module.entity_animation.engine

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathParser
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
	val entityEvents: List<String> = emptyList() // TODO: 未完成
) {
	fun apply(entity: Entity) {
		val level = entity.level()
		molangs.forEach {
			it.get()
		}
		if (level !is ServerLevel) return
		val server = level.server
		if (!commands.isEmpty()) {
			@Suppress("TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
			val commandSourceStack = CommandSourceStack(
				entity,
				entity.position(),
				entity.rotationVector,
				level,
				2,
				entity.getName().string,
				entity.displayName,
				level.server,
				entity
			)
			commands.forEach {
				if (StringUtil.isNullOrEmpty(it)) {
					return@forEach
				}
				try {
					server.commands.performPrefixedCommand(commandSourceStack, it)
				} catch (e: Exception) {
					// TODO: 补充完整报错信息
				}
			}
		}
		// TODO: 未完成
//		entityEvents.forEach {
//
//		}
	}

	companion object {
		fun parses(timelinesJson: JsonObject): List<BrAnimationTimeline> {
			val list = mutableListOf<BrAnimationTimeline>()
			timelinesJson.asMap().forEach { (key, value) ->
				val molangs = mutableListOf<MolangValue>()
				val commands = mutableListOf<String>()
				val entityEvents = mutableListOf<String>()
				value.asString.split(";").forEach {
					when {
						it.trim().startsWith("/") -> commands.add(it)
						it.trim().startsWith("@") -> entityEvents.add(it) // TODO: 未完成
						else -> molangs.add(MathParser.compileMolang(it))
					}
				}
				list.add(BrAnimationTimeline(key.toFloat(), molangs, commands, entityEvents))
			}
			return list
		}
	}
}