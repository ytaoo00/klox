# Inheritance
## Superclasses and subclasses
In Lox, the syntax for superclass is: `<`
````
class Doughnut{
    // General Doughnut Stuff
}
class BostonCream < Doughnut{
    // Boston Cream Specific Stuff
}
````
Grammar:
````
classDecl -> "class" IDENTIFIER ( "<" INDENTIFER) ? "{" function* "}" ;
````
After the class name, you can have a `<` followed by the superclass's name. <br>
Unlike java, Lox has no root "Object" class that everything inherits from.<br>
In the implementation, the author store the superclass name as an Expr.Variable, not a token. The grammar restricts teh superclass clause to a single identifier, but at runtime, that identifer is evaluated as a variable access.<br>
Wrapping the name in an Expr.Variable early on in the parser gives us an object that resolver can hang the resolution information. 

## Inheriting Methods
Inheriting from another class means that everything that is true of the superclass should be true of the subclass.<br>
Since Lox is a dynamically typed language, if you can call some method on an instance of the superclass, you should be able to call that method when given an instance of the subclass.<br>
In other words, methods are inherited from the superclass. <br>

## Super
If a method with the same name exists in both the subclass and superclass, the subclass one takes precedence or overrides the superclass method.<br>
However, now we have no way to refer to the original one, this is bad if we would only want to refine or extends the superclass's behavior.<br>
If the subclass method tries to call it by name, it will just recursively hit its own override. <br>
With the super keyword, we essentially start our search on the superclass.<br>
Usually the syntax for super is ``super.methodName()``. However, as with regular methods, the argument list is not part of the expression. Instead, a super call is a super access followed by a function call. Like other method calls, you can get a handle to superclass method and invoke it separately.<br>
Therefore we define the super grammar as ``primary -> ...... | "super" "." IDENTIFIER``.<br>
Notice how the super expression itself contains only the token for the super keyword and the name of the method being lookup.<br>

## Semantics
One interesting question, a super expression starts the method lookup from the superclass, but which superclass.<br>
First intuition, the superclass of ``this``. However, this is not always right.<br>
Consider:
````
class A{
    method() {
        print "A method";
    }
}

class B < A {
    method(){
        print "B method";
    }
    test(){
        super.method()
    }
}

class C < B {}
C().test();// "A method"
````
With what we have now, C().test() looks for method in C() and non could be found, therefore, it goes to b's body, which then a test() method is found.<br>
However, we need to resolve super, since we are calling from C, if we define super as the superclass of the current instance, super will become B, which lead to an output of "B method".<br>
The correct execution flow, on the other hand, is:
1. we call test() on an instance of C.
2. that enters the test() method inherited from B. that calls super.method()
3. The superclass of B is A, so that chains to method on A, and the program prints "A method".<br>

Therefore, we sort of have to let each instance remember its onw super.<br>
It is kind of like how we deal with `this`, in which we used our existing environment and closure mechanism to store a reference to the current object.<br>
We will use this approach once again.<br>
Notice that there is another approach, we could add a field to Lox to store a reference to the LoxClass that owns that method. The interpreter would keep a reference to the currently executing LoxFunction so that we could look it up later when we hit a super expression. From there, we'd get teh LoxClass of the method, then its superclass.<br>
One important difference between ``super`` and ``this`` is that we bound this when the method was accessed. The same method can be called on different instances and each needs its own this.<br>
With ``super`` expressions, the superclass is a fixed property of the class declaration itself. Every time you evaluate some super expression, the superclass is alwasy the same. <br>
That means we can create the environment for the superclass once, when the class definition is executed. Immediately before we define the methods, we make a new environment to bind the class's superclass to the name super. <br>
When we create the LoxFunction runtime representation for each method, that is the environment they will capture in their closure. Later, when a method is invoked and this is bound, the superclass environment becomes the parent for the method's environment.<br>
````
Global:
A -> A class
B -> B class
C -> C class  <- super -> b class <- this -> C instnace 
````
