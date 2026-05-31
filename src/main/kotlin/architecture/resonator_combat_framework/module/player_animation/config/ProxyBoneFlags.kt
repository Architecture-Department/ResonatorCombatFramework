package architecture.resonator_combat_framework.module.player_animation.config

import architecture.goldenboughs_lib.api.AllOpe

/**
 * 骨骼标志配置 — 内部用 Map<String, Boolean> 存储，第三方可通过自定义 key 扩展。
 *
 * 内置 key（用 . 分层）：
 * lock           — 锁定整个骨骼
 * blend          — 是否参与 crossfade
 * transition     — 是否参与淡入淡出
 * pos.x          — 位置 X 轴启用
 * pos.y          — 位置 Y 轴启用
 * pos.z          — 位置 Z 轴启用
 * pos.lock       — 锁定位置
 * rot.x          — 旋转 X 轴启用
 * rot.y          — 旋转 Y 轴启用
 * rot.z          — 旋转 Z 轴启用
 * rot.lock       — 锁定旋转
 * scale.x        — 缩放 X 轴启用
 * scale.y        — 缩放 Y 轴启用
 * scale.z        — 缩放 Z 轴启用
 * scale.lock     — 锁定缩放
 */
@AllOpe
data class ProxyBoneFlags(
	val flags: Map<String, Boolean> = emptyMap()
) {
	/** 第三方可直接读取 flags 中的 key */
	operator fun get(key: String): Boolean? = flags[key]
}

// ═══════════════ Lock 控制（nullable receiver，null 时默认 true） ═══════════════

fun ProxyBoneFlags?.lockVanilla(): Boolean = this?.flags?.get("lock") ?: true
fun ProxyBoneFlags?.lockPos(): Boolean = lockVanilla() || (this?.flags?.get("pos.lock") ?: true)
fun ProxyBoneFlags?.lockRotation(): Boolean = lockVanilla() || (this?.flags?.get("rot.lock") ?: true)
fun ProxyBoneFlags?.lockScale(): Boolean = lockVanilla() || (this?.flags?.get("scale.lock") ?: true)
fun ProxyBoneFlags?.hasAnyLockState(): Boolean = lockPos() || lockRotation() || lockScale()

// ═══════════════ 过渡控制（nullable receiver，null 时默认 true） ═══════════════

fun ProxyBoneFlags?.shouldBlend(): Boolean = this?.flags?.get("blend") ?: true
fun ProxyBoneFlags?.shouldTransition(): Boolean = this?.flags?.get("transition") ?: true

// ═══════════════ 轴启用控制（nullable receiver，null 时默认 true） ═══════════════

fun ProxyBoneFlags?.isEnabled(axis: String): Boolean = this?.flags?.get(axis) ?: true
