package architecture.resonator_combat_framework.module.entity_animation.engine

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import org.joml.Vector3d

data class BrAnimationSound(
	val time: Float,
	val effects: List<Effect> = emptyList()
) {
	fun apply(entity: Entity, pos: Vector3d, context: MolangData? = null) {
		effects.forEach { it.apply(entity, pos, context) }
	}

	data class Effect(
		val soundId: ResourceLocation,
		val boneName: String? = null,
		val bindToActor: Boolean = true,
		val preEffectScript: MolangValue? = null
	) {
		fun apply(entity: Entity, pos: Vector3d, context: MolangData? = null) {
			preEffectScript?.get(context)
			val soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundId) ?: return
			val x = if (bindToActor) entity.x else pos.x
			val y = if (bindToActor) entity.y else pos.y
			val z = if (bindToActor) entity.z else pos.z
			entity.level().playSound(null, x, y, z, soundEvent, SoundSource.PLAYERS, 1f, 1f)
		}
	}

	companion object {
		fun parses(soundsJson: JsonObject): List<BrAnimationSound> {
			val list = mutableListOf<BrAnimationSound>()
			soundsJson.asMap().forEach { (key, value) ->
				val effects = mutableListOf<Effect>()
				if (!value.isJsonArray) {
					parseEffect(value)?.let { effects.add(it) }
				} else {
					value.asJsonArray.forEach { parseEffect(it)?.let { e -> effects.add(e) } }
				}
				list.add(BrAnimationSound(key.toFloat(), effects))
			}
			return list
		}

		private fun parseEffect(element: JsonElement?): Effect? {
			element ?: return null
			val obj = element.asJsonObject
			val soundId = rlOf(obj.get("effect").asString)
			val boneName = obj.get("locator")?.asString
			return Effect(soundId, boneName)
		}
	}
}