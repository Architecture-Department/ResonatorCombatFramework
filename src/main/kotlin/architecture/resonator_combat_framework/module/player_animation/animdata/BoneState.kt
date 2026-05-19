package architecture.resonator_combat_framework.module.player_animation.animdata

// 骨骼状态注册表：可扩展添加新状态
object BoneStateRegistry {
	private val states = mutableMapOf<String, BoneState>()

	init {
		register("lock", LockState)
		register("no_fade_in", NoFadeInState)
		register("no_fade_out", NoFadeOutState)
	}

	fun register(name: String, state: BoneState) {
		states[name] = state
	}

	fun get(name: String): BoneState? = states[name]
}

// 骨骼状态接口：影响该骨骼的 ModelPart 变换行为
interface BoneState {
	// root: 动画活跃期间始终生效  timeline: 特定时间区间生效
	enum class Mode { ROOT, TIMELINE }

	val mode: Mode

	// blend out 速度覆盖（null = 不变）
	fun blendSpeedOverride(): Float? = null

	// 是否锁定骨骼不受 vanilla 动画影响
	fun lockVanilla(): Boolean = false
}

// 锁定：骨骼不受 vanilla 动画影响
object LockState : BoneState {
	override val mode = BoneState.Mode.ROOT
	override fun lockVanilla() = true
}

// 无淡入：跳过 blend in
object NoFadeInState : BoneState {
	override val mode = BoneState.Mode.ROOT
	override fun blendSpeedOverride() = 1f
}

// 无淡出：跳过 blend out
object NoFadeOutState : BoneState {
	override val mode = BoneState.Mode.ROOT
	override fun blendSpeedOverride() = 1f
}
