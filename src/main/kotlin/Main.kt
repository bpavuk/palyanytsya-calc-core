// TODO: turn it into Android app using Material You guidelines
// TODO: catch 100 stars at GitHub
package calculator

import java.math.BigInteger
import kotlin.math.max
import kotlin.math.min

fun main() {
    Calculator().start()
}

class Calculator(private val debug: Boolean = false) {
    private val commandRegex = Regex("/.*")
    // useful vars zone
    private val variablesMap = emptyMap<String, BigInteger>().toMutableMap()
    // functions zone
    fun start() {
        mainloop@while (true) {
            val expression = readln()

            if (expression == "/exit") break@mainloop
            else if (expression.isEmpty()) continue@mainloop
            else if (expression.matches(commandRegex)) {
                command(cmd = expression)
                continue@mainloop
            } else if (debug or !expression.matches(commandRegex)) {
                val answer = try {
                    calculate(expression.normalize())
                } catch (e: IllegalArgumentException) {
                    e.message
                }
                if (!answer.isNullOrBlank()) println(answer)
            }
        }
        println("Bye!")
    }

    private fun validate(expression: String): Boolean {
        val allSignsRegex = Regex(" - | \\+ | \\* | / | \\^ ")
        val coreValidatorRegex = Regex("\\+?( ?- |\\+)?(([a-zA-Z]+)|(\\d+))(\\+?($allSignsRegex)(([a-zA-Z]+)|(\\d+)))*")
        val assignmentRegex = Regex("([a-zA-Z]+) = $coreValidatorRegex")
        val invalidAssignmentRegex = Regex("([a-zA-Z0-9]+) = .* = .*")
        val singleVariableRegex = Regex("[a-zA-Z]+")

        return if (expression.matches(coreValidatorRegex)
            or expression.matches(commandRegex)
            or expression.isEmpty()) {
            if (expression.matches(singleVariableRegex) && expression !in variablesMap.keys) {
                throw IllegalArgumentException("Unknown variable")
            }
            true
        } else if (expression.matches(assignmentRegex)) {
            for (i in expression.split(" = ")[1].split(allSignsRegex)) {
                if ((i in variablesMap.keys) or i.isNumeric()) continue
                else {
                    throw IllegalArgumentException("Unknown variable")
                }
            }
            true
        } else if (expression.matches(invalidAssignmentRegex)) {
            throw IllegalArgumentException("Invalid identifier")
        } else {
            throw IllegalArgumentException("Invalid expression")
        }
    }

    fun calculate(expression: String): String? {
        var mutableExpression = expression
        var isMutated = false
        for (i in expression.split(Regex(" - | \\+ | \\* | / | \\^ | \\( | \\) "))) {
            if (i in variablesMap.keys) {
                mutableExpression = mutableExpression.replaceFirst(i, variablesMap[i].toString())
                isMutated = true
            }
            else if (i.isNumeric()) continue
        }
        if (isMutated) return calculate(mutableExpression.normalize())
        return if (mutableExpression.matches(Regex("([a-zA-Z])+ = ([a-zA-Z])+"))
            or mutableExpression.matches(Regex("([a-zA-Z])+ = .+"))) {
            val parsedExpression = mutableExpression.split(" = ")
            if (parsedExpression.lastIndex > 1) throw IllegalArgumentException("Invalid assignment")
            variablesMap[parsedExpression[0]] = calculate(parsedExpression[1])!!.toBigInteger()
            null
        } else {
            testCalc(mutableExpression)
        }
    }

    private fun testCalc(s: String): String {
        // сделать строку изменяемой, раскрыть скобки в начале и конце если таковые присутствуют
        var expression = s.normalize()
        while (expression.first() == '(' && expression.last() == ')') {
            expression = expression.replaceFirst("(", "")
            expression = expression.replaceLast(")", "")
        }
        if (expression.count { it == '(' } != expression.count { it == ')' }) throw IllegalArgumentException("Invalid expression")
        // пройтись по массиву, заменить выражения в скобках на нормальные цифры (рекурсия)
        val allSignsRegex = Regex(" - | \\+ | \\* | / | \\^ ")
        val coreValidatorRegex = Regex("\\+?( - |\\+)?(([a-zA-Z]+)|(\\d+))(\\+?($allSignsRegex)(([a-zA-Z]+)|(\\d+)))*")  // ебать, всегда регексы ненавидел
        while (expression.contains(Regex("\\("))) {
            val result = Regex("\\($coreValidatorRegex\\)").find(expression)
            if (result != null) expression = expression.replaceFirst(result.value, testCalc(result.value))
        }
        // если выражение не имеет скобок, то валидировать (да-да, придется использовать исключения чтоб мгновенно прерывать рекурсию)
        if (!expression.contains("[()]".toRegex())) Calculator().validate(expression)
        else throw IllegalArgumentException("Invalid expression")

        // понять, что от нас хотят - задать переменную или просто вычислить выражение
        if (s.matches("[a-zA-Z] = .*".toRegex())) {
            variablesMap[s.split(" = ")[0]] = testCalc(s.split(" = ")[1]).toBigInteger()
        }
        // создать массив из отформатированной строки, благодаря этому массиву можно будет выполнять вычисления
        val calcList = expression.split(" ").toMutableList()
        calcList.removeAll { it == "" }

        // пройтись по массиву, если был найден оператор ^ то число находящееся позади ^ возвести в степень [число находящееся впереди]
        while (calcList.contains("^")) {
            val i = calcList.indexOfFirst { it == "^" }
            var result = calcList[i - 1].toBigInteger()

            var j = BigInteger.valueOf(1)
            while (j < calcList[i + 1].toBigInteger()) {   // в for выполняется само вычисление, остальной код до и перед for
                result *= calcList[i - 1].toBigInteger()         // внутри while можно спокойно использовать как шаблон для остальных
                j++                                              // проходов, ведь это просто чтение и запись выражения
            }

            calcList.removeAt(i + 1)
            calcList.removeAt(i - 1)
            calcList[i - 1] = result.toString()
        }
        // пройтись по массиву, если был найден оператор / или * то аналогично с алгоритмом возведения в степень разделить/умножить два числа
        while (calcList.contains("*") || calcList.contains("/")) {
            val i = if (calcList.indexOfFirst { it == "/" } < 0 || calcList.indexOfFirst { it == "*" } < 0){
                max(calcList.indexOfFirst { it == "/" }, calcList.indexOfFirst { it == "*" })
            } else {
                min(calcList.indexOfFirst { it == "/" }, calcList.indexOfFirst { it == "*" })
            }
            var result = calcList[i - 1].toBigInteger()

            when (calcList[i]) {
                "*" -> result *= calcList[i + 1].toBigInteger()
                "/" -> result /= calcList[i + 1].toBigInteger()
            }

            calcList.removeAt(i + 1)
            calcList.removeAt(i - 1)
            calcList[i - 1] = result.toString()
        }
        // пройтись по массиву, если были найдены + или - то аналогичным образом посчитать сумму/разницу двух чисел
        while (calcList.contains("+") || calcList.contains("-")) {
            val i = calcList.indexOfFirst { it == "+" || it == "-" }
            var result = if (i > 0) calcList[i - 1].toBigInteger() else BigInteger.ZERO
            when (calcList[i]) {
                "+" -> result += calcList[i + 1].toBigInteger()
                "-" -> result -= calcList[i + 1].toBigInteger()
            }

            calcList.removeAt(i + 1)
            if (i > 0) calcList.removeAt(i - 1)
            if (i > 0) calcList[i - 1] = result.toString() else calcList[i] = result.toString()
        }
        return calcList[0]

        // наслаждаться
    }


    private fun command(cmd: String) {
        when (cmd) {
            "/help" -> println("Hi! I am smart-calculator, but now I can work just with +, - and variables")
            else -> println("Unknown command")
        }
    }
}

fun String.isNumeric(): Boolean {
    return this.matches(" ?-? ?\\d+".toRegex())
}

fun String.normalize(): String {
    return this.replace(" ", "")
        .replace("--", "+")
        .replace("(\\+-)|(-\\+)".toRegex(), "-")
        .replace("\\++".toRegex(), "+")
        .replace("-", " - ")
        .replace("+", " + ")
        .replace("/", " / ")
        .replace("*", " * ")
        .replace("^", " ^ ")
        .replace("=", " = ")
        .replace(" +".toRegex(), " ")
}

fun String.replaceLast(toReplace: String, newString: String): String
{
    if (last().toString() == toReplace)
    {
        return dropLast(1) + newString
    }
    return this
}
