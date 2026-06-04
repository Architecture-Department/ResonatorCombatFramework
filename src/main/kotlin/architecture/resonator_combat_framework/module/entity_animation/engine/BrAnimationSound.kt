package architecture.resonator_combat_framework.module.entity_animation.engine

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import org.joml.Vector3d

data class BrAnimationSound(
	val time: Float,
	val effects: List<Effect> = emptyList()
) {
	fun apply(entity: Entity, pos: Vector3d) {
		effects.forEach { it.apply(entity, pos) }
	}

	data class Effect(
		val particleId: ResourceLocation,
		val boneName: String? = null,
		val bindToActor: Boolean = true,
		val preEffectScript: MolangValue? = null
	) {
		fun apply(entity: Entity, pos: Vector3d) {
			val soundEvent = BuiltInRegistries.SOUND_EVENT.get(particleId) ?: return
			if (soundEvent !is ParticleOptions) return
			preEffectScript?.get()
			// TODO: 未完成
			val x = pos.x
			val y = pos.y
			val z = pos.z
			val soundSource = entity.soundSource
			val volume = 1f
			val pitch = 1f
			entity.level().playSound(null, x, y, z, soundEvent, soundSource, volume, pitch)
		}
	}

	companion object {
		fun parses(particlesJson: JsonObject): List<BrAnimationSound> {
			val list = mutableListOf<BrAnimationSound>()
			particlesJson.asMap().forEach { (key, value) ->
				val effects = mutableListOf<Effect>()
				if (!value.isJsonArray) {
					parsesEffect(value)?.apply { effects.add(this@apply) }
				} else {
					value.asJsonArray.forEach {
						parsesEffect(it)?.apply { effects.add(this@apply) }
					}
				}
				list.add(BrAnimationSound(key.toFloat(), effects))
			}
			return list
		}

		private fun parsesEffect(
			element: JsonElement?
		): Effect? {
			element ?: return null
			val asJsonObject = element.asJsonObject
			val particleId = rlOf(asJsonObject.get("effect").asString)
			val boneName = asJsonObject.get("locator")?.asString
			return Effect(particleId, boneName)
		}
	}
}