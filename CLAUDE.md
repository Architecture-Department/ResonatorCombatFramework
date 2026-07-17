# ResonatorCombatFramework (RCF)

Mod ID: `resonator_combat_framework`

战斗框架模块。提供碰撞检测、自定义实体事件、实体动画系统和装备渲染支持。

## 包结构

- `core/` — `Rcf.kt`(@Mod), `RcfClient.kt`
- `config/` — `RcfConfig`, `RcfClientConfig`
- `init/` — 注册初始化（AttachmentTypes, DataComponentTypes, Capabilitys）
- `event/definition/` — 自定义事件定义（ActionEvent, TriggerEvent, ParticleEvent 等）
- `event/listener/` — 事件监听器（Entity, Player, Level, Renderer 等 NeoForge 事件处理）
- `mixed/` — Mixin 接口（`IEntityRcf`, `IPlayerRcf`）
- `common/payload/` — 网络包（`AppurtenanceSynchroPayload`）
- `util/` — 工具类（`RcfUtil`）
- `animation/` — 核心动画引擎（控制器、映射器、关键帧动画、骨骼/几何模型）
- `molang/` — 独立 MoLang 表达式引擎（解析器、求值器、22 个内置函数）
- `combat/` — 战斗动作系统（Action, ActionController, AttackAnimationAction）
- `collision/` — 碰撞检测（OBBCollider, SAT, MultiCollider）
- `command/` — 测试动画命令
- `mixed/` — Mixin 接口（IEntityRcf, IPlayerRcf）
- `model/` — 运行时骨骼/几何模型
- `payload/` — 网络包（动画触发/停止/同步）
- `registry/` — 资源注册表（动画、模型、骨骼配置）
- `state_machine/holder/` — 实体状态容器
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
