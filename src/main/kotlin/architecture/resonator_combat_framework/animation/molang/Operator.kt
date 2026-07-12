// MoLang 运算符定义。包含所有支持的运算符及优先级和计算逻辑
package architecture.resonator_combat_framework.animation.molang

import kotlin.math.pow

@JvmRecord
data class Operator(
	val symbol: String,
	val precedence: Int,
	val operation: Operation
) : Comparable<Operator> {
	fun interface Operation {
		fun compute(a: Double, b: Double): Double
	}

	fun compute(a: Double, b: Double): Double {
		return operation.compute(a, b)
	}

	fun takesPrecedenceOver(other: Operator): Boolean {
		return this.precedence > other.precedence
	}

	override fun compareTo(other: Operator): Int {
		return this.precedence.compareTo(other.precedence)
	}

	companion object {
		private val OPERATORS = HashMap<String, Operator>()
		private val OPERATOR_SYMBOLS = HashSet<Char>()
		private var LONGEST_OPERATOR = 0

		val ADD = register("+", 10) { a, b -> a + b }
		val SUB = register("-", 10) { a, b -> a - b }
		val MUL = register("*", 20) { a, b -> a * b }
		val DIV = register("/", 20) { a, b -> a / b }
		val MOD = register("%", 20) { a, b -> a % b }
		val POW = register("^", 30) { a, b -> a.pow(b) }

		val AND = register("&&", 2) { a, b -> if (a != 0.0 && b != 0.0) 1.0 else 0.0 }
		val OR = register("||", 1) { a, b -> if (a != 0.0 || b != 0.0) 1.0 else 0.0 }

		val LT = register("<", 5) { a, b -> if (a < b) 1.0 else 0.0 }
		val LTE = register("<=", 5) { a, b -> if (a <= b) 1.0 else 0.0 }
		val GT = register(">", 5) { a, b -> if (a > b) 1.0 else 0.0 }
		val GTE = register(">=", 5) { a, b -> if (a >= b) 1.0 else 0.0 }
		val EQUAL = register("==", 5) { a, b -> if (a == b) 1.0 else 0.0 }
		val NOT_EQUAL = register("!=", 5) { a, b -> if (a != b) 1.0 else 0.0 }

		/** 空值合并：a ?? b，a 不为 0.0 时返回 a，否则返回 b */
		val NULL_COALESCE = register("??", 4) { a, b -> if (a == 0.0) b else a }


		val ASSIGN_VARIABLE = register("=", Int.MAX_VALUE) { _, b -> b }

		/** 箭头运算符：左 -> 右，将左侧值作为上下文传递给右侧 */
		val ARROW = register("->", 3) { a, b -> b }

		fun register(symbol: String, precedence: Int, operation: Operation): Operator {
			val op = Operator(symbol, precedence, operation)
			check(OPERATORS.put(symbol, op) == null) { "Duplicate operator: $symbol" }
			for (c in symbol.toCharArray()) {
				OPERATOR_SYMBOLS.add(c)
			}
			if (symbol.length > LONGEST_OPERATOR) {
				LONGEST_OPERATOR = symbol.length
			}
			return op
		}

		fun isOperator(symbol: String?): Boolean {
			return OPERATORS.containsKey(symbol)
		}

		fun bySymbol(symbol: String?): Operator? {
			return OPERATORS[symbol]
		}

		fun isOperativeSymbol(c: Char?): Boolean {
			return OPERATOR_SYMBOLS.contains(c)
		}

		fun maxOperatorLength(): Int {
			return LONGEST_OPERATOR
		}

		fun allOperators(): Collection<Operator> {
			return OPERATORS.values
		}
	}
}

