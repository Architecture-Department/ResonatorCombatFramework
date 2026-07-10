/**
 * Entity Mixin —— 向所有 [Entity] 注入 RCF 扩展接口 [IEntityRcf]。
 * 使所有实体对象均能通过 [IEntityRcf.of] 进行 RCF 相关操作。
 */
package architecture.resonator_combat_framework.mixin;

import architecture.resonator_combat_framework.mixed.IEntityRcf;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntityRcf {
}
