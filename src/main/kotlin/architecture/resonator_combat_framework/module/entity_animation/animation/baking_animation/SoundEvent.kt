package architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation

import architecture.goldenboughs_lib.util.LibUtil
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.PoseData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MoLangParser
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.withScope
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

data class SoundEvent
@JvmOverloads constructor(
	val time: Float, val effects: List<Effect> = emptyList()
) {
	fun runs(
		controller: IEntityAnimationController<*>,
		entity: Entity,
		brModel: BrModel,
		animationData: PoseData,
		context: MolangData? = null,
		partialTick: Float = 1f
	) {
		effects.forEach { it.run(controller, entity, brModel, animationData, context, partialTick) }
	}

	data class Effect
	@JvmOverloads constructor(
		val soundId: ResourceLocation,
		val locatorName: String? = null,
		val bindToActor: Boolean = true,
		val preEffectScript: MolangValue? = null
	) {
		fun run(
			controller: IEntityAnimationController<*>,
			entity: Entity,
			brModel: BrModel,
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