package architecture.resonator_combat_framework.module.player_animation.mixin.eye_lib;

import io.github.tt432.eyelib.client.manager.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ModelManager.class)
public interface ModelManagerAccessor {
	@Invoker("<init>")
	static ModelManager newModelManager() {
		throw new AssertionError();
	}
}
