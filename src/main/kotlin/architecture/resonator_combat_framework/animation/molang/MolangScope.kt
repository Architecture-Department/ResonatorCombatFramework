package architecture.resonator_combat_framework.animation.molang

import java.util.function.DoubleSupplier

/**
 * MoLang 局部作用域。继承 MolangData 以直接作为表达式求值的 context。
 * `temp.*` 变量写入局部层，`v.*` 变量写入全局层。
 * 表达式求值后可通过 getLocal() 读取临时变量。
 */
class MolangScope(private val global: MolangData) : MolangData() {

	private val locals = HashMap<String, Double>()

	/** 初始化局部变量 */
	fun setLocal(name: String, value: Double) {
		locals[name] = value
	}

	/** 读取局部变量（表达式求值后读取临时结果） */
	fun getLocal(name: String): Double? = locals[name]

	override fun resolveLocal(name: String): Double? = locals[name]

	override fun resolve(name: String): Double {
		resolveLocal(name)?.let { return it }
		return global.resolve(name)
	}

	override fun assign(name: String, value: Double) {
		if (assignLocal(name, value)) return
		// v.* 变量写入全局层
		if (name.startsWith("v.")) {
			global.assign(name, value)
			return
		}
		super.assign(name, value)
	}

	override fun assign(name: String, value: DoubleSupplier) {
		assign(name, value.asDouble)
	}
}

/** 在 MolangData 上创建作用域并执行表达式 */
fun <T> MolangData.withScope(block: (MolangScope) -> T): T {
	val scope = MolangScope(this)
	return block(scope)
}
