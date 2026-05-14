package architecture.resonator_combat_framework.module.player_animation

import net.minecraft.world.entity.player.Player
import software.bernie.geckolib.animation.RawAnimation
import java.util.function.BiConsumer

object GeoPlayerAnimations {
	@JvmField
	val RAW_ANIMATIONS: MutableMap<String, RawAnimation> = hashMapOf()

	private val PLAYER_ANIMATIONS: MutableMap<String, BiConsumer<Player, GeoPlayerModel>> =
		HashMap()

//	val TEST_TEKOKI_ANIM: String = register("test_tekoki") { player, model ->
//		model.triggerAnim<Any>(
//			player,
//			model.hashCode().toLong(),
//			GeoPlayerModel.TEST,
//			GeoPlayerModel.TEST
//		)
//	}
//	val OTSUCHI_HOLD_ANIM: String = register("otsuchi_hold") { player, model ->
//		model.triggerAnim<Any>(
//			player,
//			model.hashCode().toLong(),
//			GeoPlayerModel.OTSUCHI_HOLD,
//			GeoPlayerModel.OTSUCHI_HOLD
//		)
//	}
//	val OTSUCHI_SMASH_ANIM: String = register("otsuchi_smash") { player, model ->
//		model.triggerAnim<Any>(
//			player,
//			model.hashCode().toLong(),
//			GeoPlayerModel.OTSUCHI_SMASH,
//			GeoPlayerModel.OTSUCHI_SMASH
//		)
//	}

	fun register(id: String, anim: BiConsumer<Player, GeoPlayerModel>): String {
		PLAYER_ANIMATIONS[id] = anim
		return id
	}

	fun get(id: String): BiConsumer<Player, GeoPlayerModel>? {
		return PLAYER_ANIMATIONS[id]
	}
}
