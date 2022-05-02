# Classes

## Object-Oriented Programming
There are actually three broad paths to object-oriented programming: classes, prototypes, and multimethod.<br>
Of course the classical one is the class approach, which is what we will focus here.<br>
The main goal of OOP is to bundle data with the code that acts on it.<br>
To achieve so, we have classes which can:
1. Exposes a constructor to create and initialize new instances of the class.
2. Provides a way to store and access fields on instances.
3. Defines a set of methods shared by all instances of the class that operate on each instance's state. 

So: On one path, class provides constructor, constructor creates instances, instance stores fields. On the other path, class defines methods, and methods access those fields.<br>

## Grammar
A class statement introduces a new name, so it lives in the declaration on grammar rule.
````
declaration -> classDecl | funDecl | varDecl | statement;

classDecl   -> "class" IDENTIFIER "{" function* "}" ;

function    -> IDENTIFIER "(" parameter? ")" block ;

parameter   -> IDENTIFIER ( "," IDENTIFIER )* ;
````
Here notice that inside the class body is a list of method declarations. Unlike function declarations, methods do not have a leading fun keyword. Each method is a name, parameter list, and body. Which allows us to use the function grammar.<br>
Like other dynamically typed languages, fields are not explicitly listed in the class declaration. Instances are loose bags of data and you can freely add fields to them as you see fit using normal imperative code.<br>
To implement the class grammar, we have to visit parser, resolver, and interpreter. Just like implementing function, we create another instance since they are slightly more complicated.<br>

## Creating instance
Remember when dealing with function, we have function declaration and function calls. In the case of class, we also have class declaration and well, initialization of instance.<br> 
Notice that Lox does not support "static" method one can just call on the class itself. <br>
In Lox, since we already have class objects, and already have function calls, we will use call expression on class objects to create new instances.<br>
It is as if a class is a factory function that generates instances of itself. so we do not have to introduce new grammar that includes syntax like `new`.<br>
Like we need a runtime representation of classes and functions, we also need a runtime representation of instances. <br>
There are two things in associated with each instance, one is method, the other is property. <br>
First we work on property. <br>
### Getter
In lox, every instance is an open collection of named values. Methods on the instance's class can access and modify properties, but so can outside code. Properties are accessed using a `.` syntax.<br>
For example, ``someObject.someProperty``.<br>
For this, we do need to define a new grammar. <br>
The dot operation has the same precedence as the parentheses in a function call expression. <br>
``call -> primary ( "(" arugment? ")" | "." IDENTIFIER )*``<br>
This grammar means, after a primary expression, we allow a series of any mixture of parenthesized calls and dotted property access, or as the author defines, "get expressions".<br>
so something like ``A()().a``is totally legit. <br>
Notice that we can staff the two operation in one grammar rule to easily demonstrate the fact that they are at the same precedence level.<br>
However, we do need another AST to represent the node. When parsing, we just use the same function to handle the two cases, and generated the correct nested tree.<br>
One small thing to notice, the author switch from properties to field in different place.<br>
There is a subtle difference between the two. Fields are named bits of ``state`` stored directly in an instance. Properties are the named "things". that a get expression may return.<br>
Every field is a property, but not every property is a field. <br>
Per my understanding, methods can be a property of a class by definition, yet method can not be access by getter or setter.<br>
This is essentially a getter, and once we get the getter done, we have to worry about the setter.<br>
### Setter
The format for setter is ``someObject.someProperty = value;``.<br>
Grammar is ``assignment -> ( call "." )? IDENTIFIER "=" assignment | logic_or``<br>
Unlike getter, setter does not chain. However, the reference to call allows any high-precedence expression before the last dot.<br>
For example: ``breakfast.omelette.filling.meat = ham``<br>
Note this grammar allows to correctly identify that only the last part the ``.meat``is the setter. <br>
### Method
Notice that our parser already support method calls, for example ``object.method(argument)``. It is a chained call of `.`getter and `()` function calls. <br>
So we can go ahead implement the basic method calls.<br>
However, consider the following:
````
class Person{
    sayName() {
        print this.name;
    }
}

var jane = Person();
jane.name = "Jane";

var bill = Person();
bill.name = "Bill";

bill.sayName = jane.sayName;
bill.sayName() //????????
````
The idea is, the last lane should have return "Jane". Because it is the instance where we first grabbed the method. <br>
To do so, we need to know which callable things are methods and which are function.<br>
In other word, we will have methods "bind" `this` to the original instance when the method is first grabbed.<br>

### This
Right now, the class's handles the behavior(methods) and the instances store states(fields).<br>
There is no way to access the fields of the "current" object, nor can we call other methods on that same object.<br>
Therefore, we use `this` keyword to get that "current" instance. <br>
Inside a method body, a `this` expression evaluates to the instance that the method was called on. Or more specifically, since methods are accessed and then invoked as two steps, it will refer to the object that method was accessed from.<br>
For example: 
````
class Egotist{
    speak(){
        print this;
    }
var method = Egotist().speak; // we grab a reference to the speak() method off an instance of the class, return a function
method()// called the function, also that function needs to remember the instance it was pulled off, and find that instance when called.
````
We need to take ``this`` at the point that the method is accessed and attach it to the function somehow so that it stays around as long as we need it to. <br>
To do so, we define this as a hidden variable in an environment that surrounds the function returned when looking up a method, then uses of this in the body would be able to find it later.<br>
Example:
````
class Cake{
    taste() {
        var adjective = "delicious";
        print "The " + this.flavor + " cake is " + adjective + "!";
    }
}
var cake = Cake();
cake.flavor = "Chocolate";
cake.taste(); // The Chocolate cake is delicious!
````
After parsing and resolving, the interpreter phase. <br>
First, a class declaration. Environment global: ``{Cake -> <class cake>}`` In the runtime representation of the class, we have also a map of methods, whose key is name of method, and value is the runtime representation of function. Such LoxFunction has a closure pointing to the global environment``LoxFunction -> Global``<br>
Next, a variable declaration, which lead to the evaluation of the creation of an instance. In VisitCallExpr, LoxClass is a callable, so it gets called, which Invoke the creation of runtime representation of LoxInstance. Therefore, another variable in the global environment `{cake -> <instance cake>}`.<br>
Next, setter. Where we evaluate the `cake.flavor` first, which lead to the evaluation of `cake`, which is a LoxInstance, and we add `flavor -> Chocolate` to its fields.<br>
Lastly, cake.taste(). It is a method call, which is a chain getter and function call. Again go callExpression. Callee is cake.taste, which is the getter. We see that `cake` here is a LoxInstance, we go ahead looking for its property map.<br>
We then find the corresponding method, at this point the function taste() has a closure points to the class. 
````
Global:
Cake -> Cake class                                      <- taste -> Lox function
cake -> Cake instance(with field {flavor -> chocolate} 
````
We have to problem, one at this stage we can not deal with the ``this`` keyword; two, even if we do, we can not reach the flavor, because flavor belongs to instance.<br>
Here if we keep going, we will have somthing like:
````
Global:                                                 
Cake -> Cake class                                     <- Taste() Function Environment : {adjective -> delicious} 
cake -> Cake instance(with field {flavor -> chocolate}   
````
We need somthing like:
````
Global:
Cake -> Cake class                                      
cake -> Cake instance(with field {flavor -> chocolate}  <- [this -> Cake instance] <- [taste -> Lox function]
````
Meaning, when we evaluate teh cake.taste get expression, we do not directly return the LoxFunction, instead, we create a new environment that binds this to the object the method is accessed from, then we make a new LoxFunction with the same code as the original one but using that new environment as its closure.<br>

## Constructor and Initializer
Constructing an object is actually a pair of operations:
1. The runtime allocates the memory required for a fresh instance. (This operation is at a fundamental level beneath what user code is able to access in most language).
2. A user-provided chunk of code initializes the unformed object.<br> 

Think of the first one like creation of LoxInstance. 
In Lox, we use init() as a notation for the chunk of code that sets up a new object for a class.<br>
A couple design idea to mention here.
1. Invoking init() directly<br>
This is essentially, trying to re-initialize an object. However, the author here determines it is best if init() methods always return ``this``.<br>
2. Return from init()
We assume that a user-written initializer doesn't explicitly return a value. Therefore, we need to make it a static error when user does this. 
