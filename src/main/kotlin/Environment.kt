class Environment {
    private val enclosing: Environment?

    //constructor for global scope's environment, which ends the chain
    constructor() {
        this.enclosing = null
    }
    //constructor creating a new local scope nested inside the given outer one.
    constructor(enclosing : Environment){
        this.enclosing = enclosing
    }


    private val values: MutableMap<String, Any?> =  mutableMapOf<String, Any?>()
    /*
        support binding from a name to a value
        if the name already exists, replace the old value with the new value
     */
    fun define(name:String, value: Any?){
        values[name] = value
    }

    /*
    given a token, extract the variable name, return the value
    if no key is found, throw a exception at runtime
     */
    fun get(name: Token):Any?{
        if(values.containsKey(name.lexeme)){
            return values[name.lexeme]
        }
        //safe call to get the variable from outer scope
        if (enclosing != null){
            return enclosing.get(name)
        }
        throw  RuntimeError(name, "Undefined variable \"${name.lexeme}\".")
    }

    /*
    Assignment is not allowed to create a new variable
     */
    fun assign(name: Token, value: Any?){
        if(values.containsKey(name.lexeme)){
            values[name.lexeme] = value
        }
        if (enclosing != null){
            return enclosing.assign(name, value)
        }
        throw RuntimeError(name, "Undefined variable ${name.lexeme}. ")
    }
}