/**
 * Player Mixin —— 向 [Player] 注入 RCF 动画提供接口 [IPlayerRcf]。
 * 在玩家构造时创建 [PlayerAnimationMapperProvider]，使玩家实体支持动画系统。
 */
package architecture.resonator_combat_framework.mixin;

import architecture.resonator_combat_framework.mixed.IPlayerRcf;
import architecture.resonator_combat_framework.module.animation.mapper.IEntityAnimationMapperProvider;
import architecture.resonator_combat_framework.module.animation.mapper.PlayerAnimationMapperProvider;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.model.PlayerModel;
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
	private PlayerAnimationMapperProvider resonator_combat_framework$transformer;

	protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
		super(entityType, level);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	public void resonator_combat_framework$init(Level level, BlockPos pos, float yRot, GameProfile gameProfile, CallbackInfo ci) {
		resonator_combat_framework$transformer = new PlayerAnimationMapperProvider((Player) (Object) this);
	}

	@Override
	public @NotNull IEntityAnimationMapperProvider<Player, PlayerModel<Player>> resonator_combat_framework$getMapperProvider() {
		return resonator_combat_framework$transformer;
	}
}