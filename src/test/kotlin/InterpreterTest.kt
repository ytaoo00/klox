import org.junit.Test
import org.junit.jupiter.api.Assertions.*

internal class InterpreterTest{
    @Test
    fun isEqualTest(){
        val data = listOf<List<Any>>(
            listOf(true, "true"),
            listOf(true, 1)
        )
        data.forEach { pair ->
            println(pair[0] == pair[1])
        }
    }
}