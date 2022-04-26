# Parsing Expression

## Ambiguity
Recall that the grammar we defined in the previous chapter, is ambiguous. <br>
````
expression  -> literal | unary | binary | grouping ;
literal     -> NUMBER | STRING | "true" | "false" | "nul" ;
grouping    -> "(" expression ")" ;
unary       -> ( "-" | "!" ) expression ;
binary      -> expression operator expression ;
operator    -> "==" | "!=" | "<" | "<=" | ">" | ">=" 
                 | "+"  | "-"  | "*" | "/" ; 
````
For example, 6 / 3 - 1, can be parsed to (6 / 3) - 1 or 6 / (3 - 1).<br>
It is because the binary rule allows operands to nest any which way you want. <br>
To solve the problem, we define **Precedence** and **Associativity**.<br>
### Precedence
Precedence determines which operator is evaluated first in an expression containing a mixture of different operators. <br>
For example, division("/") is evaluated fore the minus("-").<br>
Higher precedence operators get evaluated before lower precedence operators.<br>
### Associativity
Associativity determines which operator is evaluated first in a series of the same operator. <br>
Ex. 5 - 3 - 1. The operator here is minus("-"). And the - operator is left-associative(left to right).<br>
Therefore, 5 - 3 - 1 gets evaluated to (5 - 3) - 1. <br>
Assignment is right-associated, meaning a = b = c is equivalent to a = (b = c). <br>

To solve the ambiguity issue, we need a well-defined precedence and associativity.<br>
The following table arranges the operators from the lowest precedence to the highest precedence. 

| Name       | Operators | Associates |
|:-----------|:---------:|:----------:|
| Equality   |   == !=   |    Left    |
| Comparison | > >= < <= |    Left    |
| Term       |    - +    |    Left    |
| Factor     |    / *    |    Left    |
| Unary      |    ! -    |   Right    |

The idea is to stratify the grammars so that there is a rule for each precedence level, such that each rule only matches expressions at its precedence level or higher. <br>
````
expression -> 
equality   -> 
comparison ->
term       ->
factor     ->
unary      ->
primary    ->
````
Expression will match all the rules belows, and equality is the rule with the lowest precedence.<br>
Therefore, we just need ``expression -> equality`` and equality will contain all the precedence level. <br>
Notice that we do not really need this rule, but for readility issue, we keep it as a placeholder. <br>
Now, equality has a nature form of ``... == ...``.  
````
equality -> Non-Terminal ("==" | "!=") Non-Terminal
````
Since we are working our way down, it seems nature to put the rules next precedence level down. <br>
````
equality -> comparison ("==" | "!=") comparison;
````
Notice how the comparison here make sure that higher level rules get evaluated first.<br>
equality here is a confusing name, we do not really need the whole expression. On the other hand, it is used to be passed to expression.<br>
In other word, equality here need also be a valid expression of any forms. So
````
equality -> comparison (("==" | "!=") comparison)*;
````
meaning that the "equals to" or "not equals to" operators are performed as needed. <br> 
Using the similar idea, we have
````
expression -> equality;
equality   -> comparison (("==" | "!=") comparison)*;
comparison -> term ((">" | ">=" | "<" | "<=" ) term)*;
term       -> factor (("+" | "-") factor)*;
factor     -> unary (("*" | "/") unary)* ;
unary      -> ...
primary    -> ...
````
For unary, following our design pattern, it will be ``unary -> primary (("!" | "-") primary)*``<br>
In this nature form, we have unary expression like "!true". <br>
the first thing we notice here is we do not need a primary value in the front.<br>
``unary -> (("!" | "-") primary)*``<br>
Again this is not true because this is forcing us to add a unary operation, making it impossible to parse, for example, a = 3. <br>
``unary -> (("!" | "-") primary)* | primary``<br>
We almost here, the last edge case here is "!!ture" which if a valid expression. The operand can itself be a unary operator. <br>
``unary -> ("!" | "-")* primary ``<br>
And of course the base case.<br>
``primary -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ");"``
````
expression -> equality;
equality   -> comparison (("==" | "!=") comparison)*;
comparison -> term ((">" | ">=" | "<" | "<=" ) term)*;
term       -> factor (("+" | "-") factor)*;
factor     -> unary (("*" | "/") unary)* ;
unary      -> ("!" | "-")* primary;
primary    -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ");"
````

Just to play around with the idea of design and recursion a little more...<br>
First we design the base case.<br>
``primary    -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")";``
the next level up is unary<br>
``unary -> ("!" | "-") unary | primary``
This design is "more intuitive" according to the author.<br> 
The idea here is recursion.<br>
Recall that unary array is right associates, hence the right recursion, which represents the same level of expression.<br>
````
factor -> factor ("/" | "*") unary 
          | unary;
````
Again putting the recursive production on the left side and unary on the right makes the rule left-associative and unambiguous. <br>
However, due to the implementation, we want to eliminate the left-recursion. 
Hence,``factor -> unary (("/" | "*") unary)*``. <br>
````
expression -> equality;
equality   -> comparison (("==" | "!=") comparison)*;
comparison -> term ((">" | ">=" | "<" | "<=" ) term)*;
term       -> factor (("+" | "-") factor)*;
factor     -> unary (("*" | "/") unary)* ;
unary      -> ("!" | "-") unary | primary;
primary    -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ");"
````
## Parsing Strategy
We will use recursive descent as the parsing strategy.<br>
Recursive descent is considered a top-down parser because it starts from the top or outermost grammar rule (here expression) and works its way down into the nested subexpressions before finally reaching the leaves of the syntax tree.<br>
A recursive descent parser is a literal translation of the grammar's rule into imperative code. <br>
Each rule becomes a functions.

| Grammar Notation |        Code Representation        |
|:-----------------|:---------------------------------:|
| Terminal         | Code to match and consume a token |
| Non Terminal     |   Call to that rule's function    |
| pipeline         |      if or switch statement       |
| * or +           |         while or for loop         |
| ?                |           if statement            |

The descent is described as "recursive" because when a grammar rule refers to itself, directly or indirectly, that translates to a recursive function call. 

## Chapter Two Expression vs Chapter Three Grammar
In chapter two, we defined the following Grammar
````
expression  -> literal | unary | binary | grouping ;
literal     -> NUMBER | STRING | "true" | "false" | "nul" ;
grouping    -> "(" expression ")" ;
unary       -> ( "-" | "!" ) expression ;
binary      -> expression operator expression ;
operator    -> "==" | "!=" | "<" | "<=" | ">" | ">="
                | "+"  | "-"  | "*" | "/" ;
````
In chapter three, our grammar becomes:
````
expression -> equality;
equality   -> comparison (("==" | "!=") comparison)*;
comparison -> term ((">" | ">=" | "<" | "<=" ) term)*;
term       -> factor (("+" | "-") factor)*;
factor     -> unary (("*" | "/") unary)* ;
unary      -> ("!" | "-") unary | primary;
primary    -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ");"
````
Notice that those are grammars, and the second one is unambiguous. <br>
However, we also come up with a class called Expr in chapter two, while changing the grammar, the Expr class remains untouched.<br>
If I am right, the Expr class represents the abstract syntax tree, while the grammar will produce a parse tree. <br>
At a high level, we will want to use the unambiguous grammars to construct parsers, but we insert code into parser implementation to explicitly generate abstract syntax tree.<br>
This is why we see that function call to map, say for example, equality to binary expression.<br>
For example, we see the expression 6 / 3 == ( 3 + 1) /2
Grammar-wise: we have
````
expression
    |_ equality
       |_ comparision
       |  |_ term
       |     |_factor
       |       |_ unary
       |       | |_ primary
       |       |   |_ Number
       |       |_ "/"
       |       |_ unary
       |         |_ priimary
       |           |_ Number
       |_ "=="
       |_ comparision
       |  |_ term
       |     |_factor
       |     | |_unary
       |     |   |_ primary
       |     |     |_ "("
       |     |     |_ expression
       |     |     | |_ equality
       |     |     |    |_ comparison
       |     |     |      |_ term
       |     |     |        |_ factor
       |     |     |        | |_ unary
       |     |     |        |    |_ primary
       |     |     |        |      |_ Number 
       |     |     |        |_ "+"
       |     |     |        |_ factor
       |     |     |          |_ unary
       |     |     |             |_ primary
       |     |     |               |_ Number
       |     |     |_ ")"
       |     |_ "/"
       |     | |_unary
       |     |   |_ primary
       |     |     |_ Number
````
This, if I am correct, is called a parse tree.
However, the abstract syntax tree is different. <br>
````
Binary
|_ Binary
| |_ Literal
| |_ Operator (token) /
| |_ Lieral
|_ Operator (token) ==
|_ Binary
| |_ Grouping
| | |_ Binary 
| |   |_ Literal
| |   |_ Operatoer (token) +
| |   |_ Lieral
| |_ Operator (token) /
| |_ Lieral
````

As the author points out, In a parse tree, every single grammar production becomes a node in the tree. An AST elides productions that aren't needed by later phases.<br>
Perhaps it is possible to rename the class AST to avoid confusion???

## Syntax Errors
Parser has two jobs:
1. Given a valid sequence of tokens, produce a corresponding syntax tree. 
2. Given an invalid sequence of tokens, detect any errors and tell the user about their mistakes.

In modern IDE, syntax highlight and auto-complete and many more are achieved by having the parser to constantly reparse the code. <br>

Requirement for Error reporting:
- Detect and report the error
- Avoid crashing or hanging. 
- Be fast
- Report as many distinct errors as there are: do not abort after the first error; Show the user all the error at once. 
- Minimize cascaded errors: Once the parser sees an error, it gets confused that it may report "errors" that goes away when the true error get fixed. 

### Panic Mode
A techniques for error recovery (how a parser respond to an error and keep going for later errors)<br>
Parser detects an error -> at least one token doesn't make sense given its current state.<br>
Before it can get back to parsing, it needs to get its state and the sequence of forthcoming tokens aligned such that the next token does match the rule being parsed.<br>
This process is called **synchronization**.<br>
Per my understanding, somehow the parser figures out the current product, jumps out of any nested production, and continues to parse.<br>
This way no cascaded errors will be reported, but also no real syntax errors in those discarded tokens will be reported. A trade-off.<br>
We will not do any synchronize here since it happens mostly between statements, but will do so and cover the topic in more detail in the coming chapters. <br>


### Exception
First recall Java stack
Say you have a structure such that
main()->a() -> b() -> c() -> d()<br>
The call stack will look like

| call stack |
|:----------:|
|    d()     |
|    c()     |
|    b()     |
|    a()     | 
|   main()   |
Say, for example, d() is a function that takes user input.<br>
we know, by design, it is likely to have a user input that is invalid. <br>
we can explicitly throw an exception at d().<br>
Say if our try catch statement is in main(), then if d() throws an exception, the compiler will stop executing d() and all the nested call. In this case a(); b(); and c().<br>

Let's take the Lox Parser as another more concrete example:
Here we try to parse an expression with recursive descent strategy.<br>

|  call stack  |
|:------------:|
|  primary()   |
|   unary()    |
|   factor()   |
|    term()    |
| comparison() |
|  equality()  |
| expression() |
|   parser()   |

Say there is an error happens in primary(), for example we see an unexpected symbol @.<br>

| call stack  |
|:-----------:|
| Lox.error() |
|   error()   | 
|  primary()  |
|     ...     |
|  parser()   |

In the error() function call, we return an exception, and in the primary(), we throw this exception.<br>
Why not throw the exception in error()?<br>
Again looking ahead with some more complex statement, we may need to use the synchronize method to abandon only nested function calls to a certain depth.<br>
For this simple expression parsing, we catch the exception in parser(). <br>
But for more complex design, we might need to catch at where we are synchronizing to. <br>

