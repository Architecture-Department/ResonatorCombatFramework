package architecture.resonator_combat_framework.event

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import software.bernie.geckolib.animation.AnimationController

abstract class PlayerControllerEvent(player: Player) : PlayerEvent(player) {

	class Register(player: Player) : PlayerControllerEvent(player) {
		private val controllers: LinkedHashMap<String, AnimationController<Player>> = linkedMapOf()

		fun getAll(): Map<String, AnimationController<Player>> {
			return controllers.toMap()
		}

		fun remove(name: ResourceLocation) {
			controllers.remove(name.toString())
		}

		@JvmOverloads
		fun register(
			name: ResourceLocation,
			transitionTickTime: Int = 0,
			animationHandler: AnimationController.AnimationStateHandler<Player>
		) {
			val name = name.toString()
			controllers[name] = AnimationController(entity, name, transitionTickTime, animationHandler)
		}
	}
}