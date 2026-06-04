package architecture.resonator_combat_framework.module.entity_animation.engine

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathParser
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import org.joml.Vector3d

data class BrAnimationParticle(
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
			val particle = BuiltInRegistries.PARTICLE_TYPE.get(particleId) ?: return
			if (particle !is ParticleOptions) return
			preEffectScript?.get()
			// TODO: 未完成
			val x = pos.x
			val y = pos.y
			val z = pos.z
			val xSpeed = pos.x
			val ySpeed = pos.y
			val zSpeed = pos.z
			entity.level().addParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed)
		}
	}

	companion object {
		fun parses(particlesJson: JsonObject): List<BrAnimationParticle> {
			val list = mutableListOf<BrAnimationParticle>()
			particlesJson.asMap().forEach { (key, value) ->
				val effects = mutableListOf<Effect>()
				if (!value.isJsonArray) {
					parsesEffect(value)?.apply { effects.add(this@apply) }
				} else {
					value.asJsonArray.forEach {
						parsesEffect(it)?.apply { effects.add(this@apply) }
					}
				}
				list.add(BrAnimationParticle(key.toFloat(), effects))
			}
			return list
		}

		private fun parsesEffect(
			element: JsonElement?
		): Effect? {
			element ?: return null
			val asJsonObject = element.asJsonObject
			val particleId = rlOf(asJsonObject.get("effect").asString)
			val boneName = asJsonObject.get("locator").asString
			val bindToActor = asJsonObject.get("bind_to_actor")?.asBoolean ?: true
			val preEffectScript =
				asJsonObject.get("pre_effect_script")?.let { MathParser.compileMolang(it.asString) }
			return Effect(particleId, boneName, bindToActor, preEffectScript)
		}
	}
}