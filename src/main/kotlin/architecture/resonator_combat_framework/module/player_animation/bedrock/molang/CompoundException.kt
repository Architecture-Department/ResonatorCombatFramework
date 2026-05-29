// MoLang 解析异常
package architecture.resonator_combat_framework.module.player_animation.bedrock.molang

class CompoundException(message: String?) : RuntimeException(message) {
	fun withMessage(newMessage: String?): CompoundException {
		return CompoundException(newMessage)
	}
}


