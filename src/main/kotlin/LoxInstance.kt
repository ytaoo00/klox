class LoxInstance(val klass : LoxClass) {

    //map to store property
    private val fields : MutableMap<String, Any?> = mutableMapOf()

    override fun toString(): String {
        return klass.name + " instance"
    }

    fun get(name: Token) : Any?{
        // handles get fields
        if (fields.containsKey(name.lexeme)){
            return fields[name.lexeme]
        }

        //handles get method
        val method = klass.findMethod(name.lexeme)
        if (method!= null) return method.bind(this)

        throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
    }

    fun set(name: Token, value: Any?){
//        fields.put(name.lexeme, value)
        fields[name.lexeme] = value
    }
}