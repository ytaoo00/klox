/**
 * Scanner class
 *
 * @property source the raw input steam of source code
 * @constructor create a Scanner Instance
 */

class Scanner(private val source:String) {
    /**
     * A list of generated token
     */
    private var tokens: MutableList<Token> = mutableListOf()

    /**
     * Pointer to the first character in the lexeme being scanned.
     */
    private var start = 0 // point

    /**
     * Pointer to the character currently being considered
     */
    private var current = 0

    /**
     * The source line [current] is on.
     */
    private var line = 1

    /**
     * A map that match the reserved words to the corresponding tokenType
     */
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

    /**
     * adding tokens until it reaches the end
     * @return a list of generated token
     */
    fun scanTokens() : List<Token> {
        // if not at the end of the file
        while(!isAtEnd()){
            // set the start position to the beginning of the current lexeme
            start = current
            // turn the current lexeme in to token
            scanToken()
        }
        // at the end of file
        // add EOF as a token
        tokens.add(Token(TokenType.EOF, "", null, line))
        return tokens
    }

    /**
     * Recognize a lexeme and turn it into a [Token]
     */
    private fun scanToken(){
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

            // comment or operation
            '/' ->
                // if sees "//" then comment
                if(match('/')) {
                    // move forward until line ends or reach end
                    while (peek() != '\n' && !isAtEnd()) advance()
                }
                // if just a standalone '/' then it is an operator
                else{
                    addToken(TokenType.SLASH)
                }

            // ignore whitespace
            ' ' -> {}
            '\r' -> {}
            '\t' -> {}

            // new lines, not a lexeme but need to increase the line count.
            '\n' -> line++

            // double quote mark the start of a string
            '"' -> string()

            // [1..9] or [a..z|A..Z]
            else ->
                // number literal
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
                    // record the error but not report it
                    Lox.error(line, "Unexpected character.")
                }
        }
    }

    /**
     * Handles tokens without literal values
     */
    private fun addToken(type: TokenType){
        addToken(type,null)
    }

    /**
     * Get the text for the current lexeme, creates a [Token] for it, and add the token to [tokens]
     * @param type the Type of token to be added
     * @param literal the literal value for the token
     */
    private fun addToken(type: TokenType, literal: Any?){
        val text = source.substring(start, current)
        tokens.add(Token(type,text, literal, line))
    }

    /**
     * Turns number into token
     */
    private fun number(){
        //advance until reach a non-digit character
        while (isDigit(peek())) advance()

        //if that number is a dot and the character is a digit
        if (peek() =='.' && isDigit(peekNext())){
            // move forward the pointer until reach a non-digit character
            advance()
            while(isDigit(peek())) advance()
        }

        //Lox treats all numbers as double at runtime
        addToken(TokenType.NUMBER, source.substring(start,current).toDouble())
    }

    /**
     * Determines if the current lexeme is a reserved word or identifier and turns it into the correct token
     */
    private fun identifier(){
        // assuming all lexemes starting with letter or underscore is an identifier
        // move the current pointer to the end of the lexeme
        while(isAlphaNumeric(peek())) advance()
        // get the text value
        val text = source.substring(start,current)
        // check if the text exists in the keyword list, if so it is a reserved word, else it is an identifier
        val type = keywords[text] ?: TokenType.IDENTIFIER
        // either the reserved words or identifier will have a literal value in the corresponding token
        addToken(type)
    }

    /**
     * Turns string to [Token]
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

    /**
     * Checks if scanner points to the end of file
     * @return true if scanner reaches the end of file, otherwise returns false
     */
    private fun isAtEnd() : Boolean{
        return current >= source.length
    }

    /**
     * Moves the scanner forward by one character
     * @return the next character in [source]
     */
    private fun advance() : Char{
        return source[current++]
    }

    /**
     * Consumes and returns true if the current character is [expected], otherwise return false and keep the scanner pointing at the original location.
     * @param expected the character wanted
     * @return true if the current character is [expected], otherwise false
     */
    private fun match(expected: Char) : Boolean{
        if (isAtEnd()) return false
        // if the current character is not expected, return false
        if (source[current] != expected) return false
        // if the current character is expected, we consume it and return true
        current ++
        return true
    }

    /**
     * Looks the current character without consuming that character
     * @return the current character
     */
    private fun peek() : Char{
        if (isAtEnd()) return '\u0000'
        return source[current]
    }

    /**
     * Looks up the next character
     * @return the next character, if end of file, return '\u0000'
     */
    private fun peekNext() : Char{
        if (current + 1 >= source.length) return '\u0000'
        return source[current+1]
    }

    /**
     * Checks if the current character is a digit
     * @return true if the current character is a digit, otherwise false
     */
    private fun isDigit(c:Char):Boolean{
        return c in '0'..'9'
    }

    /**
     * Checks if the current character is a letter or an underscore
     * @return true if the current character is letter or underscore, otherwise false
     */
    private fun isAlpha(c:Char):Boolean{
        return (c in 'A'..'Z') || (c in 'a'..'z') || c=='_'
    }

    /**
     * Checks if the current character is number or letter or underscore
     * @return true if the current character is number or letter or underscore, otherwise false
     */
    private fun isAlphaNumeric(c: Char) : Boolean{
        return isAlpha(c) || isDigit(c)
    }
}