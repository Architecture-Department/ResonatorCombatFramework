package architecture.resonator_combat_framework.animation.keyframe_animation

import architecture.goldenboughs_lib.util.LibUtil
import architecture.resonator_combat_framework.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.animation.molang.MoLangParser
import architecture.resonator_combat_framework.animation.molang.MolangData
import architecture.resonator_combat_framework.animation.molang.MolangValue
import architecture.resonator_combat_framework.animation.molang.withScope
import architecture.resonator_combat_framework.model.DynamicGeometryModel
import architecture.resonator_combat_framework.model.PoseData
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/**
 * 声音事件数据类，在动画时间线的指定时刻播放声音效果。
 * 每个事件可包含一个或多个声音效果，支持通过 MoLang 脚本动态控制音量和音调。
 *
 * @property time 触发时间（秒）
 * @property effects 声音效果列表
 */
data class SoundEvent
@JvmOverloads constructor(
	val time: Float, val effects: List<Effect> = emptyList()
) {
	/**
	 * 执行所有声音效果。
	 *
	 * @param controller 动画控制器
	 * @param entity 所属实体
	 * @param brModel 几何模型
	 * @param animationData 当前动画姿态数据
	 * @param context MoLang 运行上下文
	 * @param partialTick 渲染帧插值系数
	 */
	fun runs(
		controller: IEntityAnimationController<*>,
		entity: Entity,
		brModel: DynamicGeometryModel,
		animationData: PoseData,
		context: MolangData? = null,
		partialTick: Float = 1f
	) {
		effects.forEach { it.run(controller, entity, brModel, animationData, context, partialTick) }
	}

	/**
	 * 单个声音效果定义。
	 * 支持通过 MoLang 脚本设置 temp.volume 和 temp.pitch 变量来自定义音量和音调。
	 *
	 * @property soundId 声音事件 ID
	 * @property locatorName 定位器名称（当前未使用，保留扩展性）
	 * @property bindToActor 是否绑定到实体
	 * @property preEffectScript 播放前执行的 MoLang 脚本
	 */
	data class Effect
	@JvmOverloads constructor(
		val soundId: ResourceLocation,
		val locatorName: String? = null,
		val bindToActor: Boolean = true,
		val preEffectScript: MolangValue? = null
	) {
		/**
		 * 执行单个声音效果。
		 * 先在 MoLang 作用域中执行前置脚本以获取 volume/pitch，
		 * 然后在实体的所在维度播放声音。
		 *
		 * @param controller 动画控制器
		 * @param entity 所属实体
		 * @param brModel 几何模型
		 * @param animationData 当前动画姿态数据
		 * @param context MoLang 运行上下文
		 * @param partialTick 渲染帧插值系数
		 */
		fun run(
			controller: IEntityAnimationController<*>,
			entity: Entity,
			brModel: DynamicGeometryModel,
			animationData: PoseData,
			context: MolangData? = null,
			partialTick: Float = 1f
		) {
			var volume = 1.0f
			var pitch = 1.0f
			if (preEffectScript != null) {
				context?.withScope { scope ->
					preEffectScript.eval(scope)
					volume = scope.getLocal("temp.volume")?.toFloat() ?: 1.0f
					pitch = scope.getLocal("temp.pitch")?.toFloat() ?: 1.0f
				}
			}
			val soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundId) ?: return
			val entityPos = entity.position()

			val category = if (entity is Player) SoundSource.PLAYERS else SoundSource.WEATHER
			val level = entity.level()
			if (level is ClientLevel) {
				level.playLocalSound(entityPos.x, entityPos.y, entityPos.z, soundEvent, category, volume, pitch, false)
			} else {
				level.playSound(
					null, entityPos.x, entityPos.y, entityPos.z, soundEvent, category, volume, pitch
				)
			}
		}
	}

	companion object {
		/**
		 * 解析 JSON 声音效果定义。
		 * 格式：{ "time": { ... } } 或 { "time": [ {...}, {...} ] }
		 * 声音 ID 支持 "<id>; <molang_script>" 格式，分号后为前置 MoLang 脚本。
		 *
		 * @param soundsJson JSON 对象
		 * @return 声音事件列表
		 */
		@JvmStatic
		fun parses(soundsJson: JsonObject): List<SoundEvent> {
			val list = mutableListOf<SoundEvent>()
			soundsJson.asMap().forEach { (key, value) ->
				val effects = mutableListOf<Effect>()
				if (!value.isJsonArray) {
					parseEffect(value)?.let { effects.add(it) }
				} else {
					value.asJsonArray.forEach { parseEffect(it)?.let { e -> effects.add(e) } }
				}
				list.add(SoundEvent(key.toFloat(), effects))
			}
			return list
		}

		/**
		 * 解析单个声音效果 JSON 元素。
		 *
		 * @param element JSON 元素
		 * @return 解析后的 Effect
		 */
		@JvmStatic
		private fun parseEffect(element: JsonElement?): Effect? {
			element ?: return null
			val obj = element.asJsonObject
			val strings = obj.get("effect").asString.split(";", limit = 2)

			val preEffectScript: MolangValue? = if (strings.size > 1) MoLangParser.compileMolang(strings[1].trim()) else null
			val soundId = LibUtil.rlOf(strings[0].trim())
			val locators = obj.get("locator")?.asString

			return Effect(soundId, locators, preEffectScript = preEffectScript)
		}
	}
}
