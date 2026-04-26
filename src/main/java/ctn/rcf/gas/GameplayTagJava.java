package ctn.rcf.gas;

import cn.solarmoon.spark_core.gas.GameplayTag;

public class GameplayTagJava {
  public GameplayTag createGameplayTag(final String value) {
    return new GameplayTag(value);
  }
}
