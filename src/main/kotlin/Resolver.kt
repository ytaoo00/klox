class Resolver(private val interpreter: Interpreter): ExprVisitor<Unit>, StmtVisitor<Unit> {
    enum class FunctionType{
        NONE, Function, METHOD, INITIALIZER
    }

    enum class ClassType{
        NONE, CLASS, SUBCLASS
    }

    /**
     * Tracks the stack of scopes currently in scope. Each element in the stack is a Map representing a single block scope. Keys are variable names, values are boolean indicating if the variable has been successfully resolved.
     */
    private val scopes : ArrayDeque<MutableMap<String,Boolean>> = ArrayDeque()
    // track whether or not the code we are currently visiting is inside a function declaration
    private var currentFunction : FunctionType = FunctionType.NONE
    private var currentClass : ClassType = ClassType.NONE

    /**
     * Creates a new scope
     */
    private fun beginScope(){
        scopes.addLast(mutableMapOf())
    }

    /**
     * Exits from the innermost scope.
     */
    private fun endScope(){
        scopes.removeLast()
    }

    /**
     * Passes the resolution information(how many scopes there are between the current scope and the scope where the variable is defined) to interpreter.
     * @param expr The AST for expression for assignment.
     * @param name The name of the targeted variable.
     */
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

    /**
     * Resolves a list of statements AST one at a time.
     */
    fun resolve(statements : List<Stmt>){
        statements.forEach {
            resolve(it)
        }
    }

    /**
     * Initializes the resolution of a single statement AST.
     */
    private fun resolve(statement: Stmt){
        statement.accept(this)
    }

    /**
     * Initializes the resolution of an expression AST.
     */
    private fun resolve(expr: Expr){
        expr.accept(this)
    }


    /**
     * Adds the variable to the innermost scope so that it shows any outer one; Marks the variable so that Resolver knows the variable exists
     */
    private fun declare(name: Token){
        // Do not resolve global variable
        if (scopes.isEmpty()) return
        // get the innermost scope
        val scope = scopes.last()
        // records error of double declaration
        if (scope.containsKey(name.lexeme)){
            Lox.error(name, "Already a variable with this name in this scope.")
        }
        //marks the existence of the variable, since the resolver have not yet see the initializer, mark the variable as not ready yet
        scope[name.lexeme] = false
    }

    /**
     * Sets the variable's value in the scope map to true to mark it as fully initialized and available for use.
     */
    private fun define(name: Token){
        if (scopes.isEmpty()) return
        scopes.last()[name.lexeme] = true
    }

    /**
     * Introduces a new scope for the statements inside the BlockStatement
     */
    override fun visitBlockStmt(stmt: Stmt.BlockStmt) {
        // begin a new scope
        beginScope()
        // traverse into the statement inside the block
        resolve(stmt.statements)
        // discards the scope
        endScope()
    }

    /**
     * A variable declaration adds a new variable to the current(innermost) scope.
     * @see [Stmt.VarStmt]
     */
    override fun visitVarStmt(stmt: Stmt.VarStmt) {
        // record the existence of a variable
        declare(stmt.name)
        // meet the initializer
        if (stmt.expr != null){
            resolve(stmt.expr)
        }
        // finish the binding of the variable
        define(stmt.name)
    }

    /**
     * Resolves variables for variable expression
     */
    override fun visitVariableExpr(variable: Expr.Variable) {
        //check if the variable is being access inside its own initializer. Ex(var a = a)
        // that means a variable exists in the current scope but the value in the map is false
        if (!scopes.isEmpty() && scopes.last()[variable.name.lexeme] == false){
            Lox.error(variable.name, "Can't read local variable in its own initializer.")
        }
        // resolves the variable
        resolveLocal(variable, variable.name)
    }

    /**
     * Resolves variables for assignment expression
     */
    override fun visitAssignExpr(assignment: Expr.Assignment) {
        // resolve the expression for the assigned value in case it also contains reference to other variables
        resolve(assignment.value)
        // resolve variable that is being assigned to.
        resolveLocal(assignment, assignment.name)
    }

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
        // one small edge case is the class inherited from itself, we want to prohibit it.
        if (stmt.superclass != null && stmt.name.lexeme == stmt.superclass.name.lexeme){
            Lox.error(stmt.superclass.name, "A class can't inherit from itself.")
        }
        // resolve superclass if not null
        if (stmt.superclass != null){
            currentClass = ClassType.SUBCLASS
            resolve(stmt.superclass)
        }
        // if the class declaration has a superclass, then we create a new scope surrounding all of its methods.
        if (stmt.superclass != null){
            beginScope()
            scopes.last().put("super", true)
        }

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
        //once we are done resolving the class's method, we discard that scope
        if (stmt.superclass != null){
            endScope()
        }
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

    // resolve the super token exactly as if it were a variable
    // the resolution stores the number of hops along the environment chain that the interpreter needs to walk to find the environment where the superclass is stored.
    override fun visitSuperExpr(superExpr: Expr.Super) {
        if (currentClass == ClassType.NONE){
            Lox.error(superExpr.keyword, "Can't use 'super' outside of a class.")
        }
        else if (currentClass != ClassType.SUBCLASS){
            Lox.error(superExpr.keyword, "Can't use 'super' in a class with no superclass.")
        }
        resolveLocal(superExpr, superExpr.keyword)
    }
}