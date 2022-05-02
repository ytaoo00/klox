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
    The old get() method dynamically walks the chain of enclosing environments, but now we know exactly which environment in the chain will have the variable.
     */
    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance).values[name]
    }

    /*
    similar idea as getAt
     */
    fun assignAt(distance: Int, name: Token, value: Any?){
        ancestor(distance).values[name.lexeme] = value
    }

    /*
    walks a fixed number of hops up the parent chain and returns the environment there.
     */
    fun ancestor(distance: Int) : Environment {
        var environment : Environment= this
        for (i in 0 until distance){
            // should never reach to null
            // because we only deal with local variable
            // at the outermost local scope with have its environment points to global
            environment = environment.enclosing!!
        }
        // we do not have to check if the variable is there, we know it will be because the resolver already found it before.
        return environment
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
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    /*
    Assignment  not allowed creation of a new variable
     */
    fun assign(name: Token, value: Any?){
        if(values.containsKey(name.lexeme)){
            values[name.lexeme] = value
            return
        }
        if (enclosing != null){
            return enclosing.assign(name, value)
        }
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
}