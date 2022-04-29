// visitor pattern
// here we need to add in the generic type as well
// since functions can return different type
interface ExprVisitor<R>{
    fun visitBinaryExpr(binary: Expr.Binary) : R
    fun visitGroupingExpr(grouping: Expr.Grouping) : R
    fun visitUnaryExpr(unary: Expr.Unary) : R
    fun visitLiteralExpr(literal: Expr.Literal) : R
    fun visitVariableExpr(variable: Expr.Variable) : R
    fun visitAssignExpr(assignment: Expr.Assignment) : R
    fun visitLogicExpr(logic: Expr.Logical) : R
    fun visitCallExpr(call: Expr.Call) : R
}

//we do not need the abstract class here because a sealed class is abstract by itself
sealed class Expr{
    //generic function, place type parameters before the name of the function
    abstract fun <R> accept(visitor: ExprVisitor<R>) : R


    class Binary(val left : Expr, val operator: Token, val right: Expr) : Expr() {
        override fun <R> accept(visitor: ExprVisitor<R>): R {
            return visitor.visitBinaryExpr(this)
        }
    }
    class Grouping(val expr: Expr) : Expr() {
        override fun <R> accept(visitor: ExprVisitor<R>): R {
            return visitor.visitGroupingExpr(this)
        }
    }

    class Unary(val operator: Token, val right : Expr) : Expr() {
        override fun <R> accept(visitor: ExprVisitor<R>): R {
            return visitor.visitUnaryExpr(this)
        }

    }

    // here since we can have a string that is null
    // we need to explicitly define value to nullable
    class Literal(val value : Any?) : Expr(){
        override fun <R> accept(visitor: ExprVisitor<R>): R {
            return visitor.visitLiteralExpr(this)
        }
    }

    //here we treat variable name as an expression
    class Variable(val name: Token) : Expr(){
        override fun <R> accept(visitor: ExprVisitor<R>): R {
            return visitor.visitVariableExpr(this)
        }
    }

    //
    class Assignment(val name: Token, val value : Expr) : Expr(){
        override fun <R> accept(visitor: ExprVisitor<R>): R {
            return visitor.visitAssignExpr(this)
        }
    }

    class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr(){
        override fun <R> accept(visitor: ExprVisitor<R>): R {
            return visitor.visitLogicExpr(this)
        }
    }

    //we need the closing parenthesis as this helps us use that token's location when we report a runtime error caused by a function call.
    class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr(){
        override fun <R> accept(visitor: ExprVisitor<R>): R {
            return visitor.visitCallExpr(this)
        }
    }
}

