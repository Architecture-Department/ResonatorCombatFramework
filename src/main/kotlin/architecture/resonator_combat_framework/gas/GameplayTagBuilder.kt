package architecture.resonator_combat_framework.gas

import cn.solarmoon.spark_core.gas.GameplayTag

class GameplayTagBuilder {
	private val sb = StringBuilder()

	constructor()

	constructor(other: GameplayTag) {
		this.sb.append(other.path)
	}

	constructor(other: GameplayTagJava) {
		this.sb.append(other.path)
	}

	constructor(path: String?) {
		this.sb.append(path)
	}

	fun append(path: String?): GameplayTagBuilder {
		this.sb.append(".").append(path)
		return this
	}

	fun append(other: GameplayTagJava): GameplayTagBuilder {
		this.sb.append(".").append(other.path)
		return this
	}

	fun append(other: GameplayTag): GameplayTagBuilder {
		this.sb.append(".").append(other.path)
		return this
	}

	fun build(): GameplayTagJava {
		return GameplayTagJava(sb.toString())
	}

	fun buildKt(): GameplayTag {
		return GameplayTag(sb.toString())
	}
}
