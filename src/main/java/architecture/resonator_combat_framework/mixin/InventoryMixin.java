package architecture.resonator_combat_framework.mixin;

import architecture.resonator_combat_framework.event.PlayerHotbarChangeEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截玩家热键栏槽位切换，触发 [PlayerHotbarChangeEvent]。
 * 若事件被取消则不执行切换。
 */
@Mixin(Inventory.class)
public class InventoryMixin {
    @Shadow @Final
    public Player player;

    @Inject(at = @At("HEAD"), method = "setSelectedSlot", cancellable = true)
    private void rcf$onSetSelectedSlot(int slot, CallbackInfo ci) {
        Inventory self = (Inventory) (Object) this;
        int fromSlot = self.selected;
        if (fromSlot == slot) return;  // 相同槽位，不触发

        ItemStack fromStack = self.getItem(fromSlot);
        ItemStack toStack = self.getItem(slot);

        PlayerHotbarChangeEvent event = new PlayerHotbarChangeEvent(
                player, fromSlot, slot, fromStack, toStack
        );
        NeoForge.EVENT_BUS.post(event);

        if (event.isCanceled()) {
            ci.cancel();
        }
    }
}
