# 玩家动画模块

## 概述

基于 eyelib (TT432/eyelib) 的 Minecraft Bedrock 动画引擎，不替换原版渲染器，通过操纵 PlayerModel ModelPart 的
pos/rot/scale 实现自定义骨骼动画与 vanilla 动画的平滑过渡。

## 架构

```
触发 → AnimatePlayerPayload (双端网络包)
     → PlayerAnimationTransformer.trigger(id)
     → blendFactor 过渡 (0=vanilla, 1=eyelib)
     → LivingEntityRendererMixin.applyTransform()
     → BrAnimator.tickAnimation() → BoneRenderInfos
     → 叠加到 PlayerModel.ModelPart
```

## 核心类

| 类                            | 文件                                     | 作用                                              |
|------------------------------|----------------------------------------|-------------------------------------------------|
| `PlayerAnimationTransformer` | `core/PlayerAnimationTransformer.kt`   | 每玩家实例，管理 blendFactor，动画偏移应用到 ModelPart          |
| `PlayerAnimationHelper`      | `helper/PlayerAnimationHelper.kt`      | 双端触发/停止/广播                                      |
| `AnimatePlayerPayload`       | `payload/AnimatePlayerPayload.kt`      | `ToServerAndClientPayload` 网络包                  |
| `LivingEntityRendererMixin`  | `mixin/LivingEntityRendererMixin.java` | 注入 render()，在 renderToBuffer 前调用 applyTransform |
| `PlayerMixin`                | `mixin/PlayerMixin.java`               | `@Unique` 持有 Transformer 实例                     |
| `PlayerProxyProvider`        | `mixed/PlayerProxyProvider.kt`         | Mixin 接口                                        |
| `IPlayerRcf`                 | `mixed/IPlayerRcf.kt`                  | 扩展 PlayerProxyProvider                          |

## BoneState 骨骼状态

`BoneStateRegistry` 支持注册骨骼级行为标签：

| 状态            | 效果                     |
|---------------|------------------------|
| `lock`        | 骨骼不受 vanilla 动画影响      |
| `no_fade_in`  | 动画立即到 blendFactor=1    |
| `no_fade_out` | 动画停止时立即回 blendFactor=0 |

### 扩展新状态

```kotlin
object MyState : BoneState {
    override val mode = BoneState.Mode.ROOT
    override fun lockVanilla() = true
}
BoneStateRegistry.register("my_state", MyState)
```

## animdata 数据文件

位置：`assets/<modid>/eyelib/animdata/<动画ID>.json`

### 格式

```json
{
    "bones": {
        "<骨骼名>": { "lock": true, "no_fade_in": true }
    },
    "timeline": {
        "<起始秒>-<结束秒>": {
            "bones": { "<骨骼名>": { "no_fade_out": true } }
        }
    }
}
```

- `bones`：动画活跃期间始终生效
- `timeline`：特定时间区间生效（会与根级合并）

## 测试命令

```
/test_anim <target> <anim_id>       触发动画（Tab 补全动画 ID）
/test_anim_stop <target>            停止动画
```

## 添加新动画

1. 创建资源文件：
  - `eyelib/animations/bedrock/<name>.json` — 动画关键帧
  - `eyelib/animation_controllers/<name>.json` — 动画控制器（可选）
  - `eyelib/animdata/<animation_id>.json` — 骨骼状态配置（可选）

2. 无需修改代码，动画由 AnimationManager 自动发现并加载

3. 测试：`/test_anim <player> animation.player.<name>`

## 数据流详情

```
Client: /test_anim Dev animation.player.otsuchi_hold
  → TestAnimCommand → PlayerAnimationHelper.requestPlayerAnimation(target, id)
  → AnimatePlayerPayload(id, uuid) → Server

Server: toServer()
  → PlayerAnimationHelper.pushPlayerAnimation(target, id)
  → transformer.trigger(id) + broadcast to all clients

Client: toClient()  
  → PlayerAnimationHelper.triggerPlayerAnimation(target, id)
  → transformer.trigger(id) → blendTarget=1

每帧渲染:
  LivingEntityRendererMixin.render()
  → transformer.applyTransform(model, partialTick)
  → BrAnimator.tickAnimation() → BoneRenderInfos
  → applyBone() → 叠加到 ModelPart (x/y/z/xRot/yRot/zRot/xScale/yScale/zScale)
  
停止:
  /test_anim_stop Dev
  → transformer.stop() → blendTarget=0 → blendFactor lerp to 0
```

## 坐标转换

- eyelib `renderPosition`：像素 → 直接叠加，Y 轴取反 (Bedrock Y-up → vanilla Y-down)
- eyelib `renderRotation`：弧度 → 直接叠加，X/Y 取反，Z 正向
- 锁定模式 (`lock`)：替换而非叠加
