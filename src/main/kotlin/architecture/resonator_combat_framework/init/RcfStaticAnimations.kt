package architecture.resonator_combat_framework.init

import architecture.goldenboughs_lib.util.LazySupplier
import architecture.resonator_combat_framework.module.entity_animation.animation.StaticAnimation
import architecture.resonator_combat_framework.util.RcfUtil
import net.minecraft.resources.ResourceLocation

object RcfStaticAnimations {
	private val CLIENT_STATIC_ANIMATIONS = mutableMapOf<ResourceLocation, LazySupplier<StaticAnimation>>()
	private val SERVER_STATIC_ANIMATIONS = mutableMapOf<ResourceLocation, LazySupplier<StaticAnimation>>()

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
				getStaticAnimations(true)[id] = LazySupplier { staticAnimation1 }
			}

			false -> {
				getStaticAnimations(false)[id] = LazySupplier { staticAnimation1 }
			}

			else -> {
				getStaticAnimations(true)[id] = LazySupplier { staticAnimation1 }
				getStaticAnimations(false)[id] = LazySupplier { staticAnimation1 }
			}
		}
		return staticAnimation1
	}

	@JvmStatic
	fun getStaticAnimations(isClient: Boolean): MutableMap<ResourceLocation, LazySupplier<StaticAnimation>> {
		return if (isClient) CLIENT_STATIC_ANIMATIONS else SERVER_STATIC_ANIMATIONS
	}

	@JvmStatic
	fun getStaticAnimation(isClient: Boolean, id: ResourceLocation): StaticAnimation? {
		return if (isClient) CLIENT_STATIC_ANIMATIONS[id]?.get() else SERVER_STATIC_ANIMATIONS[id]?.get()
	}

	@JvmStatic
	fun getStaticAnimation(isClient: Boolean, id: String): StaticAnimation? {
		return getStaticAnimation(isClient, RcfUtil.modRl(id))
	}
}
