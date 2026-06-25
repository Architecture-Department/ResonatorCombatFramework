package architecture.resonator_combat_framework.module.collision.event

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event

/**
 * 碰撞命中事件 —— 当 [architecture.resonator_combat_framework.module.collision.CollisionSystem] 检测到一个 collider 与实体重叠时触发。
 *
 * 监听此事件的系统决定如何处理命中（伤害、击退、眩晕、状态机交互等），
 * 碰撞模块本身不处理任何游戏逻辑。
 *
 * @property attacker  碰撞源（持有 collider 的实体）
 * @property colliderId 命中的 collider 唯一标识
 * @property target    被命中的实体
 */
data class CollisionHitEvent(
	val attacker: Entity,
	val colliderId: ResourceLocation,
	val target: Entity,
) : Event()