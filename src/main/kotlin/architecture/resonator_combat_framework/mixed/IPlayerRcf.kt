package architecture.resonator_combat_framework.mixed

import architecture.resonator_combat_framework.animation.IAnimationProvider

/**
 * RCF 玩家扩展接口 —— 通过 mixin 注入到玩家实体。
 * 继承 [IAnimationProvider]，使玩家实体支持 RCF 动画系统。
 */
interface IPlayerRcf : IAnimationProvider
