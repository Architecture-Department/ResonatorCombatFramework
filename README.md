# Resonator战斗框架(ResonatorCombatFramework)

<!-- TOC -->
* [Resonator战斗框架(ResonatorCombatFramework)](#resonator战斗框架resonatorcombatframework)
  * [文档](#文档)
  * [资源类型](#资源类型)
<!-- TOC -->

## 文档

[Spark-Core源文档](https://deepwiki.com/lujunjiehhh/Spark-Core-HotReload)
请善用ai搜索功能

- 资源
- [资源创建与管理指南.md](docs/资源/resource_creation_guide.md)
- [注册新的资源类型.md](docs/资源/register_new_resource_type.md)

## 资源类型
 
- SparkCore

| 目录名             | Handler             | 注册表                               | 数据类              | 支持格式                      |
|-----------------|---------------------|-----------------------------------|------------------|---------------------------|
| animations/     | AnimationHandler    | SparkRegistries.TYPED_ANIMATION   | TypedAnimation   | .json                     |
| models/         | ModelHandler        | SparkRegistries.MODELS            | OModel           | .json                     |
| textures/       | TextureHandler      | SparkRegistries.DYNAMIC_TEXTURES  | OTexture         | .png .jpg .jpeg .tga .bmp |
| scripts/        | JavaScriptHandler   | SparkRegistries.JS_SCRIPTS        | OJSScript        | .js                       |
| ik_constraints/ | IKConstraintHandler | SparkRegistries.IK_COMPONENT_TYPE | TypedIKComponent | .json                     |
| (元数据)           | MetaHandler         | 无独立注册表                            | 			—	            | -                         | -                         

