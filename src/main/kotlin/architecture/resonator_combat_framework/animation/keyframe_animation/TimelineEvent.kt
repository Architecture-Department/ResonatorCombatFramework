package architecture.resonator_combat_framework.animation.keyframe_animation

import architecture.resonator_combat_framework.animation.molang.MoLangParser
import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue
import architecture.resonator_combat_framework.util.RcfUtil
import com.google.gson.JsonObject
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.StringUtil
import net.minecraft.world.entity.Entity

/**
 * 时间线事件数据类，在动画时间线的指定时刻执行 MoLang 脚本、实体事件或服务端命令。
 * 支持三种类型的事件：
 * - MoLang 脚本：直接求值的表达式
 * - 实体事件：以 @ 开头的实体内部事件
 * - 服务端命令：以 / 开头的 Minecraft 命令
 *
 * @property time 触发时间（秒）
 * @property molangs MoLang 脚本列表
 * @property commands 服务端命令列表
 * @property entityEvents 实体事件列表
 */
data class TimelineEvent
@JvmOverloads constructor(
	val time: Float,
	val molangs: List<MolangValue> = emptyList(),
	val commands: List<String> = emptyList(),
	val entityEvents: List<String> = emptyList()
) {
	/**
	 * 执行时间线事件。
	 * 依次执行 MoLang 脚本、实体事件和服务端命令。
	 *
	 * @param entity 所属实体
	 * @param context MoLang 运行上下文
	 */
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

	/**
	 * 处理实体事件。
	 * 当前支持 @s 前缀的事件（发送给实体自身）。
	 *
	 * @param entity 目标实体
	 * @param event 事件字符串
	 */
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
		/**
		 * 解析 JSON 时间线定义。
		 * 格式：{ "time": "script1; script2" } 或 { "time": ["item1", "item2"] }
		 * 以 / 开头的是命令，以 @ 开头的是实体事件，其余为 MoLang 脚本。
		 *
		 * @param timelinesJson JSON 对象
		 * @return 时间线事件列表
		 */
		@JvmStatic
		fun parses(timelinesJson: JsonObject): List<TimelineEvent> {
			val list = mutableListOf<TimelineEvent>()
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
				list.add(TimelineEvent(key.toFloat(), molangs, commands, entityEvents))
			}
			return list
		}
	}
}
