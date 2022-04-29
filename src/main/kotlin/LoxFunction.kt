class LoxFunction(val declaration: Stmt.FunctionStmt, val closure : Environment) : LoxCallable {
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
            return returnValue.value
        }
        // if it never catches one of these exception, it means the function reached the end of its body without hitting a return statement.
        // implicitly return null.
        return null
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme} >"
    }

}