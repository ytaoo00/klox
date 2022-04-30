# Resolving and Binding
A variable usage refers to the preceding declaration with the same name in the innermost scope that encloses the expression where the variable is used. <br>
- variable usage => cover both variable expressions and variable assignment. 
- preceding means appearing before in the program text. 
  - In most cases, in straight line code, the declaration preceding in text will also precede the usage in time.
  - However, functions may defer a chunk of code such that its dynamic temporal execution no longer mirrors the static textual ordering.
- Innermost => handle the case of shadowing. For example:
````
var a = "outer"
{
    var a = "inner"
    print a
}
````
Since the rule makes no mention of any runtime behavior, it implies that a variable expression always refer to **the same declaration** through the entire execution of the program.<br>
However, consider the example:
````
var a = "global";
{
    fun showA() {
        print a;
    }
    
    showA();
    var a = "block";
    showA();
}
````
Again, global environment with value ``{a -> "global"}`` no problem here. <br>
Then we have a block, which mean a new environment get created, with parent points to global.<br>
In that, we declare one name, showA, which is bound to the LoxFunction object. <br>
Add to the block environment, we have a binding ``{showA -> <fn showA>}``.<br>
Here, we also define the closure of function showA to be the block environment. <br>
Now we call showA().<br>
At this point, the interpreter dynamically creates an new environment for the function body of showA(). <br>
It is obviously empty, but pointing to the block environment as the closure. <br>
Inside the body of showA(), we print the value of a. <br>
The interpreter looks up this value by walking the chain of environments. It gets all the way to the global environment before finding it there.<br>
We print "global".<br>
Next we have another var declaration statement. This happens in the block statement, which has its own environment, which is the environment showA()'s closure points to. <br>
So, we call the environment's define, which at this point just add a new variable here. <br>
Now, we call showA() again, it again tries to point a, and could not find a in its own environment, so it checks the closure enviornment, which it finds the a.<br>
Unfortunately, we have another a sitting in the global environment as well. <br>

## Scope
Intuitively, we tend to consider all of the code within a block as being within the same scope. So the interpreter uses a single environment to represent that.<br>
Each environment has a mutable hash table. When a new local variable is declared, it gets added to the existing environment for that scope.<br>
This is not entirely correct.<br>
Consider:
````
{
var a;
// 1.
var b;
// 2.
}
````
At the first marked line, only a is in scope. At the second line, both a and b are.<br>
If one defines a "scope" to be a set of declarations, then those are clearly not the same scope---they don't contain the same declarations.<br>
It is almost like each var statement split the block into two separate scopes, the scope before the variable is declared and the one after, which includes the new variable (and the old one).<br>
But in our implementation, environment do act like the entire block is one scope, just a scope that changes over time.<br>
For the most part, this is fine. However, closure does not like that . When a function is declared, it captures a reference to the current environment.<br>
The function should capture a frozen snapshot of the environment as it existed at the moment the function was declared. but instead, in the java code, it has a reference to the actual mutable environment object. <br>
Going back to the example
````
var a = "global";
{
    fun showA() {
        print a;
    }
    
    showA();
    var a = "block";
    showA();
}
````
Here the declaration of a in the block statement happens after the function declaration, however, the second function call can still see it.<br>

## Persistent environments
One way we can prohibit this situation is to use persistent data structure, which can never be directly modified. <br>
Any "modification" to an existing structure produces a brand-new object that contains all the original data and the new modification. THe original is left unchanged. <br>
If we adopt this technique, then every time you declared a variable it would return a new environment that contained all of the previously declared variables along with the new name. <br>
Therefore, declaring a variable would do the implicit "split" where ou have an environment before the variable is declared and one after.
A closure retains a reference to the environment instance in play when the function was declared. Since any later declarations in that block would produce new envrionment objects, the closure would not see the new variables.<br>
It is a legit way to solve the problem, and it is the classic way to implement environments in Scheme interpreters. <br>
However, doing so meaning that we have to change a lot of code.<br>

## Semantic Analysis
Instead of making the data more statically structured, we ll make the static resolution into the access operation itself.<br>
Our interpreter resolves a variable--tracks down which declaration it refers to, each and every time the variable expression is evaluated.<br>
If that variable is swaddled inside a loop that runs a thousand times, that variable get re-resolved a thousand times. <br>
``var a = 0; while(a < 10) { print a; a = a + 1 }`` Notice the variable a get evaluated in the condition once, print statement once, and the assignment statement twice.<br>
However, since we are following static scope, which means a variable usage always resolves to the same declaration. <br>
We can resolve each variable use once, write a chunk of code that inspects the user's program, finds every variable mentioned, and figures out which declaration each refers to. <br>
This process is an example of a semantic analysis. <br>
Where a parser tells only if a program is grammatically correct, which is a syntactic analysis.<br>
Semantic analysis goes farther and starts to figure out what pieces of the program actually mean. <br>
In this case, our analysis will resolve variable bindings, which means we will know not only that an expression is a variable, but also which variable it is.<br>

### Implementation
My first intuition is to use backtracking, going all the way down to the first variable declaration. <br>
The author implemented it in a more "static" way, he says that since each environment corresponds to single lexical scope where variables are declared, if we could ensure a variable lookup always walked the same number of links in the environment chain, that would ensure that it found the same variable in the same scope evey time. <br>
To "resolve" a variable usage, we only need to calculate how many "hops" away the declared variable will be in the environment chain. <br>
The next question is, where in our interpreter's implementation do we stuff the code for it. <br>
Two possible answer, one is parser, which is the "right" answer. However, here we use a separate resolver class as a demonstration of another technique. <br>

### Resolver
This resolver will essentially be an additional pass between the parse phase and execution phase. <br>
This additional phase is common. If we had static types, we could slide a type checker here. Optimizations are often implemented in separate passes like this too. <br>
Basically, any work that doesn't rely on state that's only available at runtime can be done in this way. <br>
The resolver works almost like a mini-interpreter. It walks the tree, visiting each node, but a static analysis is different from a dynamic execution:
- There are no side effects. 
- There is no control flow. -> meaning loops are visited only once, and both branches are visited in if statement, and logic operators are not short-circuited. <br>

Even though the resolver needs to visit every node in the syntax tree, only a few kinds of nodes are interesting when it comes to resolving variables:<br>
- a block statement introduces a new scope for the statement it contains.
- a function declaration introduces a new scope for its body and binds it parameters in that scope
- a variable declaration adds a new variable to the current scope
- variable and assignment expressions need to have their variables resolved.
However, we still have to visit every node, because, say a binary expression like + doesn't itself have any variables to resolve, either of its operands might. <br>

Lexical scopes nest in both the interpreter and the resolver. They behave like a stack.<br>
The interpreter implements that stack using a linked list, which is the chain of environment objects.<br>
For the resolver, the author uses the Java stack directly. Such stack keeps a map representation of a single block scope <br>
Each block scope, in that case a map, use variable names as key, and boolean as value. <br>
THe boolean value represents whether we have finished resolving that variable's initializer.<br>
Notice that the scope stack is for local block scopes only. Variables declared at the top level in the global scope are not tracked by resolver since they are more dynamic in Lox.<br>
When resolving a variable, if we cannot find it in the stack of local scopes, we assume it must be global. <br>
Resolving a variable declaration adds a new entry to the current innermost scope's map, which is at the top of the stack<br>
However, we cannot create the binding just yet. It is two step process, first declare, then define. <br>
Consider:
````
var a = "outer";
{
  var a = a
}
````
We have the situation where the initializer for a local variable refers to a variable with the same name as the variable being declared. <br>
Option 1: Run the initializer, the put the new variable in scope:<br>
Here, the new local a would be initialized with the value of the global one.
````
val temp = a;
var a;
a = temp
````
This makes sense, however, it is probably not a user actually wants. Shadowing is rare and often an error, so initializing a shadowing variable based on the value of the shadowed one seems unlike to be deliberate. <br>
Option 2: Put the new variable in scope, then run the initializer:<br>
This means you could obvserve a variable before it is initialized, meanning the new local a would be re initialized to its own initialized value, nil.
````
var a;
a = a;
````
This is not that useful because the new local variable will always have the value null. <br>
Option 3: Make it an error:<br>
Have the interpreter fail either at compile time or runtime if an initializer mentions the variable being initialized.<br>
We choose to go with this option and make it a compiler error. <br>
To do so, as we visit expressions, we need to know if we are inside the initializer. 
Going back to the two step process, in the declare phase, we basically just put the identifier/variable name into the scope map, and mark the value to be false because it is "not yet ready".<br>
After declaring the variable we resolve its initializer expression in that same scope where the new variable now exists but is unavailable. Once the initializer expression is done, the variable is ready.<br>

## Interpreter
So our resolver generates the binding/environment results but our interpreter needs to use this information. <br>
Therefore, we have to store the resolution information somewhere so we can use it when the variable or assignment expression is later executed.<br>
One place we can store those information is in the syntax tree node itself. However, doing so would require mucking around with our syntax tree generator. <br>
Instead, we will create a global map variable that associates each syntax tree node with its resolved data.<br>
Interactive tools like IDEs often incrementally reparse and re-resolve parts of the user's program. It may be hard to find all of the bits of state that need recalculating when they're hiding in the syntax tree.<br>
One benefit of storing this data outside of the nodes is that it makes it easy to discard it, simply clear the map. <br>

## Resolution error
We do allow declaring multiple variables with the same name in the global scope, for the sake of simple PERL interaction.<br>
But doing so in a local scope is most likely a mistake.<br> 
Other error we can catch during the resolution phase is invalid return statement.<br>
Meaning a return statement that is not inside a function.<br>
The implementation is interesting.<br>
Instead of a boolean, the author have an Enum class, and claim that once we add more cases to it later, it will be useful.<br>
The idea is simple, it is equivalent to a boolean check. <br>
Many other things can be done in this phase, for example, checking if break is used only inside loops, or reporting warnings for code that isn't necessarily wrong but probably isn't useful. <br> 
Notice that while implementing the error, we do not throw the error out, we just report them.<br>
This way we can have the resolver to check out all the code and show all the error message once.<br>
