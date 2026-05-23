# eyelib 使用文档

## 概述

eyelib (https://github.com/TT432/eyelib) 是一个 Minecraft Bedrock 动画引擎，支持 Molang 表达式、动画控制器、渲染控制器。本模块使用
eyelib 21.1.14 仅做动画计算（不替换原版渲染）。

## 资源路径

eyelib 21.1.14 的资源路径以 `eyelib/` 为前缀：

```
assets/<modid>/eyelib/
├── animations/bedrock/       ← 动画关键帧 (.json)
├── animation_controllers/    ← 动画控制器 (.json)
├── bedrock_models/           ← 模型几何体 (.json)
├── render_controllers/       ← 渲染控制器 (.json, 可选)
├── textures/                 ← 纹理 (.png)
└── animdata/                 ← 骨骼状态配置 (.json, 本模块自定义)
```

## 核心 API

### RenderData

```kotlin
val renderData = RenderData.getComponent<Player>(player)
renderData.isUseBuiltInRenderSystem = false  // 不替换原版渲染
```

### AnimationComponent

```kotlin
val ac = renderData.animationComponent
ac.setup(
    mapOf("anim.name" to "anim.name"),      // 动画名 → 动画名 (AnimationManager key)
    mapOf("anim.name" to MolangValue.ONE)    // 动画名 → 倍率
)
```

### BrAnimator

```kotlin
val infos = BrAnimator.tickAnimation(
    ac, scope, AnimationEffects(), ticks, {}
)
// infos.getData(boneId).renderPosition  → Vector3f 像素
// infos.getData(boneId).renderRotation  → Vector3f 弧度
```

### GlobalBoneIdHandler

```kotlin
val boneId = GlobalBoneIdHandler.get("left_arm")  // 骨骼名 → int ID
```

### AnimationManager

```kotlin
val allAnims = EyeLibUtil.getAnimationManager().getAllData()  // 所有已加载动画
```

### BrClientEntity

实体定义，串联 model + animation + controller + render_controllers：

```kotlin
val scripts = BrClientEntityScripts(
    MolangValue.ZERO, MolangValue.ZERO,
    mapOf("anim" to MolangValue.ONE),
    MolangValue.ONE,
    Optional.empty(), Optional.empty(), Optional.empty()
)
val entity = BrClientEntity(
    "minecraft:player",
    mapOf("default" to "entity_alphatest"),
    mapOf("default" to "textures/geo/empty"),
    mapOf("default" to "resonator_combat_framework:player_proxy"),
    mapOf("anim" to "resonator_combat_framework:player_proxy"),
    mapOf(), mapOf(), listOf(),
    Optional.of(scripts)
)
Eyelib.getClientEntityLoader().put(ResourceLocation("minecraft:player"), entity)
```

## 动画格式

### 动画关键帧 (Bedrock 1.8.0)

```json
{
    "format_version": "1.8.0",
    "animations": {
        "animation.player.my_anim": {
            "loop": true,
            "animation_length": 4,
            "bones": {
                "left_arm": {
                    "rotation": {
                        "0.0": [45.0, 0.0, 0.0],
                        "2.0": [90.0, 0.0, 0.0],
                        "4.0": [45.0, 0.0, 0.0]
                    }
                }
            }
        }
    }
}
```

### 模型格式 (Bedrock 1.12.0)

```json
{
    "format_version": "1.12.0",
    "minecraft:geometry": [{
        "description": {
            "identifier": "geometry.player_proxy",
            "texture_width": 64,
            "texture_height": 64,
            "visible_bounds_width": 4,
            "visible_bounds_height": 3,
            "visible_bounds_offset": [0, 0.5, 0]
        },
        "bones": [
            {"name": "head", "pivot": [0, 24, 0], "rotation": [0, 0, 0]},
            {"name": "body", "pivot": [0, 24, 0], "rotation": [0, 0, 0]},
            {"name": "left_arm", "pivot": [5, 22, 0], "rotation": [0, 0, 0], "cubes": [...]},
            {"name": "right_arm", "pivot": [-5, 22, 0], "rotation": [0, 0, 0], "cubes": [...]},
            {"name": "left_leg", "pivot": [1.9, 12, 0], "rotation": [0, 0, 0], "cubes": [...]},
            {"name": "right_leg", "pivot": [-1.9, 12, 0], "rotation": [0, 0, 0], "cubes": [...]}
        ]
    }]
}
```

### 动画控制器 (Bedrock 1.19.0)

```json
{
    "format_version": "1.19.0",
    "animation_controllers": {
        "controller.animation.player_proxy": {
            "initial_state": "idle",
            "states": {
                "idle": {
                    "animations": ["animation.player.otsuchi_hold"]
                }
            }
        }
    }
}
```

## 版本差异

| 版本      | setup()          | tickAnimation() | 资源路径         |
|---------|------------------|-----------------|--------------|
| 21.1.0  | `setup(RL, RL)`  | 3 参数            | 直接路径         |
| 21.1.10 | `setup(Map,Map)` | 4 参数            | 直接路径         |
| 21.1.14 | `setup(Map,Map)` | 5 参数            | `eyelib/` 前缀 |

## 关键注意事项

1. 资源必须在 `eyelib/` 子目录下
2. `BrAnimator.tickAnimation()` 的 `ticks` 参数为**秒**（`tickCount / 20`）
3. `renderRotation` 返回弧度，不需要 `Math.toRadians()` 二次转换
4. `AnimationManager` key 是动画 JSON 内部的动画名（如 `animation.player.xxx`）
5. 模型加载顺序先于动画，否则骨骼映射丢失
