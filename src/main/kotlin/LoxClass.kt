// lox class handles behavior(methods) where an instance stores state.
class LoxClass(val name: String, val superclass: LoxClass?, val methods: Map<String, LoxFunction>) : LoxCallable {

    override fun arity(): Int {
        val initializer = findMethod("init")
        if (initializer == null) return 0
        return initializer.arity()
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance =  LoxInstance(this)
        //look for an init method
        val initializer = findMethod("init")
        // if we find one
        if (initializer != null){
            //immediately bind and invoke it like a normal method all
            initializer.bind(instance).call(interpreter,arguments)
        }
        return instance
    }

    override fun toString(): String {
        return name
    }

    fun findMethod(name: String) : LoxFunction?{
        if (methods.containsKey(name)){
            return methods[name]
        }
        // here we implicitly chained the findMethod function
        if(superclass!= null){
            return superclass.findMethod(name)
        }
        return null
    }
}