class LoxFunction(private val declaration: Stmt.FunctionStmt, val closure : Environment, val isInitializer: Boolean) : LoxCallable {
    fun bind(instance: LoxInstance) : LoxFunction{
        // create a new environment nestled inside the method's original closure
        val environment = Environment(closure)
        // declare "this" as a variable in that environment and bind it to the given instance
        environment.define("this", instance)
        // now the returned LoxFunction now carrries around its own persistent world where "this" is bound to the object.
        return LoxFunction(declaration, environment, isInitializer)
    }

    override fun arity(): Int {
        return declaration.params.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        //create a new environment which points to the outer environment
        val environment = Environment(closure)
        //walks the parameter and argument lists
        // for each pair, it creates a new variable with the parameter's name and binds it to the argument's value.
        declaration.params.forEachIndexed { index, token ->
            environment.define(token.lexeme, arguments[index])
        }
        // when catches a return exception, it pulls out the value and makes that return value from call()
        try {
            interpreter.executeBlock(declaration.body,environment)
        }catch (returnValue : Return){
            if (isInitializer) return closure.getAt(0,"this")
            return returnValue.value
        }
        // if the function is an initializer, we override the actual return value and forcibly return this.
        if (isInitializer) return closure.getAt(0,"this")
        // if it never catches one of these exception, it means the function reached the end of its body without hitting a return statement.
        // implicitly return null.
        return null
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }

}