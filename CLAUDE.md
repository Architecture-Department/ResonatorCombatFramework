# ResonatorCombatFramework (RCF)

## 项目结构

```
src/main/
├── java/   — Mixin 类（必须保持 Java，Kotlin 对 mixin 兼容性不好）
│   └── architecture/resonator_combat_framework/
│       ├── mixin/                           — 核心 mixin（EntityMixin, PlayerMixin）
│       │   └── gecko_lib/                   — GeckoLib 核心 mixin（AnimatableManager 相关）
│       └── module/player_animation/mixin/   — 玩家动画模块 mixin
│           ├── geckolib/                    — GeckoLib 渲染 mixin（MixinGeoRenderer, MixinGeoObjectRenderer）
│           └── gecko_lib/                   — GeckoLib 缓存 mixin（GeckoLibCacheMixin, AnimatableManagerMixin 等）
│
└── kotlin/ — 所有业务逻辑
    └── architecture/resonator_combat_framework/
        ├── api/           — 公开 API 接口
        ├── core/          — 核心类（Rcf, RcfClient, 注册表）
        ├── event/         — 事件类
        ├── events/        — 事件处理（错误拼写但保持）
        ├── init/          — 初始化/注册类
        ├── mixed/         — Mixin 接口（I-prefix）
        ├── module/        — 功能模块
        │   └── player_animation/  — 玩家动画模块
        │       ├── api/       — 模块接口
        │       ├── core/      — 核心代理/动画定义
        │       ├── event/     — 事件监听
        │       ├── helper/    — 辅助工具
        │       ├── init/      — 模块注册
        │       ├── model/     — 动画模型
        │       ├── payload/   — 网络包
        │       └── util/      — 工具类
        └── util/          — 通用工具
```

## 事件规则

- **`@EventBusSubscriber`**: 自动选择对应 bus，通常只需定义 `modid` 和 `value`（如 `value = [Dist.CLIENT]`）
- **事件分类**: 按事件的类分类（不是按作用），放在 `events/` 包下。公共事件放 `events/`，客户端专属放 `events/client/`
- **工具类模式**: 事件 subscriber 只做转发，业务逻辑放在工具类中（如 `PlayerAnimationSetup.refresh()`）

## 命名规范

- **接口**: `I` 前缀（如 `IPlayerRcf`, `IHasHoldAnim`, `IAppurtenance`）
- **工具类/Object**: 名词直接命名（如 `PlayerAnimationHelper`, `AnimationUtils`, `ItemCodecs`）
- **数据类**: 直接使用 `data class`，不加前缀
- **Mixin 唯一字段/方法**: 使用 `resonator_combat_framework$` 前缀。**必须保持此前缀不变**，否则混淆后会与其他 mod 冲突
- **包名**: 全小写 snake_case（`resonator_combat_framework`, `player_animation`）
- **常量/枚举值**: `UPPER_SNAKE_CASE`, 放在 `companion object` 或 `object` 中
- **静态方法调用**: 跨类调用必须带类名（`ClassName.method()`），无论 Kotlin 还是 Java
- **ID/常用值**: 使用静态变量引用（如 `Rcf.ID`），不硬编码字符串

## 注册体系

- **`Rcf.kt`**: 主 `@Mod` 入口，在 `init` 块中注册所有 DeferredRegister
- **`RcfClient.kt`**: 客户端侧注册
- **注册表**: 每个域有独立文件（`PayloadRegistry.kt`, `CapabilityRegistry.kt` 等），通过 `Rcf.modRegister()` 创建
- **网络包**: 实现 `CustomPacketPayload`，在 `PayloadRegistry` 中注册 TYPE + STREAM_CODEC

### 玩家动画注册示例（Rcf.kt）:

```kotlin
init {
  val modBus = MOD_BUS
  RcfDataComponentTypes.REGISTRY.register(modBus)
  PlayerAnimationAttachments.ATTACHMENT_TYPES.register(modBus)
}
```

## 玩家动画系统架构

### 动画引擎

使用 **eyelib** (TT432/eyelib) 作为动画计算引擎，不替换原版渲染。

### 数据流

```
物品/命令触发
  → PlayerAnimationHelper.requestPushAnimation(target, id)
  → AnimatePlayerPayload 双端网络包
  → Client: triggerPlayerAnimation() / Server: pushPlayerAnimation()
  → PlayerAnimationTransformer.trigger(id)
  → blendFactor 向 1.0 过渡
  → LivingEntityRendererMixin 在 renderToBuffer 前调用 applyTransform()
  → BrAnimator.tickAnimation() → BoneRenderInfos → 叠加到 ModelPart
```

### 核心类

| 类                            | 作用                                                   |
|------------------------------|------------------------------------------------------|
| `PlayerAnimationTransformer` | 每玩家实例：持有 RenderData，管理 blendFactor，动画偏移应用到 ModelPart |
| `PlayerAnimationHelper`      | 双端触发/停止：trigger / stop / push / request              |
| `AnimatePlayerPayload`       | `ToServerAndClientPayload` 双端网络包                     |
| `PlayerProxyProvider`        | Mixin 接口：`getAnimationTransformer()`                 |
| `LivingEntityRendererMixin`  | 注入 `render()`，调用 `applyTransform`                    |
| `PlayerMixin`                | 持有 `PlayerAnimationTransformer` `@Unique` 实例         |

### 动画 ID 注册

在 `PlayerAnimationTransformer.animationMap` 中映射 (ID → eyelib 动画名)：

```kotlin
private val animationMap = mapOf(
  "otsuchi_hold" to "animation.player.otsuchi_hold",
  "otsuchi_smash" to "animation.player.otsuchi_smash",
  "test_tekoki" to "animation.player.test_tekoki"
)
```

### 测试命令

```
/test_anim <target> <anim_id>    — 触发动画
/test_anim <target> stop         — 停止动画
```

### 资源位置

eyelib 21.1.14 资源路径以 `eyelib/` 为前缀：

```
assets/<modid>/eyelib/
├── animations/bedrock/    — 原始动画关键帧 JSON
├── animation_controllers/  — 动画控制器 JSON
├── bedrock_models/        — 模型几何体 JSON
└── textures/              — 纹理
```
