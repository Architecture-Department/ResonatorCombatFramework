package architecture.resonator_combat_framework.animation.molang

import architecture.resonator_combat_framework.animation.molang.function.MolangFunction
import architecture.resonator_combat_framework.animation.molang.function.generic.*
import architecture.resonator_combat_framework.animation.molang.function.limit.ClampFunction
import architecture.resonator_combat_framework.animation.molang.function.limit.MaxFunction
import architecture.resonator_combat_framework.animation.molang.function.limit.MinFunction
import architecture.resonator_combat_framework.animation.molang.function.misc.PiFunction
import architecture.resonator_combat_framework.animation.molang.function.misc.ToDegFunction
import architecture.resonator_combat_framework.animation.molang.function.misc.ToRadFunction
import architecture.resonator_combat_framework.animation.molang.function.random.DieRollFunction
import architecture.resonator_combat_framework.animation.molang.function.random.DieRollIntegerFunction
import architecture.resonator_combat_framework.animation.molang.function.random.RandomFunction
import architecture.resonator_combat_framework.animation.molang.function.random.RandomIntegerFunction
import architecture.resonator_combat_framework.animation.molang.function.round.*
import architecture.resonator_combat_framework.animation.molang.value.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.math.min

/**
 * MoLang 表达式解析器 —— 支持 Bedrock 标准 MoLang 语法的词法分析和 AST 构建。
 *
 * 支持的语法特性：
 * - 算术运算（+ - * / %）
 * - 比较运算（== != < > <= >=）
 * - 逻辑运算（&& || !）
 * - 三元条件（a ? b : c）
 * - 赋值（=）
 * - 变量引用（v.xxx, q.xxx, t.xxx）
 * - 内置函数（math.sin, math.cos, math.lerp 等）
 * - 循环（loop, for_each, break, continue）
 * - 块表达式（{...}），return
 * - 作用域嵌套
 */
object MoLangParser {
	private val EXPRESSION_FORMAT: Pattern = Pattern.compile("^[\\w\\s_+\\-/*%^&|<>=!?:.,(){};]+$")
	private val WHITESPACE: Pattern = Pattern.compile("\\s")
	private val NUMERIC_PATTERN: Pattern = Pattern.compile("^-?\\d+(\\.\\d+)?$")

	private val FUNCTION_FACTORIES: MutableMap<String, MolangFunction.Factory<*>> =
		ConcurrentHashMap<String, MolangFunction.Factory<*>>()

	// ============== 公开 API ==============
	fun isFunctionRegistered(name: String?): Boolean {
		return FUNCTION_FACTORIES.containsKey(name)
	}

	fun registerFunction(name: String, factory: MolangFunction.Factory<*>) {
		FUNCTION_FACTORIES[name] = factory
	}

	fun <T : MolangFunction> buildFunction(name: String, vararg args: MolangValue): T? {
		val factory = FUNCTION_FACTORIES[name] ?: return null
		@Suppress("UNCHECKED_CAST")
		return factory.create(*args) as T?
	}

	fun getVariableFor(name: String): Variable {
		return Variable(name)
	}

	/** 入口：将 MoLang 表达式字符串解析为可执行 AST */
	fun compileMolang(expression: String): MolangValue {
		if (expression.startsWith("return")) {
			var trimmed = expression.substring("return".length).trim { it <= ' ' }
			val semiColonIdx = trimmed.indexOf(';')
			if (semiColonIdx >= 0) {
				trimmed = trimmed.substring(0, semiColonIdx)
			}
			return ReturnExpr(compileExpression(trimmed))
		}

		if (expression.contains(";")) {
			val parts = expression.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			val compiled: MutableList<MolangValue> = ArrayList<MolangValue>()
			for (part in parts) {
				var part = part
				part = part.trim { it <= ' ' }
				if (part.isEmpty()) continue
				val isReturn = part.startsWith("return")
				if (isReturn) {
					part = part.substring("return".length).trim { it <= ' ' }
				}
				compiled.add(if (isReturn) ReturnExpr(compileExpression(part)) else compileExpression(part))
				if (isReturn) break
			}
			return CompoundValue(*compiled.toTypedArray())
		}

		return compileExpression(expression)
	}

	/** 解析单个 MoLang 表达式字符串为可执行 AST */
	fun compileExpression(expression: String): MolangValue {
		try {
			val chars = decomposeExpression(expression)
			val tokens = compileSymbols(chars)
			return parseSymbols(tokens)
		} catch (e: CompoundException) {
			throw e.withMessage("Error compiling expression: $expression")
		}
	}

	// ============== 表达式预处理 ==============
	fun decomposeExpression(expression: String): CharArray {
		var expression = expression
		if (expression.isEmpty()) {
			return charArrayOf(0.toChar())
		}

		expression = expression.trim { it <= ' ' }

		if (!EXPRESSION_FORMAT.matcher(expression).matches()) {
			throw CompoundException("Invalid characters in expression: $expression")
		}

		// 移除空白字符，转小写
		val cleaned = WHITESPACE.matcher(expression).replaceAll("")
			.lowercase()
		val chars = cleaned.toCharArray()

		// 检查括号平衡
		var depth = 0
		for (c in chars) {
			if (c == '(') depth++
			else if (c == ')') depth--
			if (depth < 0) {
				throw CompoundException("Mismatched parentheses in: $expression")
			}
		}
		if (depth != 0) {
			throw CompoundException("Unclosed parentheses in: $expression")
		}

		return chars
	}

	// ============== 分词 ==============
	fun compileSymbols(chars: CharArray): MutableList<Token> {
		val tokens: MutableList<Token> = ArrayList<Token>()
		val buf = StringBuilder()
		var lastOperatorIndex = -1

		var i = 0
		while (i < chars.size) {
			val c = chars[i]

			// 特殊处理一元负号和逻辑非
			if (c == '-' || c == '!') {
				if (buf.isEmpty() && (tokens.isEmpty() || lastOperatorIndex == tokens.size - 1)) {
					buf.append(c)
					i++
					continue
				}
			}

			// 尝试匹配多字符运算符
			val op = tryMergeOperativeSymbols(chars, i)
			if (op != null) {
				i += op.length - 1
				if (buf.isNotEmpty()) {
					tokens.add(Token(buf.toString()))
					buf.setLength(0)
				}
				lastOperatorIndex = tokens.size
				tokens.add(Token(op))
				i++
				continue
			}

			if (c == '{' || c == '(') {
				if (buf.isNotEmpty()) {
					tokens.add(Token(buf.toString()))
					buf.setLength(0)
				}
				// 查找匹配的右括号/花括号
				val closeChar = if (c == '(') ')' else '}'
				var depth = 1
				val start = i + 1
				var found = false
				for (j in i + 1..<chars.size) {
					if (chars[j] == '(') depth++
					else if (chars[j] == closeChar) depth--
					if (depth == 0) {
						// 解析括号内内容，按逗号分隔为参数
						val inner = chars.copyOfRange(start, j)
						val args: MutableList<MolangValue> = if (c == '(') parseFunctionArguments(inner) else {
							val blockExpr = BlockExpr(compileExpression(String(inner)))
							mutableListOf(blockExpr)
						}
						tokens.add(Token(args))
						i = j
						found = true
						break
					}
				}
				if (!found) {
					throw CompoundException("Unmatched opening parenthesis")
				}
				i++
				continue
			}

			if (c == ',' || c == '?' || c == ':') {
				if (buf.isNotEmpty()) {
					tokens.add(Token(buf.toString()))
					buf.setLength(0)
				}
				tokens.add(Token(c.toString()))
				i++
				continue
			}

			buf.append(c)
			i++
		}

		if (buf.isNotEmpty()) {
			tokens.add(Token(buf.toString()))
		}

		return tokens
	}

	/** 解析括号内的函数参数，按逗号分隔  */
	private fun parseFunctionArguments(inner: CharArray): MutableList<MolangValue> {
		if (inner.isEmpty()) return mutableListOf()

		// 对内层内容进行分词
		val innerTokens = compileSymbols(inner)

		// 按逗号分隔
		val args = ArrayList<MolangValue>()
		var lastComma = -1

		for (i in 0..innerTokens.size) {
			if (i == innerTokens.size || (innerTokens[i].isString() && "," == innerTokens[i].string)) {
				if (i > lastComma + 1) {
					val segment = innerTokens.subList(lastComma + 1, i)
					val value = parseSymbols(segment)
					args.add(value)
				}
				lastComma = i
			}
		}

		return args
	}

	private fun tryMergeOperativeSymbols(chars: CharArray, index: Int): String? {
		val c = chars[index]
		if (!Operator.isOperativeSymbol(c)) return null

		val maxLen = min(chars.size - index, Operator.maxOperatorLength())
		for (len in maxLen downTo 1) {
			val candidate = String(chars, index, len)
			if (Operator.isOperator(candidate)) {
				return candidate
			}
		}

		if (c == '?' || c == ':' || c == ',') {
			return c.toString()
		}

		return null
	}

	// ============== AST 构建 ==============
	fun parseSymbols(tokens: MutableList<Token>): MolangValue {
		if (tokens.isEmpty()) return Constant(0.0)

		// 单个 token 的情况
		if (tokens.size == 1) {
			val token = tokens[0]
			if (token.isString()) {
				return compileSingleValue(token.string!!)
			}
			// 子表达式 token：可能是函数参数或括号表达式
			// 若只有一个值则直接返回，否则包装为复合值's a single value, return it; otherwise wrap in compound
			if (token.isSubExpr()) {
				if (token.subExpr!!.size == 1) {
					return token.subExpr[0]
				}
				if (token.subExpr.isEmpty()) return Constant(0.0)
				// 检查是否为函数调用上下文（前一 token 是函数名）
				// 此情况由下方的函数调用路径处理
			}
		}

		// 检查函数调用模式：名称（子表达式参数）
		if (tokens.size >= 2) {
			val first = tokens[0]
			val second = tokens[1]
			if (first.isString() && second.isSubExpr()) {
				val name = first.string!!
				val args = second.subExpr!!

				// 内置控制结构（loop/for_each）
				// 一元非：!(expr) 或 !{block}
				if (name == "!") return BooleanNegate(Group(args[0]))
				if (name == "loop" && args.size >= 2) return LoopExpr(args[0], args[1])
				if (name == "for_each" && args.size >= 3) return ForEachExpr(args[0].toString(), args[1], args[2])

				val funcResult = tryBuildFunction(name, args)
				if (funcResult != null) {
					return funcResult
				}
				// 不是函数——将第一个 token 作为变量，其余视为括号分组
				// 例如：仅括号包裹的表达式
				if (second.subExpr.size == 1) {
					// 将 (expr) 作为分组处理
					return Group(second.subExpr[0])
				}
			}
		}

		// 处理三元运算符
		val ternary = compileTernary(tokens)
		if (ternary != null) return ternary

		// 按运算符优先级处理计算
		val calc = compileCalculation(tokens)
		if (calc != null) return calc

		// 兜底：将剩余 token 包装为复合值
		val values: MutableList<MolangValue> = ArrayList<MolangValue>()
		for (token in tokens) {
			if (token.isString()) {
				values.add(compileSingleValue(token.string!!))
			} else if (token.isSubExpr()) {
				values.addAll(token.subExpr!!)
			}
		}

		if (values.isEmpty()) return Constant(0.0)
		if (values.size == 1) return values[0]
		return CompoundValue(*values.toTypedArray())
	}

	private fun compileTernary(tokens: MutableList<Token>): MolangValue? {
		var qIndex = -1
		var depth = 0
		for (i in tokens.indices) {
			val t = tokens[i]
			if (t.isString()) {
				if ("?" == t.string && depth == 0) {
					qIndex = i
					break
				} else if ("(" == t.string) depth++
				else if (")" == t.string) depth--
			}
		}
		if (qIndex < 0) return null

		var cIndex = -1
		depth = 0
		for (i in qIndex + 1..<tokens.size) {
			val t = tokens[i]
			if (t.isString()) {
				if (":" == t.string && depth == 0) {
					cIndex = i
					break
				} else if ("(" == t.string) depth++
				else if (")" == t.string) depth--
			}
		}
		if (cIndex < 0) return null

		val condition = parseSymbols(tokens.subList(0, qIndex))
		val trueVal = parseSymbols(tokens.subList(qIndex + 1, cIndex))
		val falseVal = parseSymbols(tokens.subList(cIndex + 1, tokens.size))
		return Ternary(condition, trueVal, falseVal)
	}

	private fun compileCalculation(tokens: MutableList<Token>): MolangValue? {
		if (tokens.size < 3) return null

		// 收集运算符位置
		val opPositions = ArrayList<Int>()
		val operators = ArrayList<Operator>()

		for (i in tokens.indices) {
			val t = tokens[i]
			if (t.isString() && Operator.isOperator(t.string)) {
				opPositions.add(i)
				operators.add(Operator.bySymbol(t.string)!!)
			}
		}

		if (operators.isEmpty()) return null

		// 收集操作数（运算符之间的值）
		val operands: MutableList<MolangValue> = ArrayList<MolangValue>()
		operands.add(parseSymbols(tokens.subList(0, opPositions[0])))

		for (i in opPositions.indices) {
			val from = opPositions[i] + 1
			val to: Int = (if (i + 1 < opPositions.size) opPositions[i + 1] else tokens.size)
			operands.add(parseSymbols(tokens.subList(from, to)))
		}

		// 特殊处理赋值运算符
		for (i in operators.indices) {
			if (operators[i] === Operator.ASSIGN_VARIABLE) {
				val left = operands[i]
				if (left !is Variable) {
					throw CompoundException("Attempted to assign a value to a non-variable")
				}
				val right: MolangValue = if (i + 1 < operators.size) {
					buildCalculationTree(
						operators.subList(i + 1, operators.size),
						operands.subList(i + 1, operands.size)
					)
				} else {
					operands[i + 1]
				}
				return VariableAssignment(left, right)
			}
		}

		return buildCalculationTree(operators, operands)
	}

	private fun buildCalculationTree(
		operators: MutableList<Operator>,
		operands: MutableList<MolangValue>
	): MolangValue {
		if (operators.isEmpty()) {
			return if (operands.isEmpty()) Constant(0.0) else operands[0]
		}

		// 查找最低优先级的运算符
		var lowest: Operator? = null
		var lowestIndex = -1
		for (i in operators.indices) {
			val op = operators[i]
			if (lowest == null || !op.takesPrecedenceOver(lowest)) {
				lowest = op
				lowestIndex = i
			}
		}

		val left = buildCalculationTree(
			operators.subList(0, lowestIndex),
			operands.subList(0, lowestIndex + 1)
		)
		val right = buildCalculationTree(
			operators.subList(lowestIndex + 1, operators.size),
			operands.subList(lowestIndex + 1, operands.size)
		)

		return Calculation(lowest!!, left, right)
	}

	// ============== 单值处理 ==============
	private fun compileSingleValue(token: String): MolangValue {
		if (token.isNullOrEmpty() || token[0].code == 0) {
			return Constant(0.0)
		}

		// 数值字面量
		if (NUMERIC_PATTERN.matcher(token).matches()) {
			return Constant(token.toDouble())
		}

		// 布尔字面量
		if ("true" == token) return Constant(1.0)
		if ("false" == token) return Constant(0.0)

		// this 关键字：返回当前上下文值
		if (token == "this") return Constant(0.0)

		// 一元负号：-expr 转 0 - expr
		if (token.startsWith("-") && token.length > 1 && !NUMERIC_PATTERN.matcher(token).matches()) {
			return Calculation(Operator.SUB, Constant(0.0), compileSingleValue(token.substring(1)))
		}

		// break/continue 循环控制关键字
		if (token == "break") return BreakExpr
		if (token == "continue") return ContinueExpr

		// 尝试无参函数调用（如 math.pi）
		val zeroArgFunc = buildFunction<MolangFunction>(token)
		if (zeroArgFunc != null) return zeroArgFunc

		// 变量或查询引用
		// 一元非：!expr
		if (token.startsWith("!") && token.length > 1) {
			return BooleanNegate(compileSingleValue(token.substring(1)))
		}
		return getVariableFor(token)
	}

	private fun tryBuildFunction(name: String, args: MutableList<MolangValue>): MolangValue? {
		if (!isFunctionRegistered(name)) return null
		return buildFunction<MolangFunction>(name, *args.toTypedArray())
	}

	// ============== 辅助方法 ==============
	fun isNumeric(token: String): Boolean {
		return NUMERIC_PATTERN.matcher(token).matches()
	}

	fun isQueryOrFunctionName(token: String): Boolean {
		if (token.startsWith("query.") && token.length > 6) return true
		return isFunctionRegistered(token)
	}

	fun isLikelyVariable(token: String): Boolean {
		if (token.startsWith("temp.") || token.startsWith("variable.")) return true
		if (token.startsWith("geometry.") || token.startsWith("material.") || token.startsWith("texture.")) return true
		if (token.startsWith("query.")) return true
		return token.matches("[a-zA-Z_][a-zA-Z0-9_.]*".toRegex())
	}

	// ============== 静态函数注册 ==============
	init {
		// 通用数学函数
		registerFunction("math.abs", MolangFunction.Factory { args -> AbsFunction(args[0]) })
		registerFunction("math.acos", MolangFunction.Factory { args -> ACosFunction(args[0]) })
		registerFunction("math.asin", MolangFunction.Factory { args -> ASinFunction(args[0]) })
		registerFunction("math.atan", MolangFunction.Factory { args -> ATanFunction(args[0]) })
		registerFunction(
			"math.atan2",
			MolangFunction.Factory { args -> ATan2Function(args[0], args[1]) })
		registerFunction("math.cos", MolangFunction.Factory { args -> CosFunction(args[0]) })
		registerFunction("math.exp", MolangFunction.Factory { args -> ExpFunction(args[0]) })
		registerFunction("math.log", MolangFunction.Factory { args -> LogFunction(args[0]) })
		registerFunction(
			"math.mod",
			MolangFunction.Factory { args -> ModFunction(args[0], args[1]) })
		registerFunction(
			"math.pow",
			MolangFunction.Factory { args -> PowFunction(args[0], args[1]) })
		registerFunction("math.sin", MolangFunction.Factory { args -> SinFunction(args[0]) })
		registerFunction("math.sqrt", MolangFunction.Factory { args -> SqrtFunction(args[0]) })

		// 数值限制函数
		registerFunction(
			"math.clamp",
			MolangFunction.Factory { args -> ClampFunction(args[0], args[1], args[2]) })
		registerFunction(
			"math.max",
			MolangFunction.Factory { args -> MaxFunction(args[0], args[1]) })
		registerFunction(
			"math.min",
			MolangFunction.Factory { args -> MinFunction(args[0], args[1]) })

		// 杂项函数
		registerFunction("math.pi", MolangFunction.Factory { args -> PiFunction() })
		registerFunction("math.to_deg", MolangFunction.Factory { args -> ToDegFunction(args[0]) })
		registerFunction("math.to_rad", MolangFunction.Factory { args -> ToRadFunction(args[0]) })

		// 随机数函数
		registerFunction(
			"math.die_roll",
			MolangFunction.Factory { args -> DieRollFunction(args[0], args[1]) })
		registerFunction(
			"math.die_roll_integer",
			MolangFunction.Factory { args -> DieRollIntegerFunction(args[0], args[1]) })
		registerFunction(
			"math.random",
			MolangFunction.Factory { args -> RandomFunction(args[0], args[1]) })
		registerFunction(
			"math.random_integer",
			MolangFunction.Factory { args -> RandomIntegerFunction(args[0], args[1]) })

		// 取整/插值函数
		registerFunction("math.ceil", MolangFunction.Factory { args -> CeilFunction(args[0]) })
		registerFunction("math.floor", MolangFunction.Factory { args -> FloorFunction(args[0]) })
		registerFunction("math.round", MolangFunction.Factory { args -> RoundFunction(args[0]) })
		registerFunction("math.trunc", MolangFunction.Factory { args -> TruncateFunction(args[0]) })
		registerFunction(
			"math.hermite_blend",
			MolangFunction.Factory { args -> HermiteBlendFunction(args[0]) })
		registerFunction(
			"math.lerp",
			MolangFunction.Factory { args -> LerpFunction(args[0], args[1], args[2]) })
		registerFunction(
			"math.lerprotate",
			MolangFunction.Factory { args -> LerpRotFunction(args[0], args[1], args[2]) })
	}

	// ============== Token 类型 ==============
	class Token {
		val string: String?
		val subExpr: MutableList<MolangValue>?

		constructor(string: String?) {
			this.string = string
			this.subExpr = null
		}

		constructor(subExpr: MutableList<MolangValue>?) {
			this.string = null
			this.subExpr = subExpr
		}

		fun isString(): Boolean {
			return string != null
		}

		fun isSubExpr(): Boolean {
			return subExpr != null
		}

		val isOperator: Boolean
			get() = string != null && Operator.isOperator(string)
	}
}

