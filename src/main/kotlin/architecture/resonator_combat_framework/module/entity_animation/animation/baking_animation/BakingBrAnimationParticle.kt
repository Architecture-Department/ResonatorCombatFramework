package architecture.resonator_combat_framework.module.entity_animation.animation.baking_animation

import architecture.goldenboughs_lib.util.*
import architecture.resonator_combat_framework.module.entity_animation.animation.ParticleStormAnimAdapter
import architecture.resonator_combat_framework.module.entity_animation.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.entity_animation.animation.model.BrModel
import architecture.resonator_combat_framework.module.entity_animation.animation.model.ProxyModel
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MoLangParser
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangData
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.MolangValue
import architecture.resonator_combat_framework.module.entity_animation.animation.molang.withScope
import architecture.resonator_combat_framework.module.entity_animation.event.AnimationParticleEvent
import architecture.resonator_combat_framework.module.entity_animation.event.Value
import architecture.resonator_combat_framework.util.RcfUtil
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.math.Axis
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.neoforged.neoforge.common.NeoForge
import org.joml.Matrix4f

data class BakingBrAnimationParticle
@JvmOverloads constructor(
	val time: Float, val effects: List<Effect> = emptyList()
) {
	fun runs(
		controller: IEntityAnimationController<*>,
		entity: Entity,
		brModel: BrModel,
		animationData: ProxyModel,
		context: MolangData? = null,
		partialTick: Float = 1f
	) {
		effects.forEach { it.run(controller, entity, brModel, animationData, context, partialTick) }
	}

	data class Effect(
		val particleId: ResourceLocation,
		val locatorName: String? = null,
		val bindToActor: Boolean = true,
		val preEffectScript: MolangValue? = null,
		val preEffectScriptStr: String? = null
	) {
		fun run(
			controller: IEntityAnimationController<*>,
			entity: Entity,
			brModel: BrModel,
			animationData: ProxyModel,
			context: MolangData? = null,
			partialTick: Float = 1f
		) {
			locatorName ?: return

			if (RcfUtil.PARTICLESTORM_LOADED && ParticleStormAnimAdapter.trySpawnParticle(
					entity,
					particleId,
					locatorName,
					preEffectScriptStr,
					brModel,
					animationData,
					controller,
					controller.manager
				)
			) return

			if (preEffectScript != null) {
				context?.withScope { scope ->
					preEffectScript.eval(scope)
				}
			}

			var particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleId)

			val pos1 = entity.position().toVector3f()
			val matrix = Matrix4f()
			val poseStack = PoseStack()
			poseStack.translate(pos1.x, pos1.y, pos1.z)
			poseStack.mulPose(Axis.YP.rotation(-entity.getPreciseBodyRotation(1.0f).toRadians()))
			poseStack.pushPose()
			brModel.computeLocatorGlobalMatrix(locatorName, animationData, poseStack, isWorld = true)
			var pos = matrix.toPos()
			var rotate = matrix.toRot()
			val event = NeoForge.EVENT_BUS.post(
				AnimationParticleEvent.Pre(
					controller,
					locatorName,
					particleId,
					Value.of(particleType),
					Value.of(rotate),
					Value.of(pos),
				)
			)

			if (event.isCanceled) return

			particleType = event.particle.newValue
			pos = event.pos.newValue
			rotate = event.rotate.newValue

			NeoForge.EVENT_BUS.post(
				AnimationParticleEvent.Post(
					controller,
					locatorName,
					particleId,
					particleType,
					rotate,
					pos
				)
			)
		}
	}

	companion object {
		@JvmStatic
		fun parses(particlesJson: JsonObject): List<BakingBrAnimationParticle> {
			val list = mutableListOf<BakingBrAnimationParticle>()
			particlesJson.asMap().forEach { (key, value) ->
				val effects = mutableListOf<Effect>()
				if (!value.isJsonArray) {
					parseEffect(value)?.let { effects.add(it) }
				} else {
					value.asJsonArray.forEach { parseEffect(it)?.let { e -> effects.add(e) } }
				}
				list.add(BakingBrAnimationParticle(key.toFloat(), effects))
			}
			return list
		}

		@JvmStatic
		private fun parseEffect(element: JsonElement?): Effect? {
			element ?: return null
			val obj = element.asJsonObject
			val particleId = LibUtil.rlOf(obj.get("effect").asString)
			val boneName = obj.get("locator")?.asString
			val bindToActor = obj.get("bind_to_actor")?.asBoolean ?: true
			val preEffectScriptStr = obj.get("pre_effect_script")?.asString
			val preEffectScript = preEffectScriptStr?.let { MoLangParser.compileMolang(it) }
			return Effect(particleId, boneName, bindToActor, preEffectScript, preEffectScriptStr)
		}
	}
}