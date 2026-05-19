package architecture.resonator_combat_framework.gas

import com.google.common.collect.ImmutableList

@JvmRecord
data class GameplayTagJava(val path: String) {
	//	private final Lazy<GameplayTag> ktObject;
	//		this.ktObject = Lazy.of(() -> new GameplayTag(this.path));
	fun parts(): MutableList<String> {
		return ImmutableList.copyOf(this.path.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
			.toTypedArray())
	}

	fun matchs(other: GameplayTagJava): Boolean {
		val parts = this.parts()
		val otherParts = other.parts()

		if (otherParts.size > parts.size) {
			return false
		}

		return HashSet(otherParts).containsAll(otherParts.subList(0, parts.size))
	}

	//	public boolean matchs(@Nonnull final GameplayTag other) {
	//		return this.asKotlinObject().matches(other);
	//	}
	//	public GameplayTag asKotlinObject() {
	//		return ktObject.get();
	//	}
	override fun toString(): String {
		return this.path
	}
}
