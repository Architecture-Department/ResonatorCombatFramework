package architecture.resonator_combat_framework.gas;

import cn.solarmoon.spark_core.gas.GameplayTag;

import javax.annotation.Nonnull;

public class GameplayTagBuilder {
	private final StringBuilder sb = new StringBuilder();

	public GameplayTagBuilder() {
	}

	public GameplayTagBuilder(@Nonnull final GameplayTag other) {
		this.sb.append(other.getPath());
	}

	public GameplayTagBuilder(@Nonnull final GameplayTagJava other) {
		this.sb.append(other.path());
	}

	public GameplayTagBuilder(final String path) {
		this.sb.append(path);
	}

	public GameplayTagBuilder append(final String path) {
		this.sb.append(".").append(path);
		return this;
	}

	public GameplayTagBuilder append(final GameplayTagJava other) {
		this.sb.append(".").append(other.path());
		return this;
	}

	public GameplayTagBuilder append(@Nonnull final GameplayTag other) {
		this.sb.append(".").append(other.getPath());
		return this;
	}

	public GameplayTagJava build() {
		return new GameplayTagJava(sb.toString());
	}

	public GameplayTag buildKt() {
		return new GameplayTag(sb.toString());
	}
}
