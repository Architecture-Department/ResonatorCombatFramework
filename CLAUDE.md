# ResonatorCombatFramework (RCF)

Mod ID: `resonator_combat_framework`

战斗框架模块。提供碰撞检测、自定义实体事件、实体动画系统和装备渲染支持。

## 包结构

- `core/` — `Rcf.kt`(@Mod), `RcfClient.kt`
- `config/` — `RcfConfig`, `RcfClientConfig`
- `init/` — 注册初始化（AttachmentTypes, DataComponentTypes, Capabilitys）
- `event/` — 自定义事件（`AddGeckoLibCachePathEvent`）
- `events/` — 事件监听器 + 注册中心（Entity, Player, Level, Renderer 事件；AnimationController, Capability, Command,
  Payload, ResourceReload 注册）
- `mixed/` — Mixin 接口（`IEntityRcf`, `IPlayerRcf`）
- `common/payload/` — 网络包（`AppurtenanceSynchroPayload`）
- `util/` — 工具类（`RcfUtil`）
- `module/entity_animation/` — 玩家动画模块
  - `animation/` — 核心动画引擎（IEntityAnimationController, BedrockAnimationController, AnimationControllerManager,
    EntityAnimationMapper→PlayerAnimationMapper, BrModel, ProxyModel, MoLang 解析引擎）
  - `command/` — 测试动画命令
  - `event/` — 动画事件（ControllerEvent, RegisterEvent, AnimationEvent, ParticleEvent）
  - `helper/` — 双端便捷入口（PlayerAnimationHelper）
  - `mixed/` — 动画 Mixin 接口（IAnimationProxyProvider）
  - `network/` — 动画同步网络包（Trigger/Stop/Pause/Resume）
  - `registry/` — 注册中心（BedrockAnimation, BedrockModel, ProxyBoneConfigData）
  - `render/` — 第一人称渲染
  - `util/` — 骨骼变换工具、GeckoLib 工具
- `module/collision/` — 碰撞检测系统
- `mixin/java/` — Java Mixin 类（EntityMixin, PlayerMixin, GeckoLibCache, 动画客户端 Mixin）

## 关键系统

| 系统   | 说明                                         |
|------|--------------------------------------------|
| 实体动画 | 自研 Bedrock 动画引擎，支持跨动画过渡、MoLang 表达式、粒子/音效事件 |
| 碰撞检测 | 自定义碰撞逻辑                                    |
| 网络同步 | 动画触发/停止/暂停/恢复的双端网络同步                       |

## 依赖

- **GoldenBoughsLib** — Mixin 接口、网络包工具

由 ImaginaryCraft 模块 jarJar 聚合。
