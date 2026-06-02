# ResonatorCombatFramework (RCF)

## 项目结构

```
src/main/
├── java/   — Mixin 类（必须保持 Java，Kotlin 对 mixin 兼容性不好）
│   └── architecture/resonator_combat_framework/
│       ├── mixin/                           — 核心 mixin（EntityMixin, PlayerMixin）
│       │   └── gecko_lib/                   — GeckoLib 核心 mixin（AnimatableManager 相关）
│       └── module/entity_animation/mixin/   — 玩家动画模块 mixin
│           ├── client/                      — LivingEntityRendererMixin, ItemInHandLayerMixin
│           └── gecko_lib/                   — GeckoLib 缓存/渲染 mixin
│
└── kotlin/ — 所有业务逻辑
    └── architecture/resonator_combat_framework/
        ├── api/           — 公开 API 接口
        ├── core/          — 核心类（Rcf, RcfClient, RcfConstants, 注册表）
        ├── event/         — 自定义事件类（AnimationControllerRegisterEvent 等）
        ├── events/        — 事件监听器
        │   └── registry/  — AnimationControllerRegistry 等
        ├── init/          — 初始化/注册类
        ├── mixed/         — Mixin 接口（IPlayerRcf）
        ├── module/entity_animation/  — 玩家动画模块
        │   ├── api/       — IAnimationMapper, ProxyBone, ProxyModel
        │   ├── bedrock/   — BedrockAnimation, BedrockAnimator（插值引擎）
        │   │   └── molang/— MolangQueries, EasingTypes, MathParser
        │   ├── command/   — TestAnimCommand
        │   ├── config/    — AnimationPlayData, ProxyBoneFlags, ProxyBoneConfigData
        │   ├── controller/— IAnimationController, BaseAnimationController,
        │   │                BedrockAnimationController, ControllerManager
        │   ├── helper/    — PlayerAnimationHelper（双端便捷入口）
        │   ├── mapper/    — EntityAnimationMapper → LivingEntity → HumanoidEntity
        │   │                → PlayerAnimationMapper
        │   ├── mixed/     — PlayerProxyProvider（Mixin 接口）
        │   ├── payload/   — PlayPlayerPayload / StopPlayerPayload / PausePlayerPayload / ResumePlayerPayload
        │   ├── registry/  — ProxyBoneConfigRegistry, BedrockAnimationRegistry
        │   └── util/      — BoneTransformUtil
        └── util/          — 通用工具
```

## 事件规则

- **`@EventBusSubscriber`**: 自动选择对应 bus，通常只需定义 `modid` 和 `value`（如 `value = [Dist.CLIENT]`）
- **事件分类**: 按事件的类分类（不是按作用），放在 `events/` 包下
- **工具类模式**: 事件 subscriber 只做转发，业务逻辑放在工具类中

## 命名规范

- **接口**: `I` 前缀（如 `IPlayerRcf`, `IAnimationController`, `IAnimationMapper`）
- **Mixin 唯一字段/方法**: 使用 `resonator_combat_framework$` 前缀
- **包名**: 全小写 snake_case
- **常量/枚举值**: `UPPER_SNAKE_CASE`
- **ID/常用值**: 使用静态变量引用（如 `RcfConstants.ID`），不硬编码

## 注册体系

- **`Rcf.kt`**: 主 `@Mod` 入口，`init` 块中注册所有 DeferredRegister
- **`RcfClient.kt`**: 客户端侧注册
- **网络包**: 实现 `CustomPacketPayload`，在 `PayloadRegistry` 中注册

## 玩家动画系统

### 引擎

使用自研 `BedrockAnimator` 引擎，支持 Bedrock 1.8.0 格式动画关键帧：

- LINEAR / CATMULLROM / STEP 三种插值模式
- `pos`、`rotation`、`scale` 独立插值
- Molang 表达式支持（`query.anim_time`、`query.delta_time`）

### 继承链

```
IAnimationMapper
  ↑
EntityAnimationMapper<T:Entity, M>      — ControllerManager + tick/trigger/stop + root PoseStack
  ↑
LivingEntityAnimationMapper<T, M>       — resolveBoneFlags
  ↑
HumanoidEntityAnimationMapper<T, M>     — 6人形骨骼 applyProxyBone + 物品 applyProxyToItem
  ↑
PlayerAnimationMapper                   — init注册控制器 + tickAndRender 入口

IAnimationController
  ↑
BaseAnimationController                 — 状态机(IDLE/TRANSITIONING/PLAYING/PAUSED/FADING_OUT) + crossfade
  ↑
BedrockAnimationController              — BedrockAnimator 后端适配
```

### 数据流

```
触发:
PlayerAnimationHelper.triggerPlayerAnimation(config)
  → 客户端: clientTrigger → IAnimationMapper.trigger(playData)
  → 服务端: serverTrigger + PlayPlayerPayload 广播
    → EntityAnimationMapper.trigger(playData)
      → controllerManager.get(controllerName) → controller
      → controller.boneConfigs = used
      → controller.trigger(playData)
        → loadAnimation → snapshotTransitionSource → 清空 proxyModel
        → state = TRANSITIONING → blendFactor 0→1
        → freezeAllAtFrameZero + rebuildBackend

每帧渲染:
PlayerAnimationMapper.tickAndRender(model, partialTick, poseStack)
  → tick() → 每个控制器.tick(deltaSec)
    → tickBlend → 过渡渐变
    → 清除不属于 affectedBones 的旧骨骼
    → checkPlaybackBounds → 动画结束处理
    → tickBackend → BedrockAnimator.computeAndWrite → proxyModel
    → crossfadeStep → 逐骨骼lerp(transitionSource, proxyModel, blendFactor)
  → controllerManager.getRenderable() → 可渲染控制器列表
  → 逐控制器: resolveBoneFlags + effectiveWeight → applyProxyToModel
```

### 多控制器管理

`ControllerManager` 双集合存储（Map O(1) + List 顺序），由 `PlayerAnimationMapper.init` 通过
`AnimationControllerRegisterEvent` 注册。

预置控制器（在 `AnimationControllerRegistry` 中定义）：

| 名称      | Priority | 角色    |
|---------|----------|-------|
| ACTION  | 1000     | 动作层   |
| MAIN    | 0        | 默认控制器 |
| COMMAND | -1000    | 命令层   |

`getRenderable()` 按优先级遍历，`isOverriding` 允许低优先级控制器覆盖高优先级控制器的活跃骨骼。

### 状态机

```
IDLE → trigger → TRANSITIONING → blendFactor→1 → PLAYING → checkPlaybackBounds
                                                                  ↓
                                                         FADING_OUT → blendFactor→0 → IDLE
PLAYING/TRANSITIONING → pause → PAUSED → resume → TRANSITIONING/PLAYING
```

### 骨骼配置 JSON

```
assets/<modid>/rcf/animdatas/<anim_id>.json

{ "bones": { "head": { "lock": false } }, "transition": 3 }
```

- `transition`（顶层）: 淡入淡出 tick 数，默认 3
- `bones`: 骨骼名 → ProxyBoneFlags
- `timeline`: 时间段覆盖 `"0.0-1.0": { "bones": {...} }`

ProxyBoneFlags key: `lock`, `blend`, `transition`, `pos.x/y/z`, `pos.lock`, `rot.*`, `scale.*`
