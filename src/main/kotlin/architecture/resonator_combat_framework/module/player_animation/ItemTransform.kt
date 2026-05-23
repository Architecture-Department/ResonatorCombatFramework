package architecture.resonator_combat_framework.module.player_animation

/** eyelib 虚拟骨骼 "right_item" / "left_item" 的动画变换，由 ItemInHandLayerMixin 读取 */
data class ItemTransform(
	var posX: Float = 0f,
	var posY: Float = 0f,
	var posZ: Float = 0f,
	var rotX: Float = 0f,
	var rotY: Float = 0f,
	var rotZ: Float = 0f,
	var scaleX: Float = 1f,
	var scaleY: Float = 1f,
	var scaleZ: Float = 1f
)
