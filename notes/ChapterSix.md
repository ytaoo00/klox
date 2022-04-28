# Control Flow
## Conditional Execution(if statement)
As usually, we first define the new grammar.<br>
````
statement -> exprStmt | ifStmt | printStmt | block ;

ifStmt    -> "if" "(" expression ")" statement ("else" statement)? ;
````
However, there is an ambiguity caused by the else part. <br>
Consider, ``if (first) if (second) whenTrue(); else whenFalse();``
This can have two meanings.<br>
One:<br>
````
if (first)
    if (second)
        whenTrue();
else
    whenFalse();
````
Two:<br>
````
if (first)
    if (second)
        whenTrue();
    else
        whenFalse();
````
Since else clauses are optional, and there is no explicit delimiter marking the end of the if statement, the grammar is ambiguous when you nest ifs in this way. <br>
This is called the dangling else problem.<br>

Solution:
1. redefine the Context-Free grammar.<br>
To do so, we need to splitting most of the statement rules into pairs, one t hat allows an if with an else and one that doesn't.
2. Define else that is bound to the nearest if that precedes it. <br>
Our existing grammar is able to handle this. <br>

Using the same example: ``if (first) if (second) whenTrue(); else whenFalse();`` <br>
````
if
  |_ expression (first)
  |_ statement
     |_ if 
       |_ expression(second)
       |_ statement (whenTrue())
       |_ else 
         |_ statement ( whenFalse())
````
Another observation is that, consider that classic ``if-else if-else`` control. Our grammar is able to parse that as well.<br>
For example: 
````
if(a)
    doA()
else if(b)
    doB()
else
    doC()
````
to parse that, we have:
````
if 
  |_ expression(a)
  |_ statement(doA())
  |_ else
    |_ if
      |_ expression(b)
      |_ statement(doB())
      |_ else
        |_ statement(doC())
````
So very interestingly, else if is not a keyword by itself. <br>
### Logical Operator
The logical operator ``and`` and ``or`` are technically control flow construct. The author did an interesting implementation.<br>
Again, grammar first.<br>
````
expression  -> assignment;
assignment  -> IDENTIFIER "=" assignment | logic_or
logic_or    -> logic_and ( "or" logic_and )* ;
logic_and   -> equality ( "and" equality )* ;
````
Here, the logic_or and logic_and each have their own precedence. The precedence of ``or`` is lower than that of ``and``.<br>
Both of which has a lower precedence than that of ``equality``.<br>
Notice that the implementation of the two logic operator is essentially the same as binary operator. <br>
However, if we reuse the existing Binary class, the binary class would then have to check to see if the operator is one of the logical operators and use a different code path to handle the **short-circuiting**.
Short-circuit is an interesting property. In some cases, if, after evaluating the left operand, we know what the result of the logical expression must be, we do not have to evaluate the right operand.<br>
For example, ``false and sideEffect()`` is also going to be evaluated as false. <br>

## Loop
### While Loop
While loop is pretty straightforward. <br>
````
statement -> exprStmt | ifStmt | printStmt | whileStmt | block;

whileStmt -> "while" "(" expression ")" statement;
````
### For Loop
For lox, the for loop template looks like ``for(var i = 0; i < 10; i = i +1) print i``<br>
Grammar:
````
statement   -> exprStmt | forStmt | ifStmt | printStmt | whileStmt | block ; 

forStmt     -> "for" "(" ( varDecl | exprStmt | ";") 
                expression? ";"
                expression? ")" statement; 
````
In side the parentheses, three clauses separated by semicolons:
1. Initializer: it is executed exactly once, before anything else.
   1. It can be a variable declaration of form ``(val a = 1)``, in that case the variabel is scoped to the rest of the for loop.
   2. It can be an expression statement, ``val a; for(a = 1,...)``.
   3. It can be just a semicolon, which simply means no variable will be initialized.
2. Condition, which controls when to exit the loop. It is evaluated once at the **beginning** of each iteration, including the first.
   1. It can be an expression followed by a semicolon, ``a < 10``
   2. It can be just a semicolon, which means no condition. 
   3. Notice here we set this to expression instead of expression statement, because we at least need one semicolon. 
3. Increment, which is an arbitrary expression that does some work at the **end** of each loop iteration. 

Notice that, everything we can achieve in the for loop, we can achieve the same result using a while loop.<br>
````
for(var i = 0; i < 10; i = i + 1){
    print i;
}

{
    var i = 0
    while(i<10){
        print i;
        i = i + 1;
    }
}
````
The two loops does exactly the same thing. Notice the outer block in the second part ensure the same variable scope.<br>
Therefore, we can set our strategy as **desugaring** the for loop to while loop.<br>
That essentially means, for loop will not produce its own AST, but get transferred to other existing ASTs.<br>
