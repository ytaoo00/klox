# Function

## Syntax
Consider: ``getCallBack()();``. There are two call expression here. The first pair of parentheses has getCallback as its **callee**. The second call has the entire getCallback() expression as its callee.<br>
Therefore, the callee, can be any expression that evaluates to a function.<br>
Therefore, the grammar here is
````
unary -> ("!" | "-") unary | call ;
call  -> primary ( "(" arguments? ")" )*;
````
Here, the call grammar matches a primary expression followed by zero or more function calls. <br>
If there are no parentheses, this parses a bare primary expression, otherwise each call is recognized by a pair of parentheses with an optional list of arguments. <br>
````
arguments -> expression ( "," expression )* ;
````
The arguments grammar requires at least one argument expression, each preceded by a comma. To handle zero-argument calls, the call rule itself consider the entire arguments production to be optional.<br>
By so, we have achieved this "zero or more comma=separated things" pattern.<br>
Notice here, call grammar and arguments grammar are for expression, not statement.<br>
This makes sense, arguments are essentially a value, and functions returns to a value.<br>

## Number of argument
Having a maximum number of argument has no impact to the current interpreter, yet once we are going to implement the byte machine, it matters.<br>
So, we choose to set the upper bound to 255. <br> 

## Perform the function call
For statement/express, we have come up with ways to execute/evaluate them. What about functions. <br>
We could have defined a new function call to deal with the function call. <br>
However, if you consider carefully, what exactly does the function call() needs to handle.<br>
Or in order word, what will be called as a function. <br>
We have user defined function, we have class objects, and native function.<br>
Instead of defining distinct functions, the author comes up with an interface, and any Lox object that can be called like a function will implement this interface.<br>
The interface looks like
````java
interface LoxCallable {
    Object call(Interpreter interpreter, List<Object> arguments);
}
````
we pass in the interpreter in case the class implementing call() needs it. We also give it the list of evaluated argument values. The implementer's job is then to return the value that the call expression produces.
So, LoxCallable is the skeleton, and with that we can do some error handling.<br>
One, if we call something that is not a function, for exmaple ``"Not a function"()`` we can throw an error.<br>
String aren;t callable in Lox, therefore it will not be an instance of LoxCallable. We can just check if the expression is LoxCallable before we cast the expression and call it.<br>
Next, arity, which is a fancy way to say the number of argument. <br>
For example, if we have ``fun add(a,b,c)`` and call ``add(a,b)``. We are going to define this as an error.<br>
Again, we put arity check in our interface.<br> 

## Native Function
Native functions are functions that the interpreter exposes to user code but that are implemented in the host language(in the author's example, java. In our case, Kotlin), not the language being implemented(Lox)<br>
Since these functions can be called while the user's program is running, they form part of the implementation's runtime.<br>
For example, the functions that allow user to access the file system are usually implemented as native functions. <br>
The author here implement the clock() function to demonstrate the concept of native function. 

## Function Declaration
To allow user to define their own functions, we have to allow, well, function declaration. 
````
declaration -> funDecl | varDecl | statement;
funDecl     -> "fun" function;
function    -> IDENTIFIER "(" parameter? ")" block;
parameters  -> IDENTIFIER ( "," IDENTIFIER)*; 
````
Here the function rule is a helper rule. Think about class, we can reuse this rule for declaring methods, which look similar to function declarations, but aren't preceded by fun. <br>
The function rule, along with the parameters rule, handle the one or more comma seperated parameters just like how we deal with arguments. <br>
Note here, function declaration is a statement. Unlike function call, which will return something, function declaration is just side effects.<br>

## Function Object
The next question is how to represent a Lox function in java. <br>
The goal is to keep track of the parameters so that we can bind them to argument values when the function is called, and keep the code for the body of the function so that we can execute it. <br>
Going back to the function statement node. Here the function statement node has a list of parameters and all the statement in the function body, we can implement method directly here.<br>
However, if we do so, we need to have the interpreter, which is an object of the runtime phase, showing in the front end's syntax class, making the code difficult to maintain.<br>
Therefore, we implement another class, and wrap the function statement node in.<br>
The LoxCallable interface is a spoiler.<br>
### Name Environment Management
Parameters are core to functions since a function encapsulates its parameters, which means no other code outside of the function can see them.<br>
This means each function gets its own environment where it stores those variables. <br>
Further, this environment must be created dynamically. Each function call gets its own environment.<br>
If not recursion will break. <br>
````
fun count(n){
    if (n > 1) count(n-1)
    print n;
}

count(3);
````
The anticipated output is, 1, 2, 3.<br>
Imaging the stage of the interpreter right at the point where it is about to print 1 in the innermost nested call. The outer calls to print 2 and 3 haven't printed their values yet, so there must be environments somewhere in the memory that still store the fact that n is bound to 3 in one context, 2 in other, and 1 in the innermost.<br>
So, the key is, **each function call has it own environment, not each function**.
````
|count(1)|  -> has an environment
|count(2)|  -> has an environment 
|count(3)|  -> has an environment 
---------
````
Therefore, we have to create a new environment at each call, not at the function declaration.<br>
Take a closer look at the author's implementation.
````java
public Object call(Interpreter interpreter, List<Object> arguments){
    Environment environment = new Environment(interpreter.globals);
    for (int i = 0; i < declaration.params.size(); i ++){
        environment.define(declaration.params.get(i).lexeme,
            arguments.get(i)));
        }
    interpreter.executeBlock(declaration.body, environment);
    return null
}
````
Here we create a new enviroment at each call. For now we just set the environment to globals, which I believe it is subject to change. <br>
Then the call method walks the parameter and argument lists in lockstep. For each pair, it creates a new variable with the parameter's name and binds it to the argument's value.
At this point, there is another interesting implementation detail that is worth to consider.
Recall that our function grammar is ``function    -> IDENTIFIER "(" parameter? ")" block;``<br>
In our implementation of FunctionStmt Node, we have ``class FunctionStmt(val name: Token, val params: List<Token>, val body: List<Stmt>)``<br>
Notice here the last parameter is a list of statement, not a block statement. <br>
Why?
Notice this line, ``interpreter.executeBlock(declaration.body, environment);``.<br>
We see that every executeBlock is taking in the environment, which at this point contains the parameter value already.<br>
Therefore, the local variable scope is the same as the parameter scope<br>
For example:
````
fun print(a)
    var a = 1
    print(a)
````
Enter the block statement, we assigned to the block an environment which already contains the parameter. In this particular cases, a ready has a value.<br>
The author says we will eventually prohibit shadowing, therefore this is not going to be valid. However, at this point, once we interpret the assignment, which will invoke the environment's define function, we will overwrite the a value from the parameter.<br>
For example, `print(100)` will return `1`
## Return Statement
Parsing the Return Statement is quite straight forward, the only note is that, since Lox is dynamically type, the compiler has no way of preventing you from taking the result value of a call to a function that doesn't contain a return statement.<br>
To prevent:
````
fun demo(){
    print "no return"
}
var result = demo()
print result //????
````
We design that every function in lox must return something, if no return statement in the body, recall in the call function, we implicitly return null. <br>
For exmaple, ``return`` will be treated as ``return nil``<br>
The grammar is ``return -> "return" expression? ";"``

However, implementing the Return Statement is tricky, because return statement can be anywhere in the body of a function, and when the return is executed, the interpreter needs to jump all the way out of whatever context it is currently in and cause the function call to complete. <br>
The author here, very very very smartly, use the java exception to achieve this goal. <br>
The idea is, whenever we execute a return statement, we will use an exception to unwind the interpreter past the visit methods of all of the containing statements back to the code that began executing the body. <br>

## Local function and closures
We will go in more detailed explanation in next chapter. <br>
For now we focus on the parent of the function environment.<br>
In Lox, function declarations are allowed anywhere a name can be bound, in other words, Lox supports local functions that are defined inside another function, or nested inside a block. <br>
````
fun makeCounter(){
    var i = 0
    fun count() {
        i = i + 1
        print i
    }   
    return count
}
````
Here, count() uses i, which is declared inside of the outer function makeCounter(). The idea is, count will be able to access makeCounter()'s environment, when accessing i = i + 1, it updates the i value in makeCounter()'s environment.<br>
Later if we have
````
var counter = makeCounter()
counter();//1
counter();//2
````
Using this example, based on our current implementation, our global environment has a variable map ``{makeCounter -> <fn makeCounter>; counter -> <fn count>}``.<br>
The first one, ``{makeCounter -> <fn makeCounter>`` is defined during vistFunctionStmt() which is the phase of interpret the function. Notice that the environment now just store the binding between the function name and the function, nothing else happens. We have not see the function count() yet.<br<
Next, it sees a variable declaration statement.
Here, the right side of the expression is a function call, this is interesting. <br>
VisitCallExpr now see a callee, namely makeCounter(), it tries to return callee.call(). The call function, first created a environment for makeCounter whose parent is global, currently has no value in.<br>
Next, execute the block statement with the new empty environment. <br> 
Since it meet a variable declaration statement, var i get stored in the makeCounter's environment. <br>
Next, it sees a function count() again. Now the environment for makeCounter is ``{i -> 0, count -> <fn count>}``<br>
Next return count, here, makeCounter() will be able to return ``<fn count>``, however, it's environment get discarded.<br> 
so at this point, counter's value is the count(), hence the second ``{counter -> <fn count>}``<br>
However, when counter is called, just like how makeCounter get handled, count() creates an environment, whose parent is globals as well.<br>
Therefore, two problem.<br>
One. We have to somehow point the parent of count() to makeCounter().<br>
Two. makeCounter's environment get discarded once executing final. We do not want count's parent environment to be null, which is useless.<br>
Solution:
In the inner class, we have an additional field to store the parent class.<br>
More formally, this is called closure. <br>

