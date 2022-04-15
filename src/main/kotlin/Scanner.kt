class Scanner(val source:String) {
    // in kotlin we do not need the final keyword here since it is default
    // on the contrast if we want to extend or override a property or method outside of the class
    // we need to use open
    private var tokens: MutableList<Token> = mutableListOf()

    //pointers on source
    private var start = 0
    private var current = 0

    //location information
    private var line = 1

    // map for reserved world
    private val keywords = mapOf(
        "and" to TokenType.AND,
        "class" to TokenType.CLASS,
        "else" to TokenType.ELSE,
        "false" to TokenType.FALSE,
        "fun" to TokenType.FUN,
        "for" to TokenType.FOR,
        "if" to TokenType.IF,
        "nil" to TokenType.NIL,
        "or" to TokenType.OR,
        "print" to TokenType.PRINT,
        "return" to TokenType.RETURN,
        "super" to TokenType.SUPER,
        "this" to TokenType.THIS,
        "true" to TokenType.TRUE,
        "var" to TokenType.VAR,
        "while" to TokenType.WHILE
    )

    /*
    * return a list of token based on input
     */
    fun scanTokens() : List<Token> {
        while(!isAtEnd()){
            // here we at the beginning of next (possible) lexeme.
            start = current
            scanToken()
        }
        // at the end of file
        // add EOF
        tokens.add(Token(TokenType.EOF, "", null, line))
        return tokens;
    }

    /*
    * Helper function, determine if all lexemes have been tokenized
     */
    private fun isAtEnd() : Boolean{
        return current >= source.length
    }
    /*
    * Recognize Lexemes and turn them into tokens
     */
    private fun scanToken(){
        //the scanner will scan the entire input even if it spots errors
        when(val c = advance()){
            //Single character lexeme
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)

            //one or two character lexeme that share common prefix
            '!' -> addToken(if(match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> addToken(if(match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> addToken(if(match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if(match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)

            // for "/" we need to check if it is a comment, if so, we ignore the line
            '/' -> if(match('/')) {
                while (peek() != '\n' && !isAtEnd()) advance()
                }
                else{
                    addToken(TokenType.SLASH)
                }

            // ignore whitespace
            ' ' -> {}
            '\r' -> {}
            '\t' -> {}

            // new lines, not a lexeme but we need to increase the line count.
            '\n' -> line++

            '"' -> string()

            else ->
                // number literal
                // too much work to check each char, so we put in default
                if (isDigit(c)){
                    number()
                }
                //identifier literal
                //identifier can not start with number
                else if(isAlpha(c)){
                    identifier()
                }
                //unrecognized lexeme, in lox's language spec, ex. @#^
                else {
                    Lox.error(line, "Unexpected character.")
                }
        }
    }

    /*
    * help method, return the next character in the source
     */
    private fun advance() : Char{
        return source[current++]
    }

    /*
    * helper function if the lexeme has no literal mining
     */
    private fun addToken(type: TokenType){
        addToken(type,null)
    }

    /*
    * create a token based on the lexeme and add to tokenlist
     */
    private fun addToken(type: TokenType, literal: Any?){
        val text = source.substring(start, current)
        tokens.add(Token(type,text, literal, line))
    }

    /*
    * if meet the expected string
    * consume it
    * ex. when meet "!" we want to check if the following character is "="
    * if it is, we consume the "=" as well and decide that the lexeme is "!="
    * otherwise we do not consume the character and decide the lexeme is "="
     */
    private fun match(expected: Char) : Boolean{
        if (isAtEnd()) return false
        if (source[current] != expected) return false
        current ++
        return true
    }

    /*
    * helper function: lookahead without consuming
     */
    private fun peek() : Char{
        if (isAtEnd()) return '\u0000'
        return source[current]
    }

    /*
    * tokenize strings
     */
    private fun string() {
        // lox support multi-line strings
        while(peek() != '"' && !isAtEnd()){
            if(peek() == '\n') line++
            advance()
        }

        //if not seeing a closed quote when reaching to the end
        //report an error
        if(isAtEnd()){
            Lox.error(line,"Unterminated String.")
            return
        }

        //consume the close quote
        advance()

        //the literal value of a string is in between the quotes.
        //notice that Lox do not support escape sequence like \n
        //so no need to handle those
        val value = source.substring(start + 1, current -1)
        addToken(TokenType.STRING, value)
    }

    /*
    * helper functions: determine if c is a digit
     */
    private fun isDigit(c:Char):Boolean{
        return c in '0'..'9'
    }


    /*
    * Tokenize number
    * Support 123; 12.3
    * Not support 123.; .123
     */
    private fun number(){
        while (isDigit(peek())) advance()

        if (peek() =='.' && isDigit(peekNext())){
            advance()
            while(isDigit(peek())) advance()
        }

        addToken(TokenType.NUMBER, source.substring(start,current).toDouble())
    }

    /*
    * helper function: lookahead two characters
     */
    private fun peekNext() : Char{
        if (current + 1 >= source.length) return '\u0000'
        return source[current+1]
    }

    /*
    * helper functions: determine if c is alphabetical or underscore
     */
    private fun isAlpha(c:Char):Boolean{
        return (c in 'A'..'Z') || (c in 'a'..'z') || c=='_'
    }

    /*
    * tokenize reserved words or identifiers
     */
    private fun identifier(){
        while(isAlphaNumberic(peek())) advance()
        // we can only know for sure the lexeme is a reserved word until we reach to the end
        // not we do not need to know that
        // not of which requires types only, no literal attached
        //begin assuming all lexeme starting with a letter or underscore is an identifier
        val text = source.substring(start,current)
        // check if the text exists in our keyword list, if so it is a reserved word, else it is an identifier
        val type = keywords[text] ?: TokenType.IDENTIFIER
        addToken(type)
    }

    private fun isAlphaNumberic(c: Char) : Boolean{
        return isAlpha(c) || isDigit(c)
    }


}