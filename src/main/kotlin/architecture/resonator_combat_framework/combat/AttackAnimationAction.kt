package architecture.resonator_combat_framework.combat

import architecture.resonator_combat_framework.animation.AttackAnimation
import architecture.resonator_combat_framework.init.AnimationPropertys
import architecture.resonator_combat_framework.module.entity_state_machine.combat.InterruptData
import architecture.resonator_combat_framework.module.entity_state_machine.combat.StageTiming
import net.minecraft.resources.ResourceLocation
import java.util.function.Supplier

class AttackAnimationAction<T : AttackAnimation>(
	id: ResourceLocation,
	animation: Supplier<T?>,
	controllerName: ResourceLocation?,
	timing: StageTiming,
	interruptData: InterruptData = InterruptData(),
	weight: Int = 2500,
	val damageMultiplier: Float = 1.0f
) : AnimationAction<T>(id, animation, controllerName, timing, interruptData, weight) {
	override fun getAnimation(): T {
		val animation = super.getAnimation()
		animation.addProperty(AnimationPropertys.DAMAGE_MULTIPLIER, damageMultiplier)
		return animation
	}
}