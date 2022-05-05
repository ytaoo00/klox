# Expression 

The main Expr class defines the visitor interface used to dispatch against the specific expression types, and contains the other expression subclasses as nested class.

Expr()

## Assign expression

Variable assignment. ``name = expr``

Assign(Token name, Expr expr)

### Binary expression

Binary operators. ``left operator right``

Binary(Expr left, Token operator, Expr right)

### Call expression

Function call expressions. ``callee ( arguments paren `` Notice that paren needed to mark the end of arguments 

call(Expr callee, Token paren, List<Expr> arguments)

### Get expression

Property access. ``object.name``

Get(Expr object, Token name)

### Grouping expression

Using parentheses to group expressions. ``( expression )``

Grouping(Expr expression)

### Literal expression

Literal value expressions. `1` or `"1"` or `null` etc. 

Literal(Object value)

### Logical expression

The logical `and` and `or` operators. ``left operator right``

Logical(Expr left, Token operator, Expr right)

### Set expression

Property assignment, or "set" expressions. ``object.name = value``

Set(Expr object, Token name, Expr value)

### Super expression

The `super` expression. ``keyword.method`` (All the time the keyword is going to be `super`)

Super(Token keyword, Token method)

### This expression

The `this` expression. ``keyword`` (All the time this keyword is going to be `this`)

This(Token keyword)

### Unary expression

Unary operators. ``operator right``

Unary(Token operator, Expr right)

### Variable expression

Variable access expressions. ``name``

Variable(Token name)

Statements
----------

Statements form a second hierarchy of syntax tree nodes independent of expressions.

Stmt()

### Block statement

The curly-braced block statement that defines a local scope. ``{ statement_one statement_two ... }``

Block(List<stmt> statement)

### Class statement

Class declarations. ``CLASS name < superclass { methodOne, methodTwo,...} ``

Class(Token name, Expr.Variable superclass, List<Stmt.Function> methods)

### Expression statement

The expression statement. ``expression ;``

Expression(Expr expression)

### Function statement

Function declarations. ``name ( param_one, param_two, ...) { body }``

Function(Token name, List<Token> params, List<Stmt> body)

### If statement

The `if` statement. ``IF ( condition ) thenBranch ELSE elseBranch``

If(Expr condition, Stmt thenBranch, Stmt elseBranch)

### Print statement

The `print` statement. ``PRINT expression``

print(Expr expression)

### Return statement

You need a function to return from, so `return` statements. ``keyword value ;`` 

Return(Token keyword, Expr value)

### Variable statement

Variable declarations. ``VAR name = initializer``

Var(Token name, Expr initializer)

### While statement 

The `while` statement.``WHILE ( condition ) body``

While(Expr condition, Stmt body)
