// MoLang 表达式解析器。字符串→分词→递归下降构建 AST。支持四则运算、比较、逻辑、三元、函数调用、变量/查询引用、赋值
package architecture.resonator_combat_framework.module.entity_animation.engine.molang

// MoLang 表达式解析器。字符串→分词→递归下降构建 AST。支持四则运算、比较、逻辑、三元、函数调用、变量/查询引用、赋值

import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.MathFunction
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.generic.*
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.limit.ClampFunction
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.limit.MaxFunction
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.limit.MinFunction
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.misc.PiFunction
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.misc.ToDegFunction
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.misc.ToRadFunction
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.random.DieRollFunction
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.random.DieRollIntegerFunction
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.random.RandomFunction
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.random.RandomIntegerFunction
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.function.round.*
import architecture.resonator_combat_framework.module.entity_animation.engine.molang.value.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.DoubleSupplier
import java.util.regex.Pattern
import kotlin.math.min

object MathParser {
	private val EXPRESSION_FORMAT: Pattern = Pattern.compile("^[\\w\\s_+\\-/*%^&|<>=!?:.,()]+$")
	private val WHITESPACE: Pattern = Pattern.compile("\\s")
	private val NUMERIC_PATTERN: Pattern = Pattern.compile("^-?\\d+(\\.\\d+)?$")

	private val FUNCTION_FACTORIES: MutableMap<String, MathFunction.Factory<*>> =
		ConcurrentHashMap<String, MathFunction.Factory<*>>()

	// ============== PUBLIC API ==============
	fun isFunctionRegistered(name: String?): Boolean {
		return FUNCTION_FACTORIES.containsKey(name)
	}

	fun registerFunction(name: String, factory: MathFunction.Factory<*>) {
		FUNCTION_FACTORIES[name] = factory
	}

	fun <T : MathFunction> buildFunction(name: String, vararg args: MathValue): T? {
		val factory = FUNCTION_FACTORIES[name] ?: return null
		return factory.create(*args) as T?
	}

	fun registerVariable(variable: Variable) {
		MolangQueries.registerVariable(variable.name)
	}

	fun getVariableFor(name: String): Variable {
		return MolangQueries.getVariableFor(name)
	}

	fun setVariable(name: String, supplier: DoubleSupplier) {
		getVariableFor(name).set(supplier)
	}

	/** Entry point: parse a Molang expression string into an executable MathValue  */
	fun compileMolang(expression: String): MathValue {
		if (expression.startsWith("return")) {
			var trimmed = expression.substring("return".length).trim { it <= ' ' }
			val semiColonIdx = trimmed.indexOf(';')
			if (semiColonIdx >= 0) {
				trimmed = trimmed.substring(0, semiColonIdx)
			}
			return compileExpression(trimmed)
		}

		if (expression.contains(";")) {
			val parts = expression.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			val compiled: MutableList<MathValue> = ArrayList<MathValue>()
			for (part in parts) {
				var part = part
				part = part.trim { it <= ' ' }
				if (part.isEmpty()) continue
				val isReturn = part.startsWith("return")
				if (isReturn) {
					part = part.substring("return".length).trim { it <= ' ' }
				}
				compiled.add(compileExpression(part))
				if (isReturn) break
			}
			return CompoundValue(*compiled.toTypedArray())
		}

		return compileExpression(expression)
	}

	/** Parse a single Molang expression string into an executable MathValue  */
	fun compileExpression(expression: String): MathValue {
		try {
			val chars = decomposeExpression(expression)
			val tokens = compileSymbols(chars)
			return parseSymbols(tokens)
		} catch (e: CompoundException) {
			throw e.withMessage("Error compiling expression: $expression")
		}
	}

	// ============== DECOMPOSITION ==============
	fun decomposeExpression(expression: String): CharArray {
		var expression = expression
		if (expression.isEmpty()) {
			return charArrayOf(0.toChar())
		}

		expression = expression.trim { it <= ' ' }

		if (!EXPRESSION_FORMAT.matcher(expression).matches()) {
			throw CompoundException("Invalid characters in expression: $expression")
		}

		// Remove whitespace, lowercase
		val cleaned = WHITESPACE.matcher(expression).replaceAll("")
			.lowercase()
		val chars = cleaned.toCharArray()

		// Check bracket balance
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

	// ============== TOKENIZATION ==============
	fun compileSymbols(chars: CharArray): MutableList<Token> {
		val tokens: MutableList<Token> = ArrayList<Token>()
		val buf = StringBuilder()
		var lastOperatorIndex = -1

		var i = 0
		while (i < chars.size) {
			val c = chars[i]

			// Special handling for negative sign (unary minus)
			if (c == '-') {
				if (buf.isEmpty() && (tokens.isEmpty() || lastOperatorIndex == tokens.size - 1)) {
					buf.append(c)
					i++
					continue
				}
			}

			// Try to match a multi-character operator
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

			if (c == '(') {
				if (buf.isNotEmpty()) {
					tokens.add(Token(buf.toString()))
					buf.setLength(0)
				}
				// Find matching closing paren
				var depth = 1
				val start = i + 1
				var found = false
				for (j in i + 1..<chars.size) {
					if (chars[j] == '(') depth++
					else if (chars[j] == ')') depth--
					if (depth == 0) {
						// Parse inner content, split by commas for function arguments
						val inner = chars.copyOfRange(start, j)
						val args = parseFunctionArguments(inner)
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

	/** Parse function arguments from inner parentheses content, splitting by commas  */
	private fun parseFunctionArguments(inner: CharArray): MutableList<MathValue> {
		if (inner.isEmpty()) return mutableListOf()

		// Tokenize the inner content
		val innerTokens = compileSymbols(inner)

		// Split by commas
		val args = ArrayList<MathValue>()
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

	// ============== AST BUILDING ==============
	fun parseSymbols(tokens: MutableList<Token>): MathValue {
		if (tokens.isEmpty()) return Constant(0.0)

		// Single token cases
		if (tokens.size == 1) {
			val token = tokens[0]
			if (token.isString()) {
				return compileSingleValue(token.string!!)
			}
			// Sub-expression token: could be function arguments or parenthesized expression
			// If it's a single value, return it; otherwise wrap in compound
			if (token.isSubExpr()) {
				if (token.subExpr!!.size == 1) {
					return token.subExpr[0]
				}
				if (token.subExpr.isEmpty()) return Constant(0.0)
				// Check if it's a function call context (the previous token would be the function name)
				// This case is handled by the function-call path below
			}
		}

		// Check for function call pattern: name (sub-expr with args)
		if (tokens.size >= 2) {
			val first = tokens[0]
			val second = tokens[1]
			if (first.isString() && second.isSubExpr()) {
				val funcResult = tryBuildFunction(first.string!!, second.subExpr!!)
				if (funcResult != null) {
					return funcResult
				}
				// Not a function - treat as variable (first) wrapped around group
				// (e.g., just a parenthesized expression)
				if (second.subExpr.size == 1) {
					// Handle (expr) as group
					return Group(second.subExpr[0])
				}
			}
		}

		// Handle ternary operator
		val ternary = compileTernary(tokens)
		if (ternary != null) return ternary

		// Handle calculation with operator precedence
		val calc = compileCalculation(tokens)
		if (calc != null) return calc

		// Fallback: wrap remaining tokens
		val values: MutableList<MathValue> = ArrayList<MathValue>()
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

	private fun compileTernary(tokens: MutableList<Token>): MathValue? {
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

	private fun compileCalculation(tokens: MutableList<Token>): MathValue? {
		if (tokens.size < 3) return null

		// Collect operator positions
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

		// Collect operands (values between operators)
		val operands: MutableList<MathValue> = ArrayList<MathValue>()
		operands.add(parseSymbols(tokens.subList(0, opPositions[0])))

		for (i in opPositions.indices) {
			val from = opPositions[i] + 1
			val to: Int = (if (i + 1 < opPositions.size) opPositions[i + 1] else tokens.size)
			operands.add(parseSymbols(tokens.subList(from, to)))
		}

		// Handle assignment operator specially
		for (i in operators.indices) {
			if (operators[i] === Operator.ASSIGN_VARIABLE) {
				val left = operands[i]
				if (left !is Variable) {
					throw CompoundException("Attempted to assign a value to a non-variable")
				}
				val right: MathValue = if (i + 1 < operators.size) {
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
		operands: MutableList<MathValue>
	): MathValue {
		if (operators.isEmpty()) {
			return if (operands.isEmpty()) Constant(0.0) else operands[0]
		}

		// Find lowest precedence operator
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

	// ============== SINGLE VALUE ==============
	private fun compileSingleValue(token: String): MathValue {
		if (token.isNullOrEmpty() || token[0].code == 0) {
			return Constant(0.0)
		}

		// Numeric literal
		if (NUMERIC_PATTERN.matcher(token).matches()) {
			return Constant(token.toDouble())
		}

		// Boolean literals
		if ("true" == token) return Constant(1.0)
		if ("false" == token) return Constant(0.0)

		// Try 0-arg function (e.g., math.pi)
		val zeroArgFunc = buildFunction<MathFunction>(token)
		if (zeroArgFunc != null) return zeroArgFunc

		// Variable or query reference
		return getVariableFor(token)
	}

	private fun tryBuildFunction(name: String, args: MutableList<MathValue>): MathValue? {
		if (!isFunctionRegistered(name)) return null
		return buildFunction<MathFunction>(name, *args.toTypedArray())
	}

	// ============== HELPERS ==============
	fun isNumeric(token: String): Boolean {
		return NUMERIC_PATTERN.matcher(token).matches()
	}

	fun isQueryOrFunctionName(token: String): Boolean {
		if (token.startsWith("query.") && token.length > 6) return true
		return isFunctionRegistered(token)
	}

	fun isLikelyVariable(token: String): Boolean {
		if (token.startsWith("temp.") || token.startsWith("variable.")) return true
		if (token.startsWith("query.")) return true
		return token.matches("[a-zA-Z_][a-zA-Z0-9_.]*".toRegex())
	}

	// ============== STATIC FUNCTION REGISTRATION ==============
	init {
		// Generic
		registerFunction("math.abs", MathFunction.Factory { args -> AbsFunction(args[0]) })
		registerFunction("math.acos", MathFunction.Factory { args -> ACosFunction(args[0]) })
		registerFunction("math.asin", MathFunction.Factory { args -> ASinFunction(args[0]) })
		registerFunction("math.atan", MathFunction.Factory { args -> ATanFunction(args[0]) })
		registerFunction(
			"math.atan2",
			MathFunction.Factory { args -> ATan2Function(args[0], args[1]) })
		registerFunction("math.cos", MathFunction.Factory { args -> CosFunction(args[0]) })
		registerFunction("math.exp", MathFunction.Factory { args -> ExpFunction(args[0]) })
		registerFunction("math.ln", MathFunction.Factory { args -> LogFunction(args[0]) })
		registerFunction("math.log", MathFunction.Factory { args -> LogFunction(args[0]) })
		registerFunction(
			"math.mod",
			MathFunction.Factory { args -> ModFunction(args[0], args[1]) })
		registerFunction(
			"math.pow",
			MathFunction.Factory { args -> PowFunction(args[0], args[1]) })
		registerFunction("math.sin", MathFunction.Factory { args -> SinFunction(args[0]) })
		registerFunction("math.sqrt", MathFunction.Factory { args -> SqrtFunction(args[0]) })

		// Limit
		registerFunction(
			"math.clamp",
			MathFunction.Factory { args -> ClampFunction(args[0], args[1], args[2]) })
		registerFunction(
			"math.max",
			MathFunction.Factory { args -> MaxFunction(args[0], args[1]) })
		registerFunction(
			"math.min",
			MathFunction.Factory { args -> MinFunction(args[0], args[1]) })

		// Misc
		registerFunction("math.pi", MathFunction.Factory { args -> PiFunction() })
		registerFunction("math.to_deg", MathFunction.Factory { args -> ToDegFunction(args[0]) })
		registerFunction("math.to_rad", MathFunction.Factory { args -> ToRadFunction(args[0]) })

		// Random
		registerFunction(
			"math.die_roll",
			MathFunction.Factory { args -> DieRollFunction(args[0], args[1]) })
		registerFunction(
			"math.die_roll_integer",
			MathFunction.Factory { args -> DieRollIntegerFunction(args[0], args[1]) })
		registerFunction(
			"math.random",
			MathFunction.Factory { args -> RandomFunction(args[0], args[1]) })
		registerFunction(
			"math.random_integer",
			MathFunction.Factory { args -> RandomIntegerFunction(args[0], args[1]) })

		// Round
		registerFunction("math.ceil", MathFunction.Factory { args -> CeilFunction(args[0]) })
		registerFunction("math.floor", MathFunction.Factory { args -> FloorFunction(args[0]) })
		registerFunction("math.round", MathFunction.Factory { args -> RoundFunction(args[0]) })
		registerFunction("math.trunc", MathFunction.Factory { args -> TruncateFunction(args[0]) })
		registerFunction(
			"math.hermite_blend",
			MathFunction.Factory { args -> HermiteBlendFunction(args[0]) })
		registerFunction(
			"math.lerp",
			MathFunction.Factory { args -> LerpFunction(args[0], args[1], args[2]) })
		registerFunction(
			"math.lerprotate",
			MathFunction.Factory { args -> LerpRotFunction(args[0], args[1], args[2]) })
	}

	// ============== TOKEN TYPES ==============
	class Token {
		val string: String?
		val subExpr: MutableList<MathValue>?

		constructor(string: String?) {
			this.string = string
			this.subExpr = null
		}

		constructor(subExpr: MutableList<MathValue>?) {
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

