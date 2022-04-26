import java.io.IOException

class Parser(private val tokens : List<Token>) {
    // pointer to the token to be parsed
    private var current = 0

    // customer exception class, extend the kotlin Exception class.
    class ParserError : Exception()

    /*
        initialization
     */
    fun parse(): Expr? {
        return try{
            expression()
        } catch (error : ParserError){
            null
        }
    }

    /*
    expression -> equality;
    */
    fun expression() : Expr{
        return equality()
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
        // here if no matching, throw exception
        throw error(peek(), "Expect expression.")
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