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

使用 **eyelib 21.1.14** (TT432/eyelib) 作为动画计算引擎，不替换原版渲染。
`AnimationComponent.setup()` 直接注册 `BrAnimationEntry` / `BrAnimationController`，原生支持多动画并发和状态机。

### 数据流

```
物品/命令触发
  → PlayerAnimationHelper.trigger/stop (通过 IPlayerAnimator)
  → AnimatePlayerPayload 双端网络包
  → PlayerAnimationTransformer.trigger(animId)
  → 过渡: blendFactor lerp (由 currentTransitionTicks 控制速度)
  → LivingEntityRendererMixin.applyTransform()
  → BrAnimator.tickAnimation() → BoneRenderInfos
  → applyBone() → 叠加到 ModelPart
```

### 核心类

| 类                                   | 作用                                           |
|-------------------------------------|----------------------------------------------|
| `IAnimationMapper` (`api/`)         | 根接口: 生命周期+控制器管理+骨骼冲突                         |
| `EntityAnimationMapper<T, M>`       | 控制器注册+tick/trigger/stop+root PoseStack       |
| `HumanoidEntityAnimationMapper`     | 6人形骨骼 applyProxyBone + 物品 applyProxyToItem   |
| `PlayerAnimationMapper`             | EyeLibAnimationController+渲染入口+jacket/sleeve |
| `BaseAnimationController`           | 状态机+过渡+crossfade(含shouldBlend检查)             |
| `EyeLibAnimationController`         | eyelib 后端适配                                  |
| `AnimationPlayConfig`               | 播放配置: 类型/时间/速率/淡入淡出/骨骼配置(Builder)            |
| `ProxyBoneFlags`                    | 骨骼标志: Map存储+扩展函数 per-axis lock/enable        |
| `ProxyBoneConfigData`               | 骨骼配置容器: bones+timeline+transitionTicks       |
| `PlayerAnimationHelper` (`helper/`) | 双端便捷: trigger/stop/pause/resume              |
| `AnimatePlayerPayload` (`payload/`) | 双端网络包(PLAY/STOP/PAUSE/RESUME)                |
| `PlayerProxyProvider` (`mixed/`)    | Mixin 接口，返回 IAnimationMapper                 |
| `LivingEntityRendererMixin`         | 注入渲染，调用 tickAndRender                        |
| `ItemInHandLayerMixin`              | 注入物品渲染，调用 applyItemTransform                 |

### 过渡系统

- 淡入淡出分离: `AnimationPlayConfig.fadeInTicks` / `fadeOutTicks` 各自独立默认值
- Crossfade: `BaseAnimationController` 自动快照旧帧 → lerp 到新帧
- `ProxyBoneFlags.shouldBlend()` 控制每骨骼是否参与 crossfade (默认 true)
- `ProxyBoneFlags.shouldTransition()` 控制每骨骼是否参与 weight (默认 true)

### 骨骼配置 JSON 格式

嵌套 JSON → 加载时打平为 dot-notation key:

```json
{ "head": { "pos": { "lock": true, "x": false }, "blend": false } }
  → ProxyBoneFlags({ "pos.lock":true, "pos.x":false, "blend":false })
```

### 测试命令

```
/test_anim <target> <anim_id>    — 触发动画
/test_anim_stop <target>         — 停止动画
```

### 资源位置

eyelib 21.1.14 资源路径以 `eyelib/` 为前缀：

```
assets/<modid>/eyelib/
├── animations/player/     — Bedrock 动画关键帧 JSON
├── animation_controllers/ — 动画控制器 JSON
├── animdata/player/       — RCF 骨骼状态配置 JSON
├── bedrock_models/        — 模型几何体 JSON
└── textures/              — 纹理
```

