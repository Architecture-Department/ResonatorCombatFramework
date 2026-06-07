package architecture.resonator_combat_framework.module.entity_animation.engine.molang

// MoLang 解析异常
class CompoundException(message: String?) : RuntimeException(message) {
	fun withMessage(newMessage: String?): CompoundException {
		return CompoundException(newMessage)
	}
}

