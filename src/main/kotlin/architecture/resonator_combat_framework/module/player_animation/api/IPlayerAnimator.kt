package architecture.resonator_combat_framework.module.player_animation.api

interface IPlayerAnimator {
	fun trigger(animId: String)
	fun stop()
	fun stopAnimation(animId: String)
	fun isActive(): Boolean
}
