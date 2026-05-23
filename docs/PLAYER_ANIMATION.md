# 玩家动画模块

## 概述

基于 eyelib 21.1.14 的 Bedrock 动画引擎，不替换原版渲染，通过操纵 PlayerModel ModelPart 的 pos/rot/scale
实现自定义骨骼动画与 vanilla 动画的混合。

## 架构

```
触发 (物品/命令/网络包)
  → IPlayerAnimator.trigger(animId)
  → PlayerAnimationTransformer
    → AnimationComponent.setup() → BrAnimator.tickAnimation() → BoneRenderInfos
    → applyBone() → 叠加到 ModelPart
```

动画不再通过 Bridge 间接调用 — `AnimationComponent.setup()` 直接注册 `BrAnimationEntry` / `BrAnimationController`，
eyelib 原生支持多动画并发和状态机。

## 核心类

| 类                                   | 作用                                               |
|-------------------------------------|--------------------------------------------------|
| `IPlayerAnimator` (`api/`)          | 公开 API 接口: trigger/stop/stopAnimation/isActive   |
| `PlayerAnimationTransformer`        | 实现 `IPlayerAnimator`, 管理过渡/交叉淡入淡出/骨骼变换           |
| `PlayerAnimationSetup`              | 初始化 RenderData (模型 + 渲染配置)                       |
| `PlayerAnimationHelper` (`helper/`) | 双端便捷方法: trigger/stop/request/push                |
| `EyeLibUtil` (`util/`)              | eyelib 操作集中管理: AnimationManager/Data/animate map |
| `AnimatePlayerPayload` (`payload/`) | `ToServerAndClientPayload` 双端网络包                 |
| `LivingEntityRendererMixin`         | 注入 render(), 调用 applyTransform                   |
| `PlayerMixin`                       | `@Unique` 持有 Transformer 实例                      |
| `PlayerProxyProvider` (`mixed/`)    | Mixin 接口, 返回 `IPlayerAnimator`                   |

## 过渡系统

过渡是动画文件整体的属性，通过 `RcfBoneConfig.transitionTicks: Int` 配置 (默认 10, 0.5 秒)。
数值为 tick 制: 0=即时, 20=1秒。

### 三种过渡模式

1. **fade-in** (首个动画触发): blendFactor 0→1, 视觉从原版渐入动画
2. **fade-out** (stop): blendFactor 1→0, 动画保持运行直到 blendFactor 归零后清理
3. **交叉淡入淡出** (trigger B 替换 A): A/B 同时在 eyelib 中运行, multiplier 权重 A:1→0 B:0→1

### 过渡实现

- 直接存储 `currentTransitionTicks: Int`，每帧步长 `(deltaSec * 20f) / currentTransitionTicks`
- `currentTransitionTicks <= 0` 时直接跳转 (即时切换)
- `blendFactor` 每帧向 `blendTarget` 逼近
- 交叉淡入淡出期间 eyelib multiplier 控制 A/B 权重, `applyBone` weight 固定为 1f (避免双重衰减)
- `AnimationComponent.setup()` 只在结构性变化时调用 (trigger 初始), 逐帧 multiplier 更新通过
  `EyeLibUtil.setAnimateEntry()` 操作 `animate` map
- 旧动画 `animTime` 在 `rebuildAnimate()` 前后保存/恢复, 确保交叉淡入淡出时不跳帧

### 同一动画重复触发

检测到 `animId` 已在 `activeAnimations` 或 `previousAnimations` 中时, 直接重置 `animTime=0` 重启,
不和自己交叉淡入淡出。

## BoneState 骨骼状态

`BoneStateRegistry` (`config/BoneStateRegistry.kt`) 注册骨骼行为标签:

| 状态     | 效果                                  |
|--------|-------------------------------------|
| `lock` | 骨骼旋转不受 vanilla 动画影响 (替换模式), 位置/缩放叠加 |

`BoneState` 接口 (`config/BoneState.kt`):

```kotlin
@AllOpe
interface BoneState {
    fun lockVanilla(): Boolean = false
}
```

扩展新状态:
```kotlin
register("my_state", object : BoneState {
    override fun lockVanilla() = true
})
```

## animdata 数据文件

位置: `assets/<modid>/eyelib/animdata/<动画ID>.json`

文件名 (不含扩展名和路径) 作为动画 ID。

### 格式

```json
{
    "transition": 10,
    "bones": {
        "head": { "lock": true },
        "body": { "lock": true, "rot_x": false }
    },
    "timeline": {
        "0.0-1.0": {
            "bones": { "right_arm": { "lock": true } }
        }
    }
}
```

- `transition`: 可选, 默认 10 (tick 制, 0=即时, 20=1秒)
- `bones`: 动画全程生效的骨骼 flags
- `timeline`: 时间段骨骼 flags, key 格式 `"起始秒-结束秒"`

### 每轴过滤

| flag                            | 作用         |
|---------------------------------|------------|
| `pos_x`, `pos_y`, `pos_z`       | 位置轴 (默认启用) |
| `rot_x`, `rot_y`, `rot_z`       | 旋转轴 (默认启用) |
| `scale_x`, `scale_y`, `scale_z` | 缩放轴 (默认启用) |

## 坐标转换

eyelib 21.1.14 在 `BrAnimationEntry.tickAnimation()` 内部完成 Bedrock→Minecraft 转换:

- 位置: `div(16).mul(-1, 1, 1)` (像素→米制, X 取反)
- 旋转: `mul(DEGREES_TO_RADIANS).mul(-1, -1, 1)` (角度→弧度, X/Y 取反)

RCF 消费者侧不重复取反, 直接使用 eyelib 输出值。

## 测试命令

```
/test_anim <target> <anim_id>   触发动画
/test_anim <target> stop        停止动画
```

## 添加新动画

1. 创建资源文件:
    - `eyelib/animations/player/<name>.json` — 动画关键帧 (必需)
    - `eyelib/animdata/player/<name>.json` — 骨骼状态配置 (可选)
2. 动画由 AnimationManager 自动发现并加载
3. 测试: `/test_anim <player> <动画名>`
