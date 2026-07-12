/**
 * Camera Accessor —— 暴露 [Camera] 的私有 detached 字段。
 * 用于在动画播放时控制相机是否脱离玩家（如过场动画）。
 */
package architecture.resonator_combat_framework.mixin.client;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAccessor {
	@Accessor
	void setDetached(boolean detached);
}

