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
}