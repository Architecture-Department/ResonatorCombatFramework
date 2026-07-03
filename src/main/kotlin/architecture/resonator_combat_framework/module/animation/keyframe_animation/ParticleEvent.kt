package architecture.resonator_combat_framework.module.animation.keyframe_animation

import architecture.goldenboughs_lib.util.*
import architecture.resonator_combat_framework.core.RcfEventHooks
import architecture.resonator_combat_framework.module.animation.ParticleStormAnimAdapter
import architecture.resonator_combat_framework.module.animation.controller.IEntityAnimationController
import architecture.resonator_combat_framework.module.animation.model.GeometryModel
import architecture.resonator_combat_framework.module.animation.model.PoseData
import architecture.resonator_combat_framework.module.animation.molang.MoLangParser
import architecture.resonator_combat_framework.module.animation.molang.MolangData
import architecture.resonator_combat_framework.module.animation.molang.MolangValue
import architecture.resonator_combat_framework.module.animation.molang.withScope
import architecture.resonator_combat_framework.util.RcfUtil
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.math.Axis
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import org.joml.Matrix4f

/**
 * 粒子事件数据类，在动画时间线的指定时刻触发粒子效果。
 * 每个事件可包含一个或多个粒子效果，效果会绑定到骨骼定位器上播放。
 *
 * @property time 触发时间（秒）
 * @property effects 粒子效果列表
 */
data class ParticleEvent
@JvmOverloads constructor(
	val time: Float, val effects: List<Effect> = emptyList()
) {
	/**
	 * 执行所有粒子效果。
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
		brModel: GeometryModel,
		animationData: PoseData,
		context: MolangData? = null,
		partialTick: Float = 1f
	) {
		effects.forEach { it.run(controller, entity, brModel, animationData, context, partialTick) }
	}

	/**
	 * 单个粒子效果定义。
	 *
	 * @property particleId 粒子类型 ID
	 * @property locatorName 定位器名称，决定粒子生成位置
	 * @property bindToActor 是否绑定到实体
	 * @property preEffectScript 粒子生成前执行的 MoLang 脚本
	 * @property preEffectScriptStr 原始 MoLang 脚本字符串
	 */
	data class Effect(
		val particleId: ResourceLocation,
		val locatorName: String? = null,
		val bindToActor: Boolean = true,
		val preEffectScript: MolangValue? = null,
		val preEffectScriptStr: String? = null
	) {
		/**
		 * 执行单个粒子效果。
		 * 先尝试 ParticleStorm 集成，失败后回退到事件钩子系统。
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
			brModel: GeometryModel,
			animationData: PoseData,
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
				.translate(pos1.x, pos1.y, pos1.z)
				.rotate(Axis.YP.rotation(-entity.getPreciseBodyRotation(1.0f).toRadians()))
				.mul(brModel.computeLocatorGlobalMatrix(locatorName, animationData, isWorld = true))
			var pos = matrix.toPos()
			var rotate = matrix.toRot()
			val event = RcfEventHooks.animationParticlePre(
				controller,
				locatorName,
				particleId,
				Value.of(particleType),
				Value.of(rotate),
				Value.of(pos),
			)

			if (event.isCanceled) return

			particleType = event.particle.newValue
			pos = event.pos.newValue
			rotate = event.rotate.newValue

			RcfEventHooks.animationParticlePost(
				controller,
				locatorName,
				particleId,
				particleType,
				rotate,
				pos
			)

		}
	}

	companion object {
		/**
		 * 解析 JSON 粒子效果定义。
		 * 格式：{ "time": { ... } } 或 { "time": [ {...}, {...} ] }
		 *
		 * @param particlesJson JSON 对象
		 * @return 粒子事件列表
		 */
		@JvmStatic
		fun parses(particlesJson: JsonObject): List<ParticleEvent> {
			val list = mutableListOf<ParticleEvent>()
			particlesJson.asMap().forEach { (key, value) ->
				val effects = mutableListOf<Effect>()
				if (!value.isJsonArray) {
					parseEffect(value)?.let { effects.add(it) }
				} else {
					value.asJsonArray.forEach { parseEffect(it)?.let { e -> effects.add(e) } }
				}
				list.add(ParticleEvent(key.toFloat(), effects))
			}
			return list
		}

		/**
		 * 解析单个粒子效果 JSON 元素。
		 *
		 * @param element JSON 元素
		 * @return 解析后的 Effect
		 */
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
