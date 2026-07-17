package architecture.resonator_combat_framework.molang.value

import architecture.resonator_combat_framework.molang.MolangDataHolder
import architecture.resonator_combat_framework.molang.MolangValue

// MoLang AST 节点: 多条语句组合（; 分隔），返回最后一条的值

class CompoundValue(private vararg val values: MolangValue) : MolangValue {
	override fun eval(context: MolangDataHolder?): Double {
		var result = 0.0
		for (value in values) {
			result = value.eval(context)
		}
		return result
	}

	override fun isMutable(): Boolean {
		for (v in values) {
			if (v.isMutable()) return true
		}
		return false
	}

	override fun toString(): String {
		return values.contentToString()
	}
}

