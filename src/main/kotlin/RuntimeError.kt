// here two parameter are needed for the runtime error class
// also since runtimeError has a primary constructor, need to ini
class RuntimeError(_token: Token, message: String) : RuntimeException(message){
    // add token property
    val token: Token
    // initialize token property
    init {
        this.token = _token
    }
}