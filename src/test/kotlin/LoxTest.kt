import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class LoxTest {

    @Test
    fun testInput(){
        val expected = "PRINT print null\nIDENTIFIER a null\nSEMICOLON ; null\nEOF  null"

        Lox.run("print a;")
    }


}