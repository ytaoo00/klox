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
    class ExpressionStmt(val expr: Expr):Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitExpressionStmt(this)
        }
    }
    class PrintStmt(val expr: Expr):Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitPrintStmt(this)
        }
    }

    class VarStmt(val name : Token, val expr : Expr?) : Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitVarStmt(this)
        }
    }

    class BlockStmt(val statements: List<Stmt>) : Stmt() {
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitBlockStmt(this)
        }
    }

    class IfStmt(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>) : R{
            return visitor.visitIfStmt(this)
        }
    }

    class WhileStmt(val condition: Expr, val statement: Stmt): Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitWhileStmt(this)
        }
    }

    class FunctionStmt(val name: Token, val params: List<Token>, val body: List<Stmt>) : Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitFunctionStmt(this)
        }
    }

    class ReturnStmt(val keyword: Token, val expr: Expr?):Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitReturnStmt(this)
        }
    }

    class ClassStmt(val name: Token, val superclass: Expr.Variable?, val methods : List<FunctionStmt>) : Stmt(){
        override fun <R> accept(visitor: StmtVisitor<R>): R {
            return visitor.visitClassStmt(this)
        }

    }
}