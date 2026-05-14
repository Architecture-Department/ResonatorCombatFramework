package architecture.resonator_combat_framework.mixin;

import architecture.resonator_combat_framework.mixed.IPlayerRcf;
import architecture.resonator_combat_framework.module.player_animation.GeoPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public abstract class PlayerMixin implements IPlayerRcf {
	@Unique
	private final Player resonator_combat_framework$player = (Player) (Object) this;

	@Unique
	private final GeoPlayer resonator_combat_framework$proxy = new GeoPlayer(resonator_combat_framework$player);

	@Override
	public GeoPlayer resonator_combat_framework$getAnimationGeoPlayer() {
		return resonator_combat_framework$proxy;
	}
}
