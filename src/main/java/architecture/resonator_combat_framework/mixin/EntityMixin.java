package architecture.resonator_combat_framework.mixin;

import architecture.resonator_combat_framework.mixed.IEntityRcf;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntityRcf {
}
