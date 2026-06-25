package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation

object RcfStaticAnimations {
	lateinit var REGISTRY: StaticAnimation

	@JvmStatic
	internal fun register() {
		REGISTRY = register("idle", ::StaticAnimation)
	}

	internal fun <T : StaticAnimation> register(
		animationId: String,
		function: (id: ResourceLocation) -> T,
		isClient: Boolean? = null
	): T {
		return register(RcfUtil.modRl(animationId), function, isClient)
	}

	@JvmStatic
	fun <T : StaticAnimation> register(
		id: ResourceLocation,
		function: (id: ResourceLocation) -> T,
		isClient: Boolean? = null
	): T {
		val staticAnimation1 = function(id)
		when (isClient) {
			true -> {
				RcfRegistries.getStaticAnimations(true)[id] = staticAnimation1
			}
			false -> {
				RcfRegistries.getStaticAnimations(false)[id] = staticAnimation1
			}
			else -> {
				RcfRegistries.getStaticAnimations(true)[id] = staticAnimation1
				RcfRegistries.getStaticAnimations(false)[id] = staticAnimation1
			}
		}
		return staticAnimation1
	}
}
