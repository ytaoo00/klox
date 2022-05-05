/**
 * Parser Class
 * @property tokens list of token generated from [Scanner]
 * @constructor Creates a parser instance
 */
class Parser(private val tokens : List<Token>) {
    /**
     * Customer Exception class, extends from Kotlin's Exception.
     */
    class ParserError : Exception()

    /**
     * Pointer to the token to be parsed.
     */
    private var current = 0

    /**
     * Initializes the parsing, implicitly represents the grammar rule program.
     *
     * program -> declaration* EOF ;
     * @return List of [Stmt] ASTs which represent the entire program constructed from the input Token.
     */
    fun parse(): List<Stmt> {
        // prepare a empty list of statements
        val statements : MutableList<Stmt> = mutableListOf()
        // until reaching the end of the file, parse one statement at a time
        while (!isAtEnd()){
            // if declaration returns null, then some error happened, we relay on synchronize() to recover
            // and we can safely skip
            val statement = declaration()
            if (statement != null) {
                statements.add(statement)
            }
        }
        // convert the statement to Immutable before return
        return statements.toList()
    }

    /**
     * Represents the grammar rule declaration, catch error to resume the parser to normal stage if error.
     *
     * declaration -> classDecl | funDecl | varDecl | statement ;
     * @return an statement AST.
     */
    private fun declaration() : Stmt?{
        try {
            // goto class declaration
            if (match(TokenType.CLASS)) return classDeclaration()
            // go to function declaration, type of "function" is function
            if (match(TokenType.FUN)) return function("function")
            // go to variable declaration
            if(match(TokenType.VAR)) return varDeclaration()
            // otherwise, statement
            return statement()
        } catch (error : ParserError){
            // synchronize to get out of pain mode and jumps to the next possible line where parser can work normally
            synchronize()
            // do not return AST at this point
            return null
        }
    }

    /**
     * Represents the grammar rule classDecl.
     *
     * classDecl -> "class" IDENTIFIER ( "<" IDENTIFIER) ? "{" function* "}" ;
     * @return an AST which represents classDecl
     */
    private fun classDeclaration() : Stmt.ClassStmt{
        // consume the keyword name
        val name = consume(TokenType.IDENTIFIER, "Expect class name.")
        // check if there is this class is inherited from other class
        var superclass : Expr.Variable? = null
        // if so, consume the tokens necessary
        if (match(TokenType.LESS)){
            consume(TokenType.IDENTIFIER, "Expect superclass name.")
            // the grammar restricts the superclass clause to a single identifier, but at runtime, that identifier is evaluated as a variable access
            // wrapping the name in an Expr.Variable early in the parser gives us an object that resolver can hang the resolution information off of.
            superclass = Expr.Variable(previous())
        }
        // consume "{"
        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.")
        // the rest if the class method if any
        val methods = mutableListOf<Stmt.FunctionStmt>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()){
            methods.add(function("method"))
        }
        // consume is "}"
        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.")
        // return the subtree
        return Stmt.ClassStmt(name,superclass, methods)
    }

    /**
     * Represents the grammar rule funDecl.
     *
     * funDecl -> "fun" function ;
     * @return an AST which represents funDecl
     */
    private fun function(kind : String) : Stmt.FunctionStmt{
        // consume the function name
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        // consume (
        consume(TokenType.LEFT_PAREN, "Expect '(' after + $kind name.")
        // consume the parameter
        val parameters = mutableListOf<Token>()
        // until reach the ")"
        if (!check(TokenType.RIGHT_PAREN)){
            // add the parameter which in this context is an identifier, one at a time
            do {
                // more than 255 parameter, records an error
                if (parameters.size >= 255){
                    error(peek(), "Can't have more than 255 parameters.")
                }
                parameters.add(
                    consume(TokenType.IDENTIFIER, "Expect parameter name.")
                )
            }
            // parameters are seperated by ","
            while (match(TokenType.COMMA))
        }
        //consume ")"
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")
        //consume "{"
        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.")
        // the rest is function body, parse those
        val body = block()
        // return FunctionStmt node
        return Stmt.FunctionStmt(name,parameters,body)
    }

    /**
     * Represents the grammar rule varDecl, which returns a subtree for the [declaration](Final) AST
     *
     * varDecl     -> "var" IDENTIFIER ( "=" expression)? ";" ;
     * @return an AST which represents varDecl
     */
    private fun varDeclaration() : Stmt {
        // consume the variable name
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
        // assume the initializer is null
        var initializer : Expr? = null;
        // if see "=", parse the initializer
        if (match(TokenType.EQUAL)){
            initializer = expression()
        }
        // consume the semicolon
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration")
        // return varStmt node
        return Stmt.VarStmt(name, initializer)
    }

    /**
     * Represents the grammar rule statement.
     *
     * statement      → exprStmt | forStmt | ifStmt | printStmt | returnStmt | whileStmt | block ;
     * @return an AST which represents statement
     */
    private fun statement() : Stmt{
        // check if for statement
        if (match(TokenType.FOR)) return forStatement()
        // check if ifStmt
        if (match(TokenType.IF)) return ifStatement()
        // check if printStmt
        if (match(TokenType.PRINT)) return printStatement()
        // check if return Statement
        if (match(TokenType.RETURN)) return returnStatement()
        // check if while statement
        if (match(TokenType.WHILE)) return whileStatement()
        // check if block
        if (match(TokenType.LEFT_BRACE)) return Stmt.BlockStmt(block())
        // otherwise, expression statement
        return expressionStatement()
    }

    /**
     * Represents the grammar rule expressionStatement.
     *
     * exprStmt  -> expression ";" ;
     * @return An AST for expression statement
     */
    private fun expressionStatement() : Stmt{
        // parse the expression
        val expr = expression()

        // consume the ";"
        consume(TokenType.SEMICOLON, "Except ';' after value")

        // return the AST node
        return Stmt.ExpressionStmt(expr)
    }

    /**
     * Represents the grammar rule statement.
     *
     * forStmt -> "for" "(" ( varDecl | exprStmt | ";") expression? ";" expression? ")" statement;
     *
     * @return an AST for while
     */
    private fun forStatement() : Stmt{
        // already consume the keyword for in statement(), consume "("
        consume(TokenType.LEFT_PAREN, "Expect '(' after For.")

        // for ( var i = 0; i < 10; i = i + 1)
        // initializer
        // if no initializer, that is, ";".
        // match the semicolon and return null
        val initializer : Stmt? = if (match(TokenType.SEMICOLON)){
            null
        }
        // if variable declaration, that is "var a = 0"
        else if (match(TokenType.VAR)){
            varDeclaration()
        }
        // if expressionStatement, that is "a"
        else{
            expressionStatement()
        }

        // condition
        // if the next token is not a semicolon, then it must be an expression. Ex. "a < 10"
        // notice that it is technically an expression statement because of the semicolon
        // here we match the semicolon manually and parse the expression
        var condition : Expr? = if(!check(TokenType.SEMICOLON)){
            expression()
        }
        // if it is semicolon, then return null ex. ";" notice that is equivalent to while(true)
        else{
            null
        }
        // consume the semicolon
        consume(TokenType.SEMICOLON, "Expect ';' after For Loop Condition.")

        // increment
        // if the next token is not a semicolon, then it must be an expression, ex. a = a + 1
        val increment : Expr? = if (!check(TokenType.RIGHT_PAREN)){
            expression()
        }else {
            null
        }
        // consume the right expression
        consume(TokenType.RIGHT_PAREN, "Expect ')' after For Loop Clauses.")

        // body
        var body = statement()

        // transform the for loop to while loop
        // if increment is not null, we add after the body, and enclose them into a block {statement(), increment}
        if (increment != null){
            body = Stmt.BlockStmt(listOf(body,Stmt.ExpressionStmt(increment)))
        }

        // change condition to while loop condition, if no condition, it is equivalent to while(true)
        if (condition == null){
            condition = Expr.Literal(true)
        }

        // change body from a block to a while loop
        body = Stmt.WhileStmt(condition,body)

        // finally, we add in the var declaration/ expression statement if needed, and enclosed the entire thing into a block statement
        // {initializer, while(condition) {statement(), increment} }
        if (initializer != null){
            body = Stmt.BlockStmt(listOf(initializer,body))
        }

        return body
    }

    /**
     * Represents the grammar rule if.
     *
     * ifStmt    -> "if" "(" expression ")" statement ("else" statement)? ;
     *@return an AST for if statement.
     */
    private fun ifStatement() : Stmt{
        // keyword if consumed in statement(), consume "("
        consume(TokenType.LEFT_PAREN, "Expect '(' after if.")
        // parse condition
        val condition = expression()
        // consume ")"
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.")
        // parse thenBranch
        val thenBranch = statement()
        // if there is else, parse elseBranch, else return null
        var elseBranch : Stmt? = null
        if (match(TokenType.ELSE)){
            elseBranch = statement()
        }
        // return if statement AST
        return Stmt.IfStmt(condition,thenBranch,elseBranch)
    }

    /**
     * Represents the grammar rule print.
     *
     * printStmt -> "print" expression ";" ;
     * @return An AST for print statement
     */
    private fun printStatement() : Stmt{
        val expr = expression()
        consume(TokenType.SEMICOLON, "Except ';' after value")
        return Stmt.PrintStmt(expr)
    }

    /**
     * Represents the grammar rule return.
     *
     * return -> "return" expression? ";"
     * @return an AST for return statement
     */
    private fun returnStatement() : Stmt{
        // consumed keyword "return" in statement(), get it using previous()
        val keyword = previous()

        // if there is a return value, parse it
        var value : Expr? = null
        if (!check(TokenType.SEMICOLON)){
            value = expression()
        }

        // consume ";"
        consume(TokenType.SEMICOLON, "Expect ';' after return value.")

        //return Return statement AST
        return Stmt.ReturnStmt(keyword,value)
    }

    /**
     * Represents the grammar rule while.
     *
     * whileStmt -> "while" "(" expression ")" statement;
     * @return An AST for while statement
     */
    private fun whileStatement(): Stmt{
        // keyword while consumed in statement(), consume "("
        consume(TokenType.LEFT_PAREN, "Expect '(' after while.")

        // parse condition
        val condition = expression()

        // consume ")"
        consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition.")

        // parse statement()
        val stmt = statement()

        // return While Statement AST
        return Stmt.WhileStmt(condition,stmt)
    }

    /**
     * Represents the grammar rule block. Instead of return an AST for BlockStatement, returns a list of AST represents ASTs enclosed in the block.
     *
     * block          → "{" declaration* "}" ;
     * @return A lists of AST enclosed in the block.
     */
    private fun block() : List<Stmt>{
        // "{" consumed in statement()
        val statements : MutableList<Stmt> = mutableListOf()

        // parse declarations enclosed in the {} one at a time
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()){
            // it is possible for declaration to return null. If it does, it means parser has encountered an error.
            val statement = declaration()
            // if there is an error, parser will report it before interpreter executes, therefore we can safely ignore it even through we technically should not.
            if (statement != null) statements.add(statement)
        }

        //consume "}"
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")

        // return list of AST
        return statements
    }


    /*

    */
    /**
     * Represents the grammar rule expression. Basically an initialization for Expression AST parsing
     *
     * expression -> assignment;
     * @return An AST for [Expr].
     */
    fun expression() : Expr{
        return assignment()
    }

    /**
     * Represents the grammar rule assignment. It will report an error if assignment target is invalid, but throws nothing.
     *
     * assignment -> ( call "." )? IDENTIFIER "=" assignment | logic_or
     * @return [Expr.Assignment] AST if "A = B" type of expression, or [Expr.Set] if "Class.Property = A" type of expression, otherwise the result of recursive descent.
     */
    private fun assignment() : Expr{
        // Notice that logicOr will eventually decent to call or primary which covers IDENTIFIER
        // if this is an "A = B" type of assignment, we are parsing the l-value
        // otherwise if this is logic or, we are fine because it will not match a token with TokenType equal
        val expr = logicOr()

        // if meet =, we know that it is an assignment
        if (match(TokenType.EQUAL)){
            val equals = previous()
            // parse the right-hand side
            val value = assignment()
            // if expr is a variable, we process "A=B" type of assignment
            if(expr as? Expr.Variable != null){
                val name = expr.name
                return Expr.Assignment(name,value)
            }
            // if the left hand is going be parsed as getter
            // it must be actually a setter (format is "Get.IDENTIFIER = ...)
            else if (expr as? Expr.Get != null){
                return Expr.Set(expr.obj, expr.name, value)
            }

            //Otherwise, record an error but do not throw it.
            //Because if parser receives 1 = "1", the parse can not return an AST, but it is not in panic mode
            error(equals, "Invalid assignment target.")
        }
        //return the AST
        return expr
    }

    /**
     * Represents the grammar rule logic_or.
     *
     * logic_or    -> logic_and ( "or" logic_and )* ;
     * @return An [Expr.Logical] AST if "A or B" type of expression, otherwise the result of recursive descent.
     */
    private fun logicOr() : Expr{
        //parse left-hand side
        var expr = logicAnd()

        // if finds keyword Or, right-hand side exists
        while (match(TokenType.OR)){
            val operator = previous()
            val right = logicAnd()
            expr = Expr.Logical(expr,operator,right)
        }

        //return the AST
        return expr
    }

    /**
     * Represents the grammar rule logic_and.
     *
     * logic_and   -> equality ( "and" equality )* ;
     * @return An AST for [Expr.Logical] if "A and B" type of expression, otherwise the result of recursive descent.
     */
    private fun logicAnd(): Expr{
        //parse left-hand side
        var expr = equality()

        //if parser finds keyword And, right-hand side exists
        while (match(TokenType.AND)){
            val operator = previous()
            val right = expression()
            expr = Expr.Logical(expr, operator, right)
        }

        //return the AST
        return expr
    }

    /**
     * Represents the grammar rule equality.
     *
     * equality   -> comparison (("==" | "!=") comparison)*;
     * @return An [Expr.Binary] AST if "A == B" or "A != B" type of expression, otherwise the result of recursive descent.
     */
    private fun equality() : Expr{
        //parse left-hand side
        var expr = comparison()

        // if match "!=" or "==", right-hand side exist
        while(match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)){
            val operator : Token = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }

        //return the AST
        return expr
    }

    /**
     * Represents the grammar rule comparison.
     *
     * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     * @return An [Expr.Binary] AST if any of the comparison expression, otherwise the result of recursive descent.
     */
    private fun comparison():Expr{
        //parse left-hand side
        var expr = term()

        // if match ">" or ">=" or "<" or "<=", right-hand side exist
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)){
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }

        //return the AST
        return expr
    }

    /**
     * Represents the grammar rule term.
     *
     * term       -> factor (("+" | "-") factor)*;
     * @return An [Expr.Binary] AST if (A+B or A-B) type of expression, otherwise the result of recursive descent.
     */
    private fun term():Expr{
        //parse left-hand side
        var expr = factor()

        // if match "+" or "-", right-hand side exist
        while (match(TokenType.PLUS, TokenType.MINUS)){
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        // return the AST
        return expr
    }

    /**
     * Represents the grammar rule factor.
     *
     * factor         → unary ( ( "/" | "*" ) unary )* ;
     * @return An [Expr.Binary] AST if (A/B or A*B) type of expression, otherwise the result of recursive descent.
     */
    private fun factor():Expr{
        //parse left-hand side
        var expr = unary()

        //if match "/" or "*", right-hand side exist
        while (match(TokenType.STAR, TokenType.SLASH)){
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    /**
     * Represents the grammar rule unary.
     *
     * unary          → ( "!" | "-" ) unary | call ;
     * @return An [Expr.Unary] AST if (!A or -A) type of expression, otherwise the result of recursive descent.
     */
    private fun unary() : Expr{
        //if match "!" or "-", unary
        if(match(TokenType.BANG, TokenType.MINUS)){
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator,right)
        }
        //otherwise call
        return call()
    }

    /**
     * Represents the grammar rule call, here is not an exact match.
     *
     * call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
     * @See [finishCall]
     * @return An [Expr.Call] AST if (A()) type of expression, An [Expr.Get] AST if (A.B) type of expression,  otherwise the result of recursive descent.
     */
    private fun call() : Expr{
        // match the primary
        var expr = primary()

        // two kinds of call, one is get properties, the other function/class call
        // continue parse until no "." or "(" is spotted
        // handle cases like "A.B(a,b)" or A(a,b).B
        while (true){
            // parse all arguments if any
            if (match(TokenType.LEFT_PAREN)){
                expr = finishCall(expr)
            }
            // parse getter
            else if (match(TokenType.DOT)){
                val name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.")
                expr = Expr.Get(expr,name)
            }
            else{
                break
            }
        }
        return expr
    }

    /**
     * Represents the grammar rule arguments, here is not an exact match.
     *
     * arguments -> expression ( "," expression )* ;
     * @return An [Expr.Call] AST.
     */
    private fun finishCall(callee: Expr) : Expr{
        val arguments = mutableListOf<Expr>()
        // check if the next token is ")", if so, reached the end of argument
        if (!check(TokenType.RIGHT_PAREN)){
            // if not, parse argument one at a time.
            do {
                if (arguments.size >= 255){
                    // reports an error but not throw it, because here the parser is still in perfectly valid state.
                    // so it can keep going.
                    error(peek(), "Can't have more than 255 arguments.")
                }
                arguments.add(expression())
            }
            // consume "," in between argument
            while (match(TokenType.COMMA))
        }

        val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after argument")

        // wrap the callee and those arguments up into a call AST node.
        return Expr.Call(callee, paren, arguments)
    }

    /**
     * Represents the grammar rule primary, throw an error if parse error.
     *
     * primary        → "true" | "false" | "nil" | "this" | NUMBER | STRING | IDENTIFIER | "(" expression ")" | "super" "." IDENTIFIER ;
     * @return A expression AST.
     * @throws ParserError if none of the keyword is matched.
     */
    private fun primary() : Expr{
        if (match(TokenType.TRUE)){
            return Expr.Literal(true)
        }
        if (match(TokenType.FALSE)){
            return Expr.Literal(false)
        }
        if (match(TokenType.NIL)){
            return Expr.Literal(null)
        }
        if (match(TokenType.NUMBER, TokenType.STRING)){
            return Expr.Literal(previous().literal)
        }

        if (match(TokenType.LEFT_PAREN)){
            val expr = expression()
            // looks for the right parenthesis
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }

        if (match(TokenType.IDENTIFIER)){
            return Expr.Variable(previous())
        }

        if (match(TokenType.THIS)){
            return Expr.This(previous())
        }
        if (match(TokenType.SUPER)){
            val keyword = previous()
            consume(TokenType.DOT, "Expect '.' after 'super'.")
            val method = consume(TokenType.IDENTIFIER, "Expect superclass method name.")
            return Expr.Super(keyword,method)
        }

        // here if no matching, throw exception
        throw error(peek(), "Expect expression.")
    }

    /**
     * Sends an error to [Lox] to be printed on console, sends the [ParserError] to the calling method.
     *
     */
    private fun error(token: Token, message: String) : ParserError{
        // error method for handling token related error
        Lox.error(token,message)
        return ParserError()
    }

    /*
        helper method: discard tokens until the beginning of the next statement

        the other is keyword, which marks the start of a new statement.
    */
    /**
     * Discards tokens until the beginning of the next statement where parser can function normally.
     */
    private fun synchronize(){
        advance()
        while (!isAtEnd()){
            //semicolon which by designs mark end of statement
            if (previous().type == TokenType.SEMICOLON) return;
            // the following keys marks the start of a new statement.
            when(peek().type){
                TokenType.CLASS -> return
                TokenType.FOR -> return
                TokenType.FUN -> return
                TokenType.IF -> return
                TokenType.PRINT -> return
                TokenType.RETURN -> return
                TokenType.VAR -> return
                TokenType.WHILE -> return
                else -> {}
            }
            // consumes all the tokens if not one of the keywords because an error will compromise parser's ability.
            advance()
        }
    }

    /**
     * Checks if the current Token's TokenType is [type], if so consumes it, otherwise reports an error.
     * @param type the expected tokenType.
     * @param message the message to console in case of an exception.
     * @return the token consumed.
     * @exception [ParserError] if Tokentype of the current token is not [type]
     */
    private fun consume(type: TokenType, message: String):Token{
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    /**
     * Consumes the token to be consumed if its types is one of the [types].
     * @param types a list of expected TokenType.
     * @return true if the tokenType of the current Token is one of [types].
     */
    private fun match(vararg types :TokenType): Boolean{
        for (type in types){
            if(check(type)){
                advance()
                return true
            }
        }
        return false
    }

    /**
     * Consume the token to be consumed and return it
     * @return the token consumed by this method
     */
    private fun advance() : Token{
        if(!isAtEnd()) current++;
        return previous()
    }

    /**
     * Gets the token to be consumed
     * @return the token to be consumed
     */
    private fun peek() : Token{
        return tokens[current]
    }

    /**
     * Gets the token before the token to be consumed
     * @return the token before the token to be consumed
     */
    private fun previous() : Token{
        return tokens[current-1]
    }

    /**
     * Checks if the token to be consumed has type [type].
     * @param type the type of Token to be compared with the token to be consumed.
     * @return true if token to be consumed is of type [type] otherwise false.
     */
    private fun check(type: TokenType) : Boolean{
        if (isAtEnd()) return false;
        return peek().type == type
    }

    /**
     * checks if parser has consumed all tokens other than EOF
     * @return true if all tokens other than EOF are consumed, otherwise false
     */
    private fun isAtEnd() : Boolean{
        return peek().type == TokenType.EOF;
    }
}