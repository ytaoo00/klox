class AstPrinter : ExprVisitor<String>, StmtVisitor<String> {
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

    override fun visitVariableExpr(variable: Expr.Variable): String {
        return variable.toString()
    }

    override fun visitAssignExpr(assignment: Expr.Assignment): String {
        return parenthesize(assignment.name.lexeme, assignment.value )
    }

    override fun visitLogicExpr(logic: Expr.Logical): String {
        return parenthesize(logic.operator.lexeme, logic.left,logic.right)
    }

    override fun visitExpressionStmt(stmt: Stmt.ExpressionStmt): String {
        TODO("Not yet implemented")
    }

    override fun visitPrintStmt(stmt: Stmt.PrintStmt): String {
        TODO("Not yet implemented")
    }

    override fun visitVarStmt(stmt: Stmt.VarStmt): String {
        TODO("Not yet implemented")
    }

    override fun visitBlockStmt(stmt: Stmt.BlockStmt): String {
        TODO("Not yet implemented")
    }

    override fun visitIfStmt(stmt: Stmt.IfStmt): String {
        TODO("Not yet implemented")
    }

    override fun visitWhileStmt(stmt: Stmt.WhileStmt): String {
        TODO("Not yet implemented")
    }

    override fun visitCallExpr(call: Expr.Call): String {
        TODO("Not yet implemented")
    }

    override fun visitFunctionStmt(stmt: Stmt.FunctionStmt): String {
        TODO("Not yet implemented")
    }

    override fun visitReturnStmt(stmt: Stmt.ReturnStmt): String {
        TODO("Not yet implemented")
    }

    override fun visitGetExpr(getExpr: Expr.Get): String {
        TODO("Not yet implemented")
    }

    override fun visitSetExpr(setExpr: Expr.Set): String {
        TODO("Not yet implemented")
    }

    override fun visitClassStmt(stmt: Stmt.ClassStmt): String {
        TODO("Not yet implemented")
    }

    override fun visitThisExpr(thisExpr: Expr.This): String {
        TODO("Not yet implemented")
    }

    override fun visitSuperExpr(superExpr: Expr.Super): String {
        TODO("Not yet implemented")
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