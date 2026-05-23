package architecture.resonator_combat_framework.module.player_animation.mixin.eye_lib;

import io.github.tt432.eyelib.client.manager.AnimationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AnimationManager.class)
public interface AnimationManagerAccessor {

	@Invoker("<init>")
	static AnimationManager newAnimationManager() {
		throw new AssertionError();
	}
}
