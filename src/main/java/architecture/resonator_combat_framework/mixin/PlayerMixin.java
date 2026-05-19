package architecture.resonator_combat_framework.mixin;

import architecture.resonator_combat_framework.mixed.IPlayerRcf;
import architecture.resonator_combat_framework.module.player_animation.core.PlayerAnimationTransformer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements IPlayerRcf {
	@Unique
	private PlayerAnimationTransformer resonator_combat_framework$transformer;

	protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
		super(entityType, level);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	public void resonator_combat_framework$init(Level level, BlockPos pos, float yRot, GameProfile gameProfile, CallbackInfo ci) {
		resonator_combat_framework$transformer = new PlayerAnimationTransformer((Player) (Object) this);
	}

	@Override
	public @NotNull PlayerAnimationTransformer resonator_combat_framework$getAnimationTransformer() {
		return resonator_combat_framework$transformer;
	}
}
