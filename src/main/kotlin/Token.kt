/**
 * Representation of Lox Token
 *
 * @property type Type for each keyword, operator, bit of punctuation, and literal type.
 * @property lexeme Raw character in the original input stream
 * @property lexeme Literal value of the token if any.
 * @property line Location on where the token is located, for Klox only line number is recorded
 * @constructor create a new Token
 */
class Token(val type: TokenType, val lexeme: String, val literal: Any?, val line: Int) {

    override fun toString(): String {
        return "$type $lexeme $literal"
    }

}
