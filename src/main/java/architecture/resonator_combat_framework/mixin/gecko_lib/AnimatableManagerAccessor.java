package architecture.resonator_combat_framework.mixin.gecko_lib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import software.bernie.geckolib.animation.AnimatableManager;

@Mixin(AnimatableManager.class)
public interface AnimatableManagerAccessor {
	@Invoker(remap = false)
	void callFinishFirstTick();
}