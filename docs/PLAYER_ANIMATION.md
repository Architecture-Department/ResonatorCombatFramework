# 玩家动画模块

## 概述

基于 eyelib 21.1.14 的 Bedrock 动画引擎，通过 ProxyModel 中间层解耦动画数据与模型渲染。

核心思路：控制器负责驱动动画 + crossfade → Mapper 负责合并多控制器 + 输出到 MC 模型。

## 模块结构

```
player_animation/
├── api/          — IAnimationMapper, ProxyModel/ProxyBone/ProxyLocator
├── command/      — /test_anim 命令
├── config/       — AnimationPlayConfig, ProxyBoneFlags, ProxyBoneConfigData, ProxyBoneConfigLoader
├── controller/   — IAnimationController, BaseAnimationController, IBoneController
│   └── eyelib/   — EyeLibAnimationController, EyelibBoneController, EyelibLocatorController
├── helper/       — PlayerAnimationHelper (双端便捷入口)
├── init/         — PlayerAnimationSetup, PlayerAnimationAttachments
├── mapper/       — EntityAnimationMapper → LivingEntity → HumanoidEntity → PlayerAnimationMapper
├── mixed/        — PlayerProxyProvider (Mixin 接口)
├── payload/      — AnimatePlayerPayload (双端网络包)
└── util/         — EyeLibUtil
```

## 继承链

```
IAnimationMapper                        — 根接口: 生命周期+控制器管理+骨骼冲突
  ↑
EntityAnimationMapper<T:Entity, M>      — 控制器注册+tick/trigger/stop, root PoseStack
  ↑
LivingEntityAnimationMapper<T, M>       — animTimeTracker + boneConfigs
  ↑
HumanoidEntityAnimationMapper<T, M>     — 6人形骨骼 applyProxyBone + 物品 applyProxyToItem
  ↑
PlayerAnimationMapper                   — EyeLibAnimationController + jacket/sleeve/pants
```

## 控制器

```
IAnimationController                    — 纯接口
  ↑
BaseAnimationController                 — 状态机(TRANSITIONING/PLAYING/PAUSED/FADING_OUT) + crossfade
  ↑
EyeLibAnimationController               — eyelib 后端适配
  ├── EyelibBoneController              — BoneRenderInfos → ProxyModel.bones
  └── EyelibLocatorController           — locator 读取（保留，暂无实际作用）
```

## 数据流

```
trigger(AnimationPlayConfig)
  → Mapper.trigger → 控制器.trigger
    → 加载动画 → 保存当前 proxyModel 快照(transitionSource) → 清空 proxyModel
    → 重建后端 + freezeAllAtFrameZero → blendFactor 从 0→1 (TRANSITIONING)

每帧 tickAndRender:
  → Mapper.tick() → 每个控制器.tick(deltaSec)
    → tickBackend() → EyelibBoneController 写入 proxyModel
    → crossfadeStep() → 逐骨骼 lerp(transitionSource, proxyModel, blendFactor)
  → collectProxyModels() → 合并多控制器
  → resolveMergedFlags() → 合并骨骼配置
  → applyRootTransform(proxyModels, poseStack, weight)
  → applyProxyToModel(proxyModels, model, flags, weight)
```

## AnimationPlayConfig（播放配置）

```kotlin
data class AnimationPlayConfig(
    val animId: String,                      // 动画名称
    val controllerName: String = "default",  // 控制器 ID
    val animType: AnimType = DEFAULT,        // 播放类型
    val startTime: Int = 0,                  // 起始时间(tick)
    val endTime: Int = 0,                    // 结束时间(tick), 0=到末尾, 负数=N帧前
    val speedMultiplier: Float = 1f,         // 播放速率
    val durationTicks: Int = 0,              // 在指定tick内播完(与speed互斥)
    val originalAnimLengthSec: Float = 0f,   // 原始时长,配合durationTicks
    val boneConfig: ProxyBoneConfigData? = null,  // 手动骨骼配置(覆盖默认)
    val fadeInTicks: Int = -1,               // 淡入tick, -1=用默认
    val fadeOutTicks: Int = -1               // 淡出tick, -1=用默认
)

enum class AnimType {
    DEFAULT,       // 按动画本身的 loop 类型
    PLAY_ONCE,     // 强制播放一次后停止
    STOP_AT_LAST,  // 播放一次，停在最后一帧
    LOOP           // 强制循环
}
```

### Builder 示例

```kotlin
AnimationPlayConfig.builder("player.idle")
    .type(AnimType.LOOP)
    .speed(1.5f)
    .fadeIn(5)
    .fadeOut(10)
    .boneConfig(myConfig)
    .build()
```

## 骨骼配置（ProxyBoneFlags）

用 `Map<String, Boolean>` 存储 + Kotlin 扩展函数访问。所有函数接受 nullable receiver。

JSON 嵌套格式，加载时打平为 dot-notation：

```json
{
  "bones": {
    "head": {
      "lock": false,         // 锁定整个骨骼(位置+旋转+缩放)
      "blend": true,         // 是否参与 crossfade
      "transition": true,    // 是否参与淡入淡出 weight
      "pos":  { "x": true, "y": true, "z": true, "lock": false },
      "rot":  { "x": true, "y": true, "z": true, "lock": false },
      "scale":{ "x": true, "y": true, "z": true, "lock": false }
    }
  },
  "timeline": {
    "0.0-1.0": {
      "bones": { "right_arm": { "rot": { "lock": true } } }
    }
  },
  "transition": 3
}
```

内置 key（dot-notation）:

| key           | 含义           | 默认    |
|---------------|--------------|-------|
| `lock`        | 锁定整个骨骼       | false |
| `blend`       | 参与 crossfade | true  |
| `transition`  | 参与 weight    | true  |
| `pos.lock`    | 锁定位置         | false |
| `rot.lock`    | 锁定旋转         | false |
| `scale.lock`  | 锁定缩放         | false |
| `pos.x/y/z`   | 轴启用          | true  |
| `rot.x/y/z`   | 轴启用          | true  |
| `scale.x/y/z` | 轴启用          | true  |

**扩展函数（nullable receiver）**：

```kotlin
fun ProxyBoneFlags?.lockPos(): Boolean         // null → false
fun ProxyBoneFlags?.shouldBlend(): Boolean      // null → true
fun ProxyBoneFlags?.shouldTransition(): Boolean // null → true
fun ProxyBoneFlags?.isEnabled(axis: String): Boolean  // null → true
```

第三方可通过 `flags["custom.key"]` 扩展自定义标志。

## PlayerAnimationHelper（便捷 API）

```kotlin
// 触发
player.triggerPlayerAnimation("anim_id")
player.triggerPlayerAnimation(AnimationPlayConfig.builder("anim_id").speed(1.5f).build())
player.triggerPlayerAnimationForDuration("anim_id", 40, 2.0f)

// 停止
player.stopPlayerAnimation()              // 带淡出
player.stopPlayerAnimationImmediate()     // 立即

// 暂停/恢复
player.pausePlayerAnimation()
player.resumePlayerAnimation()
```

所有方法在客户端和服务端均可调用，服务端会自动同步到追踪的客户端。

## 测试命令

```
/test_anim <target> play <anim_id> [speed] [fade_in]
/test_anim <target> stop
/test_anim <target> stop_immediate
/test_anim <target> pause
/test_anim <target> resume
```

## 资源位置

```
assets/<modid>/eyelib/
├── animations/player/     — Bedrock 动画关键帧 .json
├── animation_controllers/ — 动画控制器 .json
├── animdata/player/       — 骨骼配置 .json (ProxyBoneConfigLoader 读取)
├── bedrock_models/        — 模型几何体 .json
└── textures/              — 纹理
```
