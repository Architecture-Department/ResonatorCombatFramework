package ctn.rcf.gas;

import cn.solarmoon.spark_core.gas.GameplayTag;
import com.google.common.collect.ImmutableList;
import cpw.mods.util.Lazy;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;

public class GameplayTagJava {
  private final Lazy<GameplayTag> ktObject;
  public final String path;

  public GameplayTagJava(String path) {
    this.path = path;
    this.ktObject = Lazy.of(() -> new GameplayTag(this.path));
  }

  public List<String> parts() {
    return ImmutableList.copyOf(this.path.split("\\."));
  }

  public boolean matchs(@Nonnull final GameplayTagJava other) {
    final List<String> parts = this.parts();
    final List<String> otherParts = other.parts();

    if (otherParts.size() > parts.size()) {
      return false;
    }

    return new HashSet<>(otherParts).containsAll(otherParts.subList(0, parts.size()));
  }

  public boolean matchs(@Nonnull final GameplayTag other) {
    return this.asKotlinObject().matches(other);
  }

  public GameplayTag asKotlinObject() {
    return ktObject.get();
  }

  @Override
  public String toString() {
    return this.path;
  }

  @Override
  public int hashCode() {
    return this.path.hashCode();
  }
}
