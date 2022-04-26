// Here we use Any? because in lox we can possibly have null
class Interpreter : ExprVisitor<Any?>{
    override fun visitBinaryExpr(binary: Expr.Binary): Any? {
        val left = evaluate(binary.left)
        val right = evaluate(binary.right)


        // basic idea for arithmetic operation is
        // first we check if operands are number
        // if so we continue evaluation
        // if not we throw an exception and unwind
        return when(binary.operator.type){
            TokenType.MINUS -> {
                checkNumberOperands(binary.operator,left, right)
                (left as Double) - (right as Double)
            }
            // plus is a bit tricky
            // two possibility: 1 arithmetic operation; 2 string concatenation
            TokenType.PLUS -> {
                if (left as? Double != null && right as? Double != null){
                    // no cast is needed here because of the above check
                    left + right
                }
                else if (left as? String != null && right as? String != null){
                    left + right
                }
                else {
                    throw RuntimeError(binary.operator, "Operands must be two number or two String")
                }
            }
            TokenType.SLASH -> {
                checkNumberOperands(binary.operator,left,right)
                (left as Double) / (right as Double)
            }
            TokenType.STAR -> {
                checkNumberOperands(binary.operator,left,right)
                (left as Double) * (right as Double)
            }
            TokenType.BANG_EQUAL -> isEqual(left,right)
            TokenType.EQUAL_EQUAL -> isEqual(left,right)
            TokenType.GREATER -> {
                checkNumberOperands(binary.operator,left,right)
                (left as Double) > (right as Double)
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(binary.operator,left,right)
                (left as Double) >= (right as Double)
            }
            TokenType.LESS -> {
                checkNumberOperands(binary.operator,left,right)
                (left as Double) < (right as Double)
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(binary.operator,left, right)
                (left as Double) <= (right as Double)
            }
            else -> null
        }
    }

    /*
    recursively evaluate the subexpression within the parentheses and return it
     */
    override fun visitGroupingExpr(grouping: Expr.Grouping): Any? {
        return evaluate(grouping.expr)
    }
    /*
    recursively evaluate the subexpression within the unary expression
    handle the unary operator and return it
     */
    override fun visitUnaryExpr(unary: Expr.Unary): Any? {
        val right = evaluate(unary.right)

        return when(unary.operator.type){
            // negate the result of the subexpression
            // in which case the subexpression must be number
            TokenType.MINUS -> {
                // first we check if the expression is number
                checkNumberOperand(unary.operator,right)
                // since we have checked, we know the cast is always safe
                right as Double
            }
            TokenType.BANG -> !isTruthy(right)

            // should not reach
            else -> null
        }
    }

    /*
    Convert a literal tree node into a runtime value
     */
    override fun visitLiteralExpr(literal: Expr.Literal): Any? {
        return literal.value
    }

    /*
    helper method to send the expression back into the interpreter's visitor implementation
    if the expression is null, which is not going to happen
     */
    private fun evaluate(expr: Expr) : Any?{
        return expr.accept(this)
    }

    /*
    helper function: determine truthiness and falseness
    particularly useful when using something other than true or false in a logic operation
    for Lox in particular, false and nil are false, everything else is truthy.
     */
    private fun isTruthy(any: Any?) : Boolean{
        // safe cast to boolean, if error happens during parse, return null
        val value = any as? Boolean
        // if value is not null, return value, else return false
        return value ?: false
    }

    /*
    helper function: make sure that operand in unary expression is a number
    otherwise throw exception
     */
    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand as? Double != null) return
        throw RuntimeError(operator,"Operand must be a number")
    }

    /*
    helper function: make sure that operands of both sides in binary expression is a number
    otherwise throw exception
     */
    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left as? Double != null && right as? Double != null) return
        throw RuntimeError(operator,"Operand must be a number")
    }

    /*
    helper method: the author is using java, so by using a.equals(b), he wants to make sure that a is not null
    Otherwise he will get a null pointer exception
    In kotlin I think we are fine.
     */
    private fun isEqual(left: Any?, right: Any?) : Boolean{
        return left == right
    }
    /*
    helper method: essentially a pretty printer
     */
    private fun stringify(obj: Any?): String{
        if (obj == null) return "nil"

        // remove the .0 if obj is actually an integer
        if (obj as? Double != null){
            var text = obj.toString()
            if(text.endsWith(".0")){
               text = text.substring(0,text.length-2)
            }
            return text
        }
        return obj.toString()
    }

    /*
    public API: takes in a syntax tree for an expression and evaluates it.
    if succeeds, evaluate returns an object for the result value.
     */
    fun interpret(expression: Expr){
        try {
            val value = evaluate(expression)
            println(stringify(value))
        }
        catch (error : RuntimeError){
            Lox.runtimeError(error)
        }
    }
}