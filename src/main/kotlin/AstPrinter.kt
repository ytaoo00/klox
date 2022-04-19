class AstPrinter : ExprVisitor<String> {
    override fun visitBinaryExpr(binary: Expr.Binary): String {
        return parenthesize(binary.operator.lexeme, binary.left, binary.right)
    }

    override fun visitGroupingExpr(grouping: Expr.Grouping): String {
        return parenthesize("group", grouping.expr)
    }

    override fun visitUnaryExpr(unary: Expr.Unary): String {
        return parenthesize(unary.operator.lexeme, unary.right)
    }

    override fun visitLiteralExpr(literal: Expr.Literal): String {
        if (literal.value == null) return "nil";
        return literal.value.toString()
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val string = buildString {
            append("(")
            append(name)
            exprs.forEach{ expr ->
                append(" ")
                // If this has no qualifiers, it refers to the innermost enclosing scope. To refer to this in other scopes, label qualifiers are used:
                // so in this case, this refers to the StringBuilder
                append(expr.accept(this@AstPrinter))
            }
            append(")")
        }
        return string
    }

    fun print(expr : Expr) :String{
        return expr.accept(this)
    }
}

fun main() {
    val expr = Expr.Binary(
        Expr.Unary(
            Token(TokenType.MINUS, "-", null,1),
            Expr.Literal(123)
        ),
        Token(TokenType.STAR, "*", null, 1),
        Expr.Grouping(
            Expr.Literal(45.67)
        )
    )
    print(AstPrinter().print(expr))
}