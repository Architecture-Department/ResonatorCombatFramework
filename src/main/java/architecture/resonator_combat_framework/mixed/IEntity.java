package architecture.resonator_combat_framework.mixed;

import architecture.goldenboughs_lib.api.NoMixinException;
import architecture.resonator_combat_framework.api.AppurtenanceHost;
import architecture.resonator_combat_framework.api.appurtenance.AppurtenanceInfo;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface IEntity extends AppurtenanceHost {
	static IEntity of(Entity entity) {
		return entity;
	}

	@Override
	default @NotNull Map<@NotNull String, @NotNull AppurtenanceInfo<?>> getAppurtenanceInfoMap() {
		throw new NoMixinException();
	}
}
