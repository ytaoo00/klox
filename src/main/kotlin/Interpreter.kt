
// Here we use Any? because in lox we can possibly have null
class Interpreter : ExprVisitor<Any?>, StmtVisitor<Unit>{
    // hold a fixed reference to the global(outermost environment)
    val globals = Environment()
    // this is the current environment
    private var environment = globals

    /**
     * map that associates each expression node with its resolved data.
     */
    private var locals : MutableMap<Expr,Int> = mutableMapOf()


    constructor(){
        globals.define("clock",object :LoxCallable{
            override fun arity(): Int {
                return 0
            }

            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
                return System.currentTimeMillis() / 1000.0
            }

            override fun toString(): String {
                return "<native fn>"
            }

        })
    }

    /**
     * Sets the resolution information in interpreter.
     */
    fun resolve(expr: Expr, depth : Int){
        locals[expr] = depth
    }

    /**
     * Looks up the resolved distance in the map.
     */
    private fun lookUpVariable(name: Token, expr: Expr) : Any?{
        val distance = locals[expr]
        //resolved only local variables.
        //if a distance exists, a local variable exists
        return if (distance!= null){
            //take advantage of the results of our static analysis.
            environment.getAt(distance, name.lexeme)
        }
        //if we do not have a distance, we assume it is a global variable
        else{
            globals.get(name)
        }
    }

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
                    throw RuntimeError(binary.operator, "Operands must be two numbers or two strings.")
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
            TokenType.BANG_EQUAL -> !isEqual(left,right)
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
                -(right as Double)
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
        if (any as? Boolean != null){
            return any
        }
        if (any != null){
            return true
        }
        return false
    }

    /*
    helper function: make sure that operand in unary expression is a number
    otherwise throw exception
     */
    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand as? Double != null) return
        throw RuntimeError(operator,"Operand must be a number.")
    }

    /*
    helper function: make sure that operands of both sides in binary expression is a number
    otherwise throw exception
     */
    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left as? Double != null && right as? Double != null) return
        throw RuntimeError(operator,"Operands must be numbers.")
    }

    /*
    helper method: the author is using java, so by using a.equals(b), he wants to make sure that a is not null
    Otherwise he will get a null pointer exception
    In kotlin the == is compiled to equals, whereas === is equivalent of java's ==.
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
    public API: takes in list of statement syntax tree and evaluate them
     */
    fun interpret(statements: List<Stmt>){
        try {
            statements.forEach {
                execute(it)
            }
        }
        catch (error : RuntimeError){
            Lox.runtimeError(error)
        }
    }
    /*
    the statement analogue to the evaluate method
     */
    private fun execute(stmt: Stmt){
        stmt.accept(this)
    }

    /*
    Evaluate the expression in expression statement
     */
    override fun visitExpressionStmt(stmt: Stmt.ExpressionStmt) {
        evaluate(stmt.expr)
    }

    /*
    Evaluate the expression in the print statement tree.
    Print out the value get directly
     */
    override fun visitPrintStmt(stmt: Stmt.PrintStmt) {
        println(stringify(evaluate(stmt.expr)))
    }

    /**
     * forwards variable to the resolved variables
     */
    override fun visitVariableExpr(variable: Expr.Variable): Any? {
        return lookUpVariable(variable.name, variable)
    }



    /*
    If the variable has an initializer, we evaluate it.
    if not, set the variable to null.
     */
    override fun visitVarStmt(stmt: Stmt.VarStmt) {
        var value :Any? = null
        if (stmt.expr != null){
            value = evaluate(stmt.expr)
        }

        environment.define(stmt.name.lexeme, value)
    }

    /*
    similar idea as variable expression
     */
    override fun visitAssignExpr(assignment: Expr.Assignment): Any? {
        val value = evaluate(assignment.value)
        val distance = locals[assignment]
        if (distance != null){
            environment.assignAt(distance,assignment.name, value)
        }else{
            globals.assign(assignment.name, value)
        }

        return value
    }

    override fun visitBlockStmt(stmt: Stmt.BlockStmt) {
        executeBlock(stmt.statements, Environment(environment))
    }

    /*
    Manual change and discard environment
    if we are just about to enter to a block
    the current block will be outer
    when we enter the block, we created a new environment
    the new environment, which pointer to the outer environment
     */
    fun executeBlock(statements: List<Stmt>, environment: Environment){
        // we store a previous environment, which is the outer environment
        val previous = this.environment
        try {
            // switch the environment to the inner environment
            this.environment = environment

            // execute the statement inside the block
            statements.forEach {
                execute(it)
            }
        }finally {
            // once the execution of the statement inside the environment is done
            // we discard the inner environment and switch back to the outer environment
            this.environment = previous
        }

    }

    override fun visitIfStmt(stmt: Stmt.IfStmt) {
        if (isTruthy(evaluate(stmt.condition))){
            execute(stmt.thenBranch)
        }
        else if (stmt.elseBranch != null){
            execute(stmt.elseBranch)
        }
    }

    override fun visitLogicExpr(logic: Expr.Logical): Any? {
        // this is interesting.
        // first evaluate the left side
        val left = evaluate(logic.left)
        // if operator is OR
        if (logic.operator.type == TokenType.OR){
            // if left is true, we do not need to evaluate right side, since True or anything will be true
            if (isTruthy(left)) {
                return left
            }
        }
        // if operator is And
        else{
            // if left is not true, again we do not need to evaluate right, since False and anything will be False.
            if (!isTruthy(left)){
                return left
            }
        }
        // here the two case is, Or operator and left is false, therefore if right is true we return true if right is false we return false
        // And operator and left side is true, therefore again if right is true we return true if right is false we return false
        return evaluate(logic.right)
    }

    override fun visitWhileStmt(stmt: Stmt.WhileStmt) {
        while (isTruthy(evaluate(stmt.condition))){
            execute(stmt.statement)
        }
    }

    override fun visitCallExpr(call: Expr.Call): Any? {
        //get callee ready
        val callee = evaluate(call.callee)

        val arguments = mutableListOf<Any?>()
        // get each argument ready
        call.arguments.forEach {
            arguments.add(evaluate(it))
        }
        //safe type cast callee to Loxcallable
        if (callee as? LoxCallable == null) {
            throw RuntimeError(call.paren, "Can only call functions and classes.")
        }

        if (arguments.size != callee.arity()){
            throw RuntimeError(call.paren, "Expected ${callee.arity()} arguments but got ${arguments.size}.")
        }

        return callee.call(this, arguments)

    }


    override fun visitFunctionStmt(stmt: Stmt.FunctionStmt) {
        // take a function syntax node -- a compile time representation of the function-- and convert it to its runtime representation
        // we also pass in the environment variable
        // imaging in the context of block
        // this is going to be the outer environment
        val function = LoxFunction(stmt, environment, false)
        // here we create a new binding of the resulting object to a new variable in the current environment.
        environment.define(stmt.name.lexeme, function)
    }

    override fun visitReturnStmt(stmt: Stmt.ReturnStmt) {
        // here we initialize the return value to null
        var value : Any? = null
        // if the return value expression is not null, we evaluate it
        if (stmt.expr!= null){
            value = evaluate(stmt.expr)
        }
        // then we return the return value
        throw Return(value)
    }

    override fun visitClassStmt(stmt: Stmt.ClassStmt) {
        // first we check if this class has a super class
        var superclass: Any? = null
        if (stmt.superclass != null){
            superclass = evaluate(stmt.superclass)
            if (superclass as? LoxClass == null){
                throw RuntimeError(stmt.superclass.name, "Superclass must be a class.")
            }
        }
        //declare the class's name in the current environment.
        // we do not direct binding the name and the class reference because we need to use that reference in later stage.
        environment.define(stmt.name.lexeme,null)

        // create an new environment
        if (stmt.superclass != null){
            environment = Environment(environment)
            environment.define("super", superclass)
        }
        // turn the method AST into its runtime representation
        val methods : MutableMap<String, LoxFunction> = mutableMapOf()
        stmt.methods.forEach { method->
            // check if the method is init
            val function = LoxFunction(method,environment,method.name.lexeme == "init")
            methods[method.name.lexeme] = function
        }
        if (superclass != null){
            // if super class is not null, then it has an enclosing
            environment = environment.enclosing!!
        }

        // turn the class syntax node into a LoxClass, the runtime representation of a class
        val klass = LoxClass(stmt.name.lexeme, superclass as? LoxClass , methods)
        //circle back and store the class object in the variable we previously declared
        environment.assign(stmt.name, klass)

    }

    override fun visitGetExpr(getExpr: Expr.Get): Any? {
        // evaluate the expression whose property is being accessed
        val obj = evaluate(getExpr.obj)
        // if the object is a loxInstance
        if (obj as? LoxInstance != null){
            // look up the property
            return obj.get(getExpr.name)
        }
        // Lox only instances of class have properties
        throw RuntimeError(getExpr.name, "Only instances have properties.")
    }

    override fun visitSetExpr(setExpr: Expr.Set): Any? {
        val obj = evaluate(setExpr.obj)

        if (obj as? LoxInstance == null){
            throw RuntimeError(setExpr.name, "Only instances have fields.")
        }
        val value = evaluate(setExpr.value)
        obj.set(setExpr.name, value)
        return value
    }

    override fun visitThisExpr(thisExpr: Expr.This): Any? {
        return lookUpVariable(thisExpr.keyword, thisExpr)
    }
    // check get expression for reference
    override fun visitSuperExpr(superExpr: Expr.Super): Any? {
        val distance = locals[superExpr]
        val superclass = distance?.let { environment.getAt(it, "super") } as LoxClass
        val obj = environment.getAt((distance.minus(1)), "this") as LoxInstance
        val method = superclass.findMethod(superExpr.method.lexeme)
        if (method == null){
            throw RuntimeError(superExpr.method, "Undefined property '${superExpr.method.lexeme}'.")
        }
        return method.bind(obj)
    }
}