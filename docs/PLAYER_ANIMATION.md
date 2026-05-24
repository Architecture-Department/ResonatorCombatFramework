# 玩家动画模块

## 概述

基于 eyelib 21.1.14 的 Bedrock 动画引擎，通过 ProxyModel 中间层解耦动画数据与模型应用。

## API

```kotlin
interface IAnimationMapper {
    companion object { const val DEFAULT_CONTROLLER_NAME = "default" }

    val animControllerMap: MutableMap<String, String>
    fun resolveController(animId: String): String

    // 触发
    fun trigger(animId: String)
    fun trigger(controllerName: String, animId: String)

    // 停止
    fun stop(controllerName: String)
    fun stopAll()
    fun stopAnimation(animId: String)
    fun stopAnimation(controllerName: String, animId: String)

    // 查询
    fun isActive(): Boolean
    fun isControllerActive(): Boolean
    fun isControllerActive(controllerName: String): Boolean
    fun getController(): IAnimationController
    fun getController(controllerName: String): IAnimationController
    fun hasController(): Boolean
    fun hasController(controllerName: String): Boolean

    // 管理
    fun addController(name: String, controller: IAnimationController)
    fun removeController(name: String)
}
```

## 继承链

```
IAnimationMapper
  ↑
EntityAnimationMapper<T:Entity, M:EntityModel<T>>   ← 控制器 + 生命周期 + 配置
  ↑
LivingEntityAnimationMapper<T:LivingEntity, M>       ← resolveBoneFlags + animTimeTracker
  ↑
HumanoidEntityAnimationMapper<T:LivingEntity, M:HumanoidModel<T>> ← applyProxyToModel + applyProxyBone + applyProxyToItem
  ↑
PlayerAnimationMapper                                 ← EyeLibAnimationController + 渲染
```

## 控制器

```kotlin
interface IAnimationController {
    var blendFactor: Float
    var blendTarget: Float
    var currentTransitionTicks: Int
    fun isActive()
    fun tick(partialTick, deltaSec)
    fun trigger(animId, transitionTicks)
    fun stop()
    fun stopAnimation(animId)
    fun restartAnimation(animId)
}
```

## 过渡系统

`ProxyBoneConfigData.transitionTicks: Int` (tick制, 0=即时, 默认10=0.5s)

交叉淡入淡出: 深拷贝旧 ProxyModel → 清空 eyelib → 只注册新动画 → 每帧 lerp 新旧 ProxyModel

## 添加新动画

1. `eyelib/animations/player/<name>.json` — 动画关键帧 (必需)
2. `eyelib/animdata/player/<name>.json` — 配置 (可选)
3. 测试: `/test_anim <player> <动画名>`
