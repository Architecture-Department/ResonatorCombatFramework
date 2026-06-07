package architecture.resonator_combat_framework.module.entity_animation.engine

import architecture.goldenboughs_lib.util.LibUtil.rlOf
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MathParser
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d

data class BrAnimationParticle(
	val time: Float,
	val effects: List<Effect> = emptyList()
) {
	fun apply(entity: Entity, pos: Vector3d, context: MolangData? = null) {
		effects.forEach { it.apply(entity, pos, context) }
	}

	data class Effect(
		val particleId: ResourceLocation,
		val boneName: String? = null,
		val bindToActor: Boolean = true,
		val preEffectScript: MolangValue? = null
	) {
		fun apply(entity: Entity, pos: Vector3d, context: MolangData? = null) {
			preEffectScript?.get(context)
			val particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleId) ?: return
			val emitPos = if (bindToActor) entity.position() else Vec3(pos.x, pos.y, pos.z)
			if (entity.level().isClientSide) {
				@Suppress("UNCHECKED_CAST")
				val particle = particleType as? ParticleOptions
				if (particle != null) {
					entity.level().addParticle(particle, emitPos.x, emitPos.y, emitPos.z, 0.0, 0.0, 0.0)
				}
			}
		}
	}

	companion object {
		fun parses(particlesJson: JsonObject): List<BrAnimationParticle> {
			val list = mutableListOf<BrAnimationParticle>()
			particlesJson.asMap().forEach { (key, value) ->
				val effects = mutableListOf<Effect>()
				if (!value.isJsonArray) {
					parseEffect(value)?.let { effects.add(it) }
				} else {
					value.asJsonArray.forEach { parseEffect(it)?.let { e -> effects.add(e) } }
				}
				list.add(BrAnimationParticle(key.toFloat(), effects))
			}
			return list
		}

		private fun parseEffect(element: JsonElement?): Effect? {
			element ?: return null
			val obj = element.asJsonObject
			val particleId = rlOf(obj.get("effect").asString)
			val boneName = obj.get("locator")?.asString
			val bindToActor = obj.get("bind_to_actor")?.asBoolean ?: true
			val preEffectScript = obj.get("pre_effect_script")?.let { MathParser.compileMolang(it.asString) }
			return Effect(particleId, boneName, bindToActor, preEffectScript)
		}
	}
}