import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readLines
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.todo

@RunWith(value = Parameterized::class)
class LoxTest(val paramOne: String, val paramTwo : String, val paramThree: String) {

    private var outContent = ByteArrayOutputStream()
    private var errContent = ByteArrayOutputStream()
    /**
     * Turns on stdOut output capture
     */
    private fun captureOut() {
        System.setOut(PrintStream(outContent,true))
    }

    /**
     * Turns on stdErr output capture
     */
    private fun captureErr() {
        System.setErr(PrintStream(errContent,true))
    }

    /**
     * Turns off stdOut capture and returns the contents
     * that have been captured
     *
     * @return
     */
    private fun getOut(): String? {
        System.setOut(PrintStream(FileOutputStream(FileDescriptor.out)))
        return outContent.toString().replace("\r".toRegex(), "")
    }

    /**
     * Turns off stdErr capture and returns the contents
     * that have been captured
     *
     * @return
     */
    private fun getErr(): String? {
        System.setErr(PrintStream(FileOutputStream(FileDescriptor.out)))
        return errContent.toString().replace("\r".toRegex(), "")
    }

    companion object {

        val expectedOutputPattern = Regex("// expect: ?(.*)")
        val expectedErrorPattern = Regex("// (Error.*)")
        val expectedRuntimeErrorPattern = Regex("// expect runtime error: (.+)")
        val nonTestPattern = Regex("// nontest")

        val earlyChapter = listOf(
            //earlier chapter
            "scanning",
            "expressions",
            "benchmark",
//            "class",
//            "inheritance",
//            "regression",
//            "super"
        )



        var noJavaLimit = listOf(
            "loop_too_large.lox",
            "no_reuse_constants.lox",
            "too_many_constants.lox",
            "too_many_locals.lox",
            "too_many_upvalues.lox",
            // Rely on JVM for stack overflow checking.
            "stack_overflow.lox",
            //earlier chapter
            "scanning",
            "expressions",
            //JVM doesn't correctly implement IEEE equality on boxed doubles.
            "nan_equality.lox"
        )

        @JvmStatic
        @Parameters
        fun data(): Collection<Array<Any>> {
            val projectDirAbsolutePath = Paths.get("").toAbsolutePath().toString()
            val testResourcesPath = Paths.get(projectDirAbsolutePath, "/src/test/resources")
            val fileResultMap = mutableListOf<Array<Any>>()
            // all the file need to check
            val paths = Files.walk(testResourcesPath)
                .filter { item -> item.toString().endsWith(".lox") }
                .filter { item ->
                    val tokens = item.toString().split('\\')
                    val inFolder = tokens[tokens.size - 2]
                    !earlyChapter.contains(inFolder)
                }
                .filter { item ->
                    val tokens = item.toString().split('\\')
                    val loxFile = tokens[tokens.size - 1]
                    !noJavaLimit.contains(loxFile)
                }

            // get the expected outcome for each file
            paths.forEach { path ->
                // first we parse the file to check the expected outcome
                val lines = path.readLines()
                var expected = ""
                var errorType = "No Error"
                lines.forEachIndexed { index, line ->
                    val lineNumber = index + 1
                    // flag for error
                    // first match runtime error
                    val runTimeError = expectedRuntimeErrorPattern.find(line)?.value
                    if (runTimeError != null) {
                        errorType = "Runtime"
                        expected = runTimeError.substring(25) + "\n" + "[line $lineNumber]" + "\n"
                    }
                    //then match no error
                    val result = expectedOutputPattern.find(line)?.value
                    if (result != null) {
                        expected += result.substring(11)
                        // correct line breaker
                        expected += "\n"
                    }
                    // if no match then we can expect a parser error
                    val error = expectedErrorPattern.find(line)?.value
                    if (error != null) {
                        errorType = "Parser"
                        expected = expected + "[line $lineNumber] " + error.substring(3) + "\n"
                    }
//                    val runTimeError = expectedRuntimeErrorPattern.find(line)?.value
//                    if (runTimeError != null) {
//                        expected = runTimeError.substring(25) + "\n" + "[line $lineNumber]"
//                    }
//                    val result = expectedOutputPattern.find(line)?.value
//                    if (result != null) {
//                        expected += result.substring(11)
//                        // correct line breaker
//                        expected += "\n"
//                    }
                }
                fileResultMap.add(arrayOf(path.toString(), expected, errorType))
            }
            return fileResultMap.toList()
        }
    }

    @Test
    fun shouldReturnExpectedOutput() {
        println("testing $paramOne")
        if (paramThree == "No Error"){
//            println("has no Error")
            captureOut()
            val bytes = File(paramOne).readBytes()
            Lox.run(String(bytes))
            val output = getOut()
            assertEquals(paramTwo, output)
        }
        else if (paramThree == "Parser"){
//            println("has Parser Error")
            captureErr()
            val bytes = File(paramOne).readBytes()
            Lox.run(String(bytes))
            val output = getErr()
            assertEquals(paramTwo, output)
        } else{
//            println("has Runtime Error")
            captureErr()
            val bytes = File(paramOne).readBytes()
            Lox.run(String(bytes))
            val output = getErr()
            assertEquals(paramTwo, output)
        }
            println("passed $paramOne")
    }
}
