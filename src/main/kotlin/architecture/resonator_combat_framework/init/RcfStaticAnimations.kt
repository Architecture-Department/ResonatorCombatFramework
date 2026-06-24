package architecture.resonator_combat_framework.init

import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation

object RcfStaticAnimations {
	lateinit var REGISTRY: StaticAnimation

	@JvmStatic
	fun init(isClient: Boolean) {
		REGISTRY = register(RcfUtil.modRl("idle"), ::StaticAnimation)
	}

	@JvmStatic
	inline fun <T : StaticAnimation> register(
		id: ResourceLocation,
		staticAnimation: (id: ResourceLocation) -> T,
		isClient: Boolean? = null
	): T {
		val staticAnimation1 = staticAnimation(id)
		when (isClient) {
			true -> {
				RcfRegistries.getStaticAnimations(true)[id] = staticAnimation1
				staticAnimation1.init(true)
			}
			false -> {
				RcfRegistries.getStaticAnimations(false)[id] = staticAnimation1
				staticAnimation1.init(false)
			}
			else -> {
				RcfRegistries.getStaticAnimations(true)[id] = staticAnimation1
				staticAnimation1.init(true)
				RcfRegistries.getStaticAnimations(false)[id] = staticAnimation1
				staticAnimation1.init(false)
			}
		}
		return staticAnimation1
	}
}
