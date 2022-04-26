# Chapter Four Notes
Goal is to evaluate an expression and produce a value. <br>
For each kind of expression syntax we can parse, we need a corresponding code to evaluate that tree and produce a result. <br>
Two questions:
1. What kinds of value do we produce?
2. How do we organize those code?

## Representing the value
A variable in Lox can store a value of any Lox type, and can even store values of different types at different points in time.<br>
In the book, the author uses java to implement lox. <br>
Given a java variable with that static type, we must also be able to determine which kind of value it holds at runtime.<br>
For example, given a + operation, the interpreter needs to tell if it is adding two numbers or concatenating two strings.<br>
Therefore the author decide to treat any Lox value as java.lang.Object. <br>
Kotlin is interesting because it requires explicit syntax on nullability. <br>
Kotlin treats some Java types specifically. Such types are not loaded from Java "as is", but are mapped to corresponding Kotlin types. The mapping only matters at compile time, the runtime representation remains unchanged.<br>

|   Lox type    | Java representation | Kotlin representation |
|:-------------:|:-------------------:|:---------------------:|
| Any Lox value |       Object        |      kotlin.Any!      |
|      nil      |        null         |         null          |
|    Boolean    |       Boolean       |    kotlin.Boolean?    |
|    number     |       Double        |    kotlin.Double?     |
|    string     |       String        |    kotlin.String!     |
````
Curious to learn more about the design idea behind this mapping.
Why some types are mapped to platform types while others get mapped to nullable.
````

## A deeper look into platform types
The type names or class names ending with single exclamation mark ! are called platform types in Kotlin. You find them when you are working in Kotlin with old Java code that doesn't contain nullability information.<br>
Examples:
- Nullable Information: Nullable Type<br>
``@Nullable String`` in Java is considered as ``String?`` by Kotlin.
- Non-null Information: Non-null Type<br>
``@NotNull String`` in Java is considered as ``String`` by Kotlin.
- No Information: Platform Type<br>
``String`` without annotations in Java is considered as ``String!`` by Kotlin.<br>
Note that you can't declare platform types in Kotlin code, they come only from Java code.<br>

## Evaluating Expressions
In the evaluating phase, we continue with our Visitor pattern. <br>
See the code for detail.

## Play around with Kotlin Null safety
Again, the question market followed by variable type allows null.<br>
````
var a : String = "abc" // non-null
a = null // compilation error

a.length // guaranteed not to cause an Null Pointer Exception

var b : String? = "abc" // can be null
b = null // ok

b.length // error: variable 'b' can be null
````
So, how can we safely access the properties of b? <br>
1. Checking for null in condition
````
if (b!= null) b.length else -1
````
2. Safe calls(?.)
``b?.length`` this returns b.length if b is not null, and null otherwise.
3. Elvis operator(?:)
This is really just syntax sugar for the if else expression. ``b?.length ?: -1``.<br>
if the expression to the left of ?: is not null, the Elvis operator returns it, otherwise it returns the expression to the right.
4. The not-null assertion operator converts any value to a non-null type and throws an exception if the value is null.<br>
``b!!.length`` first convert b to a non-null value of b or throw an NPE if b is null.<br>

### Safe casts
Regular casts may result in a ClassCastException if the object is not of the target type.<br>
Another option is to use safe casts that return null if the attempt was not successful.<br>
For example, ``a as? Int``<br>

## Kotlin Exception
An error was encountered when inheriting the kotlin runtimeException.<br>
````
//ask to override the message
class RuntimeError(val message: String) : RuntimeException()
````
and when removing the val keyword, the error goes way. <br>
Without the val keyword, we say that message is a **parameter**. <br>
With the val keyword, we declared the **property**. <br>
Looking in to the source code.<br>
````
public expect open class RuntimeException : Exception {
    constructor()
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
    constructor(cause: Throwable?)
}

public actual open class RuntimeException : Exception {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
    public actual constructor(cause: Throwable?) : super(cause)
}

public expect open class Exception : Throwable {
    constructor()
    constructor(message: String?)
    constructor(message: String?, cause: Throwable?)
    constructor(cause: Throwable?)
}

public actual open class Exception : Throwable {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
    public actual constructor(cause: Throwable?) : super(cause)
}

public open class Throwable(open val message: String?, open val cause: Throwable?) {
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(cause?.toString(), cause)
    constructor() : this(null, null)
}
````
We see that the base class here is "Throwable", which has a primary constructor.<br>
One of the parameter here is message, which the open keyword, which means it can be override with the annotation.<br>
Notice that since Throwable has a primary constructor, each secondary constructor needs to delegate to the primary constructor.<br>
Either directly or indirectly through another secondary constructor. <br>
Delegation to another constructor of the same class is done using ``this`` keyword.
Coming to Exception, first of all, ignore the ``expect`` keyword for now, and focus on the part with the ``actual`` keyword.<br>
This class has no primary constructor, then each secondary constructor has to initialize the base type using the ``super`` keyword or it has to delegate to another constructor which does. Note that in this case different secondary constructors can call different constructors of the base type.<br>
On the other hand, If the derived class has a primary constructor, the base class can (and must) be initialized in that primary constructor according to its parameters.<br>
Now for example, when you do ``throw Exception("Exception here")``, essentially you pass in a parameter to the second constructor, which calls the first constructor in throwable.<br>
Going back to ``class RuntimeError(val message: String) : RuntimeException()``<br>
This is essentially redeclare the "message" property. Removing the val keyword, the "message" is just a parameter.<br>