// again we applied the visitor pattern
// just as a recap
// the interface contains a list of function
// which will be called from the appropriate statement class by its corresponding accept function.
// the visitor function will be overrided and implemented in the appropriate parsing and interpret fun
interface StmtVisitor<R>{
    fun visitExpressionStmt(stmt: Stmt.ExpressionStmt) : R
    fun visitPrintStmt(stmt: Stmt.PrintStmt) : R
    fun visitVarStmt(stmt: Stmt.VarStmt) : R
    fun visitBlockStmt(stmt: Stmt.BlockStmt) : R
    fun visitIfStmt(stmt: Stmt.IfStmt) : R
    fun visitWhileStmt(stmt: Stmt.WhileStmt) : R
    fun visitFunctionStmt(stmt: Stmt.FunctionStmt) : R
    fun visitReturnStmt(stmt: Stmt.ReturnStmt) : R
    fun visitClassStmt(stmt: Stmt.ClassStmt) : R
}

// essentially an abstract class
// with one function called accept
// each implementation of accept will take in the visitor
// pass to the visitor the current statement
sealed class Stmt {
    abstract fun <R> accept(visitor: StmtVisitor<R>) : R

    /**
     * Statement Expression AST (expression;)
     * @property expr Expression AST
     */
    class ExpressionStmt(val expr: Expr):Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitExpressionStmt(this)
        }
    }

    /**
     * Print Statement AST (print expression;)
     * @param expr Expression AST
     */
    class PrintStmt(val expr: Expr):Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitPrintStmt(this)
        }
    }

    /**
     * Variable Declaration AST (VAR name = initializer)
     *
     * @property name The token with type VAR
     * @property expr Initializer
     */
    class VarStmt(val name : Token, val expr : Expr?) : Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitVarStmt(this)
        }
    }

    /**
     * Block statement AST ({ statement_one statement_two ... })
     */
    class BlockStmt(val statements: List<Stmt>) : Stmt() {
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitBlockStmt(this)
        }
    }

    /**
     * If statement AST (IF ( condition ) thenBranch ELSE elseBranch)
     */
    class IfStmt(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>) : R{
            return visitor.visitIfStmt(this)
        }
    }

    /**
     * While statement AST (WHILE ( condition ) body)
     */
    class WhileStmt(val condition: Expr, val statement: Stmt): Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitWhileStmt(this)
        }
    }

    /**
     * Function statement AST (name ( param_one, param_two, ...) { body })
     */
    class FunctionStmt(val name: Token, val params: List<Token>, val body: List<Stmt>) : Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitFunctionStmt(this)
        }
    }

    /**
     * Return statement AST (keyword value)
     */
    class ReturnStmt(val keyword: Token, val expr: Expr?):Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitReturnStmt(this)
        }
    }

    /**
     * Class statement (CLASS name < superclass { methodOne, methodTwo,...})
     */
    class ClassStmt(val name: Token, val superclass: Expr.Variable?, val methods : List<FunctionStmt>) : Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitClassStmt(this)
        }

    }
}