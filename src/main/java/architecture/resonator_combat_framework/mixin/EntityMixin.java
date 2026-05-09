package architecture.resonator_combat_framework.mixin;

import architecture.resonator_combat_framework.api.appurtenance.AppurtenanceInfo;
import architecture.resonator_combat_framework.mixed.IEntity;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntity {
	@Unique
	private final Map<String, AppurtenanceInfo<?>> resonator_combat_framework$appurtenanceInfoMap = new HashMap<>();

	@Override
	public @NotNull Map<@NotNull String, @NotNull AppurtenanceInfo<?>> getAppurtenanceInfoMap() {
		return resonator_combat_framework$appurtenanceInfoMap;
	}
}
