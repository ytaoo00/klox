# Statement
So far we have defined grammar and expression(AST). Now we move to statement.<br.
- expression statement:<br>
    Essentially is to place an expression where a statement is expected.<br>
    They exist to evaluate expressions that have side effects.<br>
    Side effects: essentially code that does things other than evaluate values.
- print statement:<br>
    Usually things like print will be in a library function. However, for now we left it as an expression.<br>

Recall that we have to define grammar for the expression first, we need to do the same things for statement syntax. <br>
````
program   -> statement* EOF ; 
statement -> exprStmt | printStmt ;
exprStmt  -> expression ";" ;
printStmt -> "print" expression ";" ;
````
The first rule, program, is the entry point for the grammar.<br>
Essentially a program is a list of statements followed by "end of file" token.<br>
Notice that no where in the grammar where am expression and a statement are allowed.<br>
For example, the operands of + are always expressions, and the body of while loop is always a statement.<br>
Therefore, we place expression and statements into separate classes. <br>
Recall that in the earlier notes I mentioned that to replace expression with AST.<br>
Now I found that to be not necessary true, because Statements also need to have their AST.<br>

## State
some sort of combination of variable declaration and lexical scoping. 
### Global Variable
1. variable declaration statement
``var beverage = "Beer"`` which creates a binding that associates a name with a value.<br>
2. variable expression which accesses that binding. 
``print beverage`` when identifier "beverage" is used as an expression, it looks up the value bound to that name and returns it. <br>
#### Grammar
Consider the following 
````
if (monday) print "Ugh" // allowed
if (monday) var beverage = "Beer" // not allowed
````
It is a design reason that caused the second one to be disabled. <br>
Imaging that is allowed, what is the scope of that beverage variable? Does it persist after the if statement? If so, what is its value on days other than Monday? Does the variable exist at all on those days? <br>
To avoid this confusion, this type of grammar is not allowed. <br>
The result is like if there are two levels of "precedence" for statements. <br>
To accommodate this distinction, we modify our grammar.
````
program     -> declaration* EOF ; 
declaration -> varDecl | statement ;
statement   -> exprStmt | printStmt ;
varDecl     -> "var" IDENTIFIER ( "=" expression)? ";" ; 
````
Declaration statements go under the new declaration rule, later we can add functions and classes. <br>
Any place where a declaration is allowed also allows non-declaring statements, hence the additional statement grammar.
IDENTIFIER here is just a symbol, ex. variable name, therefore we add this to our PRIMARY grammar.<br>
Notice the ``( "=" expression)?`` part, this is for declaration without initialization. <br>
````
primary    -> NUMBER | STRING 
            | "true" | "false" 
            | "nil" | "(" expression ") 
            | IDENTIFIER ;
````
As for AST, we design that we add one more AST for statement, called Var. One more AST for expression, called Variable.<br>
### Environment
Now we have the parser works, we have to find a way to have those declarations stored in-memory.<br>
One can think of environment as a map where the keys are variable name and values are variable value. <br>
There are a coule of things we need to decide.<br>
One: Should we check to see if the key(variable name) is already presented in the map?<br>
````
var a = "before"
print a // before
var a = "after"
print a // after
````
We could choose to make this an error. The user may not intend to refine an existing variable in this case. If they did mean to, they probably would have used assignment. <br>
However, doing so interacts poorly with the REPL. In the middle of a REPL session, it is nice to not have to mentally track which variables you have already defined.<br>
Therefore, we allow this type of syntax for now. 

Two: What if the variable is not found?<br>
we have three choices here. 
1. Make it a syntax error
2. Make it a runtime error
3. Allow it and return some default value like nil. <br>
In general, it is a good idea to make it a syntax error. Because using an undefined variable is a bug, and the sooner one can detect the mistake, the better.<br>
The problem is, using a variable isn't the same as referring to it. One can refer to a variable without immediately evaluating it if that chunk of code is wrapped inside a function.<br>
Imaging the following recursion function:
````
fun isOdd(n){
    if (n == 0) return false;
    return isEven(n-1)
}   
fun isEven(n){
    if (n==0) return true;
    return isOdd(n-1)
}
````
The isEven() function is not defined by the time we are looking at the body of isOdd(), causing a syntax error.<br>
Therefore, we will defer the error to runtime. It is OK to refer to variable before it is defined as long as you do not evaluate the reference. <br>

## Error Report
Notice in this chapter, we change the location of our error reporting mechanism.<br>
We use to have the try catch block in parser(). <br>
Now since we have implemented statement, we need to include the synchronize() as well.<br>
The declaration() method is the method we call repeatedly when parsing a series of statements, so this is the right place to synchronize. <br>
If an error has occurred, it will be catched and the error recovery starts. <br>
The real parsing happens inside the try block. First it looks to see if it can start parsing the statement to variable declaration, it not, it goes to parse statement grammar.<br>
statement(), at this stage, will parse expression() if no other statement matches. and expression() reports a syntax error if it cannot parse an expression at the current token.<br>
This chain of calls ensures we report an error if a valid declaration or statement is not parsed.

## Assignment
Notice that in Lox, or other C-derived Languages, assignment is an expression and not a statement.<br>
It is the lowest precedence expression form, slotting between **expression** and **equality**
````
    expression -> assignment;
    assignment -> IDENTIFIER "=" assignment | equality
    equality   -> comparison (("==" | "!=") comparison)*;
````
For example: Consider ``int x = 5;`` the statement defines the variable named x and initializes it with the expression ``5``, which is a literal value.<br>
Now, if ``x`` has been previously declared, ``x = 5;`` is a simple expression statement. Notice here ``x=5`` is an expression.<br>
Perhaps this concept can be better illustrated by ``printf("%d\n", (x = 5));``<br>
It is a little trickier to implement the method as well.<br>
Consider:
````
var a = "before";
a = "value";
````
A single token lookahead recursive descent parser cannot see far enough to tell that it is parsing an assignment unitl after it has gone through the left-hand side and stumbled onto the =.<br>
It matters because the left-hand side of an assignment isn't an expression that evaluate to a value.<br>
On the first line. ``var a = "before";`` will follow the varDecl grammar<br>
On the second line, ``a = "value";`` is an expression statement.<br>
We cannot evaluate ``a`` here. If you do, you get the previously assigned value. <br>
The classic terms for these two constructs are **l-value**(left-hand side value) and **r-value**(right-hand side value).<br>
All the expressions that we have seen so far the produce values are r-values. An l-value "evaluates" to a storage location that you can assign into.<br>
The syntax tree needs to reflect that an l-value isn't evaluated like a normal expression.<br>
To do so, the Expr.Assign node has a Token for the left-hand side, not an Expr. <br>
However, the parser does not know it is parsing an l-value until it hits the ``=``. <br>
For example, ``makeList().head.next = node``. The left-hand side have only a single token(maybe two if we force it), but we are not sure since l-value can be in theory as long as it needs. <br>
The author demonstrates a neat trick. <br>
We parse the left-hand side, which can be any expression of higher precedence, but we do not loop to build up a sequence of the same operator. <br>
Since assignment is right-associative, we recursively call assignment() to parse the right-hand side.<br>
The trick is that, right before we create the assignment expression node, we look at the left0hand side expression and figure out what kind of assignment target it is. <br>
We convert the r-value expression node into an l-value representation. <br>
This conversion works because it turns out that every valid assignment target happens to also be valid syntax as a normal expression.<br>
Notice that you can still use this trick even if there are assignment targets that are not valid expressions.<br>
Define a Cover Grammar, which is a looser grammar that accepts all of the valid expression and assignment target syntaxes. When you hit an =, report an error if the left-hand side isn't within the valid grammar. Conversely, if you don't hit an =, report an error if the left-hand side isn't a valid expression. <br>
We can parse the left-hand side as if it were an expression and then after the fact produce a syntax tree that turns into an assignment target. If the left0hand side expression isn't a valid assignment target, we fail with a syntax error.<br>
Right now, the only valid target is a simple variable expression. <br>
For example, ``a = 3``, we go ahead parse `a` as we normally would. <br>
Then we see `=`, which we then know that is going to be an assignment.<br>
we then parse the r-value.<br>
Then, we check if l-value is the same instance as of r-value. <br>
If so, we convert l-value back to token. 
The end result of this trick is an assignment expression tree node that knows what it is assigning to and has an expression subtree for the value being assigned. <br> 
All with only a single token of lookahead and no backtracking. <br>

## Scope
- Lexical Scope:<br>
Lexical Scope, aka static scope, is a specific style of scoping where the text of the program itself shows where a scope begins and ends.<br>
- Dynamic Scope:<br>
Dynamic scope is where you do not know what a name refers to until you execute the code. For example:
````
class Saxophone{
 play(){print("Whisper")}   
}

class GolfClub{
 play(){print("Fore")}
}

fun playIt(thing){
 thing.play();
}
````
What is the result of the fun playIt depends on what you passed in to the function, we can not know that until runtime.<br>

Scope and environments are closely related. The former is the theoretical concept, and the latter is the machinery that implements it. <br>

### Implementation of Scope
#### First Try
1. As we visit each statement inside the block, keep track of any variables declared. 
2. After the last statement is executed, tell the environment to delete all of those information.
This will not work, For example
````
var volume = 11;

volume = 0;

{
    var volume = 3 * 4 * 5;
    print volume;
}
````
after the execution of the block, in which we create a local variable that shares the same name with global variable, the interpreter will delete the global volume variable. A behavior we do not want. <br>
When a local variable has the same name as a variable in an enclosing scope, it **shadows** the outer one. <br>
Code inside the block cannot see it any more, it is hidden in the "shadow" cast by the inner one, but it is still there.

#### Parent-pointer tree
To solve the shadow problem, we can define a fresh environment for each block containing only the variables defined in that scope. <br>
When we exit the block, we discard its environment and restore the previous one. 
When searching for a specific variable, the interpreter would need to go through all the environment from the innermost to the outermost.<br>
````
        -> block one -> ...
local ->                    -> global
        -> block two -> ...
------------------------------------------------->
````
The implementation in this idea is similar to a linked list.<br>
The author here use the word "enclosing", which to me is not a clear word. <br>
The global environment has a null enclosing.<br>
The inner scope will have an enclosing pointing to the outer scope.<br>
For example
````
-a-
...
-b-
{
 ...
 -c-
 {
 ...
 }
 ...
}
````
a is the global scope, b is the inner scope of a, and c is the inner scope of b.<br>
c has a enclosing points to b, and b has an enclosing scope points to a.<br>
Using the linkedlist analogy, c points to b and b points to a and a points to null.<br>

#### Grammar
````
statement -> exprStmt | printStmt | block;
block     -> "{" declaration* "}" ;
````
Here block itself is a statement, which consists of a series of statements.<br>