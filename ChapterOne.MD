#Chapter One Note
##Scanner
Scanner takes in raw source code as a series of characters and groups it into a series of chunks we call tokens.
##REPL
**R**ead a line from input, **E**valuate it, **P**rint the result, then **L**oop and do it all over again. 
##Error Report
Notice that the Error Report Mechanism is in the Lox class instead of scanning and other phases where the error might actually occur.<br>
It is a good practice to separate the code taht generates the errors from the code that reports them.<br>
Otherwise the implementation will be filled with error reporting, not exactly easy to read especially when considering large implementation.<br>
Ideally, we can have an actual abstraction, for example an "ErrorReporter" Interface that gets passed to the scanner and parser so taht we can swap out different reporting strategies.<br>
##Lexemes
Smallest sequences that represent meaningful information for the program language.<br>
Lexical analysis -> scan through the list of characters(from the line of code) and group them together into the smallest sequences that still representing something.<br>
Ex: <ins>var</ins> <ins>language</ins> <ins>=</ins> <ins>"lox"</ins> <ins>;</ins><br>
##Token
Token{Tokentype, lexeme; literal; location}<br>
- Tokentype : 
  - which kind of lexeme it represents. Ex. Identifier, reserved words
  - Parser requires that information
- lexeme: 
  - the original "text" shown in code
- literal : 
  - some lexeme represents literal values. Ex. number, string
  - convert lexeme to value if it has one, otherwise null
  - For interpreter later.
-location: 
  - the whereabout on that "text" in the code
  - error report require this information
##Problem
One small problem, because we assume that all identifiers will start with letter or underscore.<br>
Therefore, an illegal input, say 1abc will be tokenized to NUMBER 1 1.0 and IDENTIFIER abc null.<br>
This won't be a problem in the actual implementation because Lox require a ";" at the end of each statement.  