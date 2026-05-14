package architecture.resonator_combat_framework.module.player_animation.mixed

import architecture.resonator_combat_framework.module.player_animation.GeoPlayer
import net.minecraft.world.entity.player.Player
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager

interface IPlayerAnimationExpand : GeoEntity {
	private fun getPlayer() = this as Any as Player

	fun `resonator_combat_framework$getAnimationGeoPlayer`(): GeoPlayer

	override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {}

	override fun getAnimatableInstanceCache(): AnimatableInstanceCache =
		getAnimationGeoPlayer().cache

	override fun getTick(`object`: Any): Double {
		return getAnimationGeoPlayer().getTick(`object`)
	}

	companion object {
		@JvmStatic
		fun of(plater: Player): IPlayerAnimationExpand {
			return plater as IPlayerAnimationExpand
		}

		@JvmStatic
		fun IPlayerAnimationExpand.getAnimationGeoPlayer(): GeoPlayer {
			return `resonator_combat_framework$getAnimationGeoPlayer`()
		}
	}
}