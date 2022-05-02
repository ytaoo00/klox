class Resolver(val interpreter: Interpreter): ExprVisitor<Unit>, StmtVisitor<Unit> {
    enum class FunctionType{
        NONE, Function, METHOD, INITIALIZER
    }

    enum class ClassType{
        NONE, CLASS
    }

    private val scopes : ArrayDeque<MutableMap<String,Boolean>> = ArrayDeque()
    // track whether or not the code we are currently visiting is inside a function declaration
    private var currentFunction : FunctionType = FunctionType.NONE
    private var currentClass : ClassType = ClassType.NONE

    override fun visitBinaryExpr(binary: Expr.Binary) {
        resolve(binary.left)
        resolve(binary.right)
    }

    override fun visitGroupingExpr(grouping: Expr.Grouping) {
        resolve(grouping.expr)
    }

    override fun visitUnaryExpr(unary: Expr.Unary) {
        resolve(unary.right)
    }

    override fun visitLiteralExpr(literal: Expr.Literal) {
        return
    }

    override fun visitVariableExpr(variable: Expr.Variable) {
        // if the variable exists in the current scope but its value is false,
        // it means we have declared it but not yet defined it
        if (!scopes.isEmpty() && scopes.last()[variable.name.lexeme] == false){
            Lox.error(variable.name, "Can't read local variable in its own initializer.")
        }

        resolveLocal(variable, variable.name)
    }

    private fun resolveLocal(expr: Expr, name: Token){
        // start at the innermost scope and work outwards, looking in each map for a matching name
        for (i in (scopes.size -1) downTo 0){
            // if we find the variable
            if (scopes[i].containsKey(name.lexeme)) {
                //we resolve it, passing in the number of scope between the current innermost scope and the scope where the variable was found
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    override fun visitAssignExpr(assignment: Expr.Assignment) {
        // resolve the expression for the assigned value in case it also contains reference to other variables
        resolve(assignment.value)
        // resolve variable that is being assigned to.
        resolveLocal(assignment, assignment.name)
    }

    override fun visitLogicExpr(logic: Expr.Logical) {
        resolve(logic.left)
        resolve(logic.right)
    }

    override fun visitCallExpr(call: Expr.Call) {
        resolve(call.callee)
        call.arguments.forEach {
            resolve(it)
        }
    }

    override fun visitExpressionStmt(stmt: Stmt.ExpressionStmt) {
        resolve(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.PrintStmt) {
        resolve(stmt.expr)
    }

    override fun visitVarStmt(stmt: Stmt.VarStmt) {
        declare(stmt.name)
        if (stmt.expr != null){
            resolve(stmt.expr)
        }
        define(stmt.name)
    }

    private fun declare(name: Token){
        if (scopes.isEmpty()) return
        val scope = scopes.last()
        if (scope.containsKey(name.lexeme)){
            Lox.error(name, "Already a variable with this name in this scope.")
        }

        scope[name.lexeme] = false
    }

    private fun define(name: Token){
        if (scopes.isEmpty()) return
        scopes.last()[name.lexeme] = true
    }

    // Block statement introduces a new scope for the statement it contains
    override fun visitBlockStmt(stmt: Stmt.BlockStmt) {
        // begin a new scope
        beginScope()
        // traverse into the statement inside the block
        resolve(stmt.statements)
        // discards the scope
        endScope()
    }

    fun resolve(statements : List<Stmt>){
        statements.forEach {
            resolve(it)
        }
    }

    fun resolve(statement: Stmt){
        statement.accept(this)
    }

    fun resolve(expr: Expr){
        expr.accept(this)
    }

    fun beginScope(){
        scopes.addLast(mutableMapOf())
    }

    fun endScope(){
        scopes.removeLast()
    }

    override fun visitIfStmt(stmt: Stmt.IfStmt) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch!= null) resolve(stmt.elseBranch)
    }

    override fun visitWhileStmt(stmt: Stmt.WhileStmt) {
        resolve(stmt.condition)
        resolve(stmt.statement)
    }

    override fun visitFunctionStmt(stmt: Stmt.FunctionStmt) {
        // to bind the name of the function in the surrounding scope where the function is declared.
        declare(stmt.name)
        define(stmt.name)
        // bind the function parameters into the inner function scope
        // recursively refer to itself inside its own body
        resolveFunction(stmt, FunctionType.Function)
    }

    private fun resolveFunction(function : Stmt.FunctionStmt, type: FunctionType){
        // store the previously visited function type before we enter the function
        val enclosingFunction = currentFunction
        // change the current function type to type
        currentFunction = type
        beginScope()
        function.params.forEach {
            declare(it)
            define(it)
        }
        resolve(function.body)
        endScope()
        // the current visit is done, we are going back to the previously function type
        currentFunction = enclosingFunction
    }

    override fun visitReturnStmt(stmt: Stmt.ReturnStmt) {
        if (currentFunction == FunctionType.NONE){
            Lox.error(stmt.keyword, "Can't return from top-level code.")
        }

        if (stmt.expr != null){
            if (currentFunction == FunctionType.INITIALIZER){
                Lox.error(stmt.keyword,"Can't return a value from an initializer.")
            }
            resolve(stmt.expr)
        }
    }

    // it is a bit weird, but lox allows class to be treated as local variable
    override fun visitClassStmt(stmt: Stmt.ClassStmt) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS
        // resolve the function name
        declare(stmt.name)
        define(stmt.name)
        // what we want to do is whenever a this expression is encoutnered, it will resolve to a "local variable" defined in an implicit scope just outside of the block for the method body.
        // we push a new scope
        beginScope()
        //define "this" in the new scope as if it were a variable
        scopes.last()["this"] = true
        // resolve method
        stmt.methods.forEach { method ->
            var declaration = FunctionType.METHOD
            if (method.name.lexeme == "init"){
                declaration = FunctionType.INITIALIZER
            }
            resolveFunction(method, declaration)
        }
        endScope()
        currentClass = enclosingClass
    }

    // since properties are looked up dynamically, they don't get resolved. During resolution, we recurse only into the expression to the left of the dot.
    // the actual property access happens in the interpreter.
    override fun visitGetExpr(getExpr: Expr.Get) {
        resolve(getExpr.obj)
    }

    override fun visitSetExpr(setExpr: Expr.Set) {
        resolve(setExpr.obj)
        resolve(setExpr.value)
    }

    /*
    resolve this as a local variable
     */
    override fun visitThisExpr(thisExpr: Expr.This) {
        if (currentClass == ClassType.NONE){
            Lox.error(thisExpr.keyword, "Can't use 'this' outside of a class.")
            return
        }
        resolveLocal(thisExpr, thisExpr.keyword)
    }
}