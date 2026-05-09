# Resonator战斗框架(ResonatorCombatFramework)

<!-- TOC -->
* [Resonator战斗框架(ResonatorCombatFramework)](#resonator战斗框架resonatorcombatframework)
  * [文档](#文档)
  * [资源脚本路径](#资源脚本路径)
  * [资源类型](#资源类型)
<!-- TOC -->

## 文档

[Spark-Core源文档](https://deepwiki.com/lujunjiehhh/Spark-Core-HotReload)
请善用ai搜索功能

## 资源脚本路径

- 实际路径:
  run/spark_modules/{包名称}/{模组Id}/{资源类型}

- 开发路径:
  resources/spark_modules/{包名称}/{模组Id}/{资源类型}

## 资源类型

- SparkCore

| 目录名         | 描述    | Module            | 数据类                    | 支持格式       |
|-------------|-------|-------------------|------------------------|------------|
| ability/    | 技能    | AbilityTypeModule | AbilityTypeManager     | .json      |
| animations/ | 动画    | AnimationModule   | OAnimationSet          | .json      |
| controller/ | 动画控制器 | AnimStateModule   | OAnimStateMachineSet   | .json      |
| font/       | 字体    | FontModule        | SparkPackLoaderApplier | .ttf .json |
| lang/       | 语言文件  | LangModule        | SparkPackLoaderApplier | .json      |
| models/     | 模型    | ModelModule       | OModel                 | .json      |
| recipe/     | 配方    | RecipeModule      | SparkPackLoaderApplier | .json      |
| sounds/     | 音效    | SoundModule       | SparkPackLoaderApplier | .ogg       |
| textures/   | 纹理    | TextureModule     | SparkPackLoaderApplier | .png       |