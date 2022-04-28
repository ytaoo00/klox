class Parser(private val tokens : List<Token>) {
    // pointer to the token to be parsed
    private var current = 0

    // customer exception class, extend the kotlin Exception class.
    class ParserError : Exception()

    /*
        initialization
        parser will take in a line/one file of plain text
        and try to return a list of statement.
        This is also a direct translation of the program rule.
        program   -> declaration* ;
     */
    fun parse(): List<Stmt> {
        // here we use a val because we want to add but we do not want to reassign
        val statements : MutableList<Stmt> = mutableListOf()
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

    /*
    declaration -> varDecl | statement ;
    */
    private fun declaration() : Stmt?{
        try {
            if(match(TokenType.VAR)) return varDeclaration()
            return statement()
        } catch (error : ParserError){
            synchronize()
            return null
        }
    }
    /*
    varDecl     -> "var" IDENTIFIER ( "=" expression)? ";" ;
    */
    private fun varDeclaration() : Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")

        var initializer : Expr? = null;

        if (match(TokenType.EQUAL)){
            initializer = expression()
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration")

        return Stmt.VarStmt(name, initializer)
    }

    /*
    expression -> assignment;
    */
    fun expression() : Expr{
        return assignment()
    }

    /*
    assignment -> IDENTIFIER "=" assignment | logic_or
    for now we only deal with variable
    */
    private fun assignment() : Expr{
        // parse the l-value
        val expr = logicOr()
        // if meet =, we know that it is an assignment
        if (match(TokenType.EQUAL)){
            val equals = previous()
            // parse the right-hand side
            val value = assignment()
            // if expr is also a variable
            // recall that variable has expression but one Token
            if(expr as? Expr.Variable != null){
                val name = expr.name
                return Expr.Assignment(name,value)
            }
            error(equals, "Invalid assignment target")
        }
        return expr
    }
    /*
    logic_or    -> logic_and ( "or" logic_and )* ;
     */
    private fun logicOr() : Expr{
        var expr = logicAnd()

        while (match(TokenType.OR)){
            val operator = previous()
            val right = logicAnd()
            expr = Expr.Logical(expr,operator,right)
        }
        return expr
    }

    /*
        logic_and   -> equality ( "and" equality )* ;
     */
    private fun logicAnd(): Expr{
        var expr = equality()
        while (match(TokenType.AND)){
            val operator = previous()
            val right = expression()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }
    /*
    statement -> exprStmt | ifStmt | printStmt | block ;
     */
    private fun statement() : Stmt{
        // check if printStmt
        if (match(TokenType.PRINT)) return printStatement()
        // check if block
        if (match(TokenType.LEFT_BRACE)) return Stmt.BlockStmt(block())
        // check if ifStmt
        if (match(TokenType.IF)) return ifStatement()
        // check if while statement
        if (match(TokenType.WHILE)) return whileStatement()
        // check if for statement
        if (match(TokenType.FOR)) return forStatement()
        return expressionStatement()
    }
    /*
    ifStmt    -> "if" "(" expression ")" statement ("else" statement)? ;
     */
    private fun ifStatement() : Stmt{
        consume(TokenType.LEFT_PAREN, "Expect '(' after if.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.")
        val thenBranch = statement()
        var elseBranch : Stmt? = null
        if (match(TokenType.ELSE)){
           elseBranch = statement()
        }
        return Stmt.IfStmt(condition,thenBranch,elseBranch)
    }

    /*
    whileStmt -> "while" "(" expression ")" statement;
     */
    private fun whileStatement(): Stmt{
        consume(TokenType.LEFT_PAREN, "Expect '(' after while.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition.")
        val stmt = statement()

        return Stmt.WhileStmt(condition,stmt)
    }

    /*
    forStmt     -> "for" "(" ( varDecl | exprStmt | ";")
                expression? ";"
                expression? ")" statement;
     */
    private fun forStatement() : Stmt{
        consume(TokenType.LEFT_PAREN, "Expect '(' after For.")

        // initializer
        val initializer : Stmt? = if (match(TokenType.SEMICOLON)){
            null
        } else if (match(TokenType.VAR)){
            varDeclaration()
        } else{
            expressionStatement()
        }

        // condition
        var condition : Expr? = if(!check(TokenType.SEMICOLON)){
            expression()
        }else{
            null
        }

        consume(TokenType.SEMICOLON, "Expect ';' after For Loop Condition.")

        // increment
        val increment : Expr? = if (!check(TokenType.RIGHT_PAREN)){
            expression()
        }else {
            null
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after For Loop Clauses.")

        // body
        var body = statement()

        // transform to block
        // if increment is not null, we add that to the end
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
        if (initializer != null){
            body = Stmt.BlockStmt(listOf(initializer,body))
        }

        return body
    }


    private fun block() : List<Stmt>{
        val statements : MutableList<Stmt> = mutableListOf()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()){
            // again we can safely ignore null here because the only case it will return a null is statement parse was wrong
            declaration()?.let { statements.add(it) }
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    /*
        fun for "matching" the non-terminal grammar
        equality   -> comparison (("==" | "!=") comparison)*;
     */
    private fun equality() : Expr{
        // first non-terminal comparison
        var expr = comparison()
        // not know for sure if other things exists
        // so check
        while(match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)){
            // if we successfully math the desired token
            // assign the token consumed to be the operator
            val operator : Token = previous()
            // the second non-terminal comparison
            val right = comparison()
            // now here we convert the "parser tree" to AST
            expr = Expr.Binary(expr, operator, right)
        }
        //return the AST
        return expr
    }
    /*
        fun for "matching" the non-terminal grammar
        comparison -> term ((">" | ">=" | "<" | "<=" ) term)*;
     */
    private fun comparison():Expr{
        var expr = term()
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)){
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    /*
        fun for "matching" the non-terminal grammar
        term       -> factor (("+" | "-") factor)*;
    */
    private fun term():Expr{
        var expr = factor()
        while (match(TokenType.PLUS, TokenType.MINUS)){
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    /*
        fun for "matching" the non-terminal grammar
        factor     -> unary (("*" | "/") unary)* ;
    */
    private fun factor():Expr{
        var expr = unary()
        while (match(TokenType.STAR, TokenType.SLASH)){
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }
    /*
        fun for "matching" the non-terminal grammar
        unary      -> ("!" | "-") unary | primary;
    */
    private fun unary() : Expr{
        if(match(TokenType.BANG, TokenType.MINUS)){
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator,right)
        }
        return primary()
    }

    /*
        fun for "matching" the non-terminal grammar
        primary    -> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ");"
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
        // here if no matching, throw exception
        throw error(peek(), "Expect expression.")
    }
    /*
    fun for matching the expression statement grammar
    exprStmt  -> expression ";" ;
     */
    private fun expressionStatement() : Stmt{
        val expr = expression()
        consume(TokenType.SEMICOLON, "Except ';' after value")
        return Stmt.ExpressionStmt(expr)
    }

    /*
    fun for matching the expression statement grammar
    printStmt -> "print" expression ";" ;
     */
    private fun printStatement() : Stmt{
        val expr = expression()
        consume(TokenType.SEMICOLON, "Except ';' after value")
        return Stmt.PrintStmt(expr)
    }

    /*
        helper method: check if the next token is of the expected type
        if so, consumes the token
        otherwise, report an error
     */
    private fun consume(type: TokenType, message: String):Token{
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    /*
        helper method: reports the error at a given tokens
        Notice that the error() fun returns the error instead of throwing it
        because we want to let the calling method inside the parser decide whether to unwind it or not.
    */
    private fun error(token: Token, message: String) : ParserError{
        // error method for handling token related error
        Lox.error(token,message)
        return ParserError()
    }

    /*
        helper method: discard tokens until the beginning of the next statement
        Two signals: one is semicolon which by design mark end of statement
        the other is keyword, which marks the start of a new statement.
    */
    private fun synchronize(){
        advance()
        while (!isAtEnd()){
            if (previous().type == TokenType.SEMICOLON) return;
            when(peek().type){
                TokenType.CLASS -> return
                TokenType.FOR -> return
                TokenType.FUN -> return
                TokenType.IF -> return
                TokenType.PRINT -> return
                TokenType.RETURN -> return
                TokenType.VAR -> return
                TokenType.WHILE -> return
                else -> return
            }
            advance()
        }
    }

    /*
        helper method: takes a number of TokenType as parameter
        if so, consumes the token and return true
        else return false.
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

    /*
        helper method: takes one TokenType
        returns true if the token to be consumed is of the given type
     */
    private fun check(type: TokenType) : Boolean{
        if (isAtEnd()) return false;
        return peek().type == type
    }

    /*
        helper method: consumes the current token and return it
     */
    private fun advance() : Token{
        if(!isAtEnd()) current++;
        return previous()
    }

    /*
        helper method: return true if the token to be consumed is EOF
     */
    private fun isAtEnd() : Boolean{
        return peek().type == TokenType.EOF;
    }

    /*
        helper method: return the token to be consumed
     */
    private fun peek() : Token{
        return tokens[current]
    }

    /*
        helper method: return the token before the token to be consumed
        primarily used to get the token consumed
        ....
        consumed the token
        token_consumed = previous()
     */
    private fun previous() : Token{
        return tokens[current-1]
    }


}