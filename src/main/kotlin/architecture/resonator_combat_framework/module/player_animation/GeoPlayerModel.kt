package architecture.resonator_combat_framework.module.player_animation

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.model.GeoModel
import java.util.function.BiConsumer
import java.util.function.Function

class GeoPlayerModel(val player: Player) : GeoModel<GeoPlayer>() {
	override fun getModelResource(animatable: GeoPlayer): ResourceLocation {
		return MODEL
	}

	override fun getTextureResource(animatable: GeoPlayer): ResourceLocation {
		return TEXTURE
	}

	override fun getAnimationResource(animatable: GeoPlayer): ResourceLocation {
		return ANIMATION
	}

	class ProxiedPlayerAnimationController
	@JvmOverloads
	constructor(
		animatable: GeoPlayer,
		name: String,
		animationHandler: AnimationStateHandler<GeoPlayer>,
		val triggerCondition: Function<Player, Boolean>,
		val outroAnim: BiConsumer<GeoPlayer, ProxiedPlayerAnimationController> = OUTRO_DEFAULT
	) : AnimationController<GeoPlayer>(animatable, name, animationHandler) {

		fun outro(model: GeoPlayer) {
			this.outroAnim.accept(model, this)
		}

		fun check(player: Player): Boolean {
			return this.triggerCondition.apply(player)
		}

		companion object {
			@JvmField
			val OUTRO_DEFAULT: BiConsumer<GeoPlayer, ProxiedPlayerAnimationController> =
				{ model, animationController -> animationController.forceAnimationReset() }
		}
	}

	companion object {
		const val TEST: String = "test_tekoki"
		const val OTSUCHI_HOLD: String = "otsuchi.hold"
		const val OTSUCHI_SMASH: String = "otsuchi.smash"

		@JvmField
		val MODEL: ResourceLocation = ResourceLocation.parse("geo/player/player.geo.json")

		@JvmField
		val TEXTURE: ResourceLocation = ResourceLocation.parse("textures/geo/empty.png")

		@JvmField
		val ANIMATION: ResourceLocation = ResourceLocation.parse("animations/entity/player.animation.json")

		@JvmField
		val ANIM_TEST: RawAnimation = RawAnimation.begin().thenPlay(TEST)

		@JvmField
		val ANIM_OTSUCHI_HOLD: RawAnimation = RawAnimation.begin().thenPlay(OTSUCHI_HOLD)

		@JvmField
		val ANIM_OTSUCHI_SMASH: RawAnimation = RawAnimation.begin().thenPlay(OTSUCHI_SMASH)
	}
}
