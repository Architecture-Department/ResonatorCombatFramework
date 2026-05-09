package architecture.resonator_combat_framework.mixed;

import architecture.goldenboughs_lib.api.NoMixinException;
import architecture.resonator_combat_framework.api.AppurtenanceHost;
import architecture.resonator_combat_framework.api.appurtenance.AppurtenanceInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface IEntity extends AppurtenanceHost {
	@Override
	default @NotNull Map<@NotNull String, @NotNull AppurtenanceInfo<?>> getAppurtenanceInfoMap() {
		throw new NoMixinException();
	}
}
