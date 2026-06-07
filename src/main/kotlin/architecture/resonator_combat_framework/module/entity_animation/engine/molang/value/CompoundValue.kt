package architecture.resonator_combat_framework.module.entity_animation.engine.molang.value

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangData

// MoLang AST 节点: 多条语句组合（; 分隔），返回最后一条的值
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.MolangValue

class CompoundValue(private vararg val values: MolangValue) : MolangValue {
	override fun get(context: MolangData?): Double {
		var result = 0.0
		for (value in values) {
			result = value.get(context)
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

