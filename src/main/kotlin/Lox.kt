import java.io.*
import kotlin.system.exitProcess

class Lox {
    companion object{
        // this is the primary reason we have the error function here
        //
        var hadError = false

        /*
        * Initialize script running
        * convert file to string that can be used as parameter for fun run
         */
        @Throws(IOException::class)
        fun runFile(path : String) {
            val bytes = File(path).readBytes()
            run(String(bytes))
            // if we fun an error in scanning/parsing, we do not want to execute the code
            if (hadError) {
                exitProcess(65)
            }
        }

        /*
        * Initialize REPL
        * Prepare console input as parameter to fun run.
         */
        fun runPrompt(){
            val input = InputStreamReader(System.`in`)
            val reader = BufferedReader(input)
            while(true){
                println("> ")
                //elvis operator
                //String = readline() if readLine() returns nonnull string
                //else say we type Control-D in the command line
                //an end of file signal is triggered
                //we break the loop hence stop the program
                //Expected an EOF token in this situation
                val line : String = reader.readLine() ?: break;
                run(line)
                // reset the hadError to false
                // in case some errors happens in the previous line
                // we do not want to stop the program.
                hadError = false
            }
        }

        /*
        * Core function
        * Interpreter runs
        * Order: Tokenize -> ...
         */
        fun run(source:String){
            val scanner = Scanner(source)
            val tokens = scanner.scanTokens()
            // for now just print token
            for(token in tokens){
                println(token)
            }
            // TODO: 4/15/2022 Parsing. AST, etc
        }

        /*
        * So far the bare bone for error reporting
        * The idea is
        * Say you have function(first,second,)
        * The error reporting mechanism will show something like
        * 15 function(first,second,) Unexpected ","
        * We leave out the where section for now.
         */
        fun error(line: Int, message: String){
            report(line, "", message)
            // TODO: 4/15/2022 create an ErrorReporter Interface and pass the interface to Scanner, Parser, etc.
        }

        /*
        * Helper function for error reporting
         */
        fun report(line: Int, where: String, message: String){
            println("[line $line] Error $where : $message")
            hadError = true
        }
    }
}

//main class
/*
* Run klox from command line prompt if no input
* Run klox script on given location if contains a lox file.
 */
fun main(args: Array<String>) {

    if(args.size > 1) {
        println("Usage klox [script]")
        exitProcess(64)
    }
    //start klox and give it a file path
    //if file not found, throw an exception.
    else if(args.size == 1){
        try {
            Lox.runFile(args[0])
        }
        catch (e: IOException){
            println(e.message)
            exitProcess(1)
        }
    }
    //interactively interface
    else{
        Lox.runPrompt()
    }
}
