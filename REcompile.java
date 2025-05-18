

/**
 * REcompile.java
 * Author: Kai Meiklejohn 
 * 
 * Entry point: read one regexp, build FSM, print it.
 */
public class REcompile {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java compile.REcompile \"<regexp>\"");
            System.exit(1);
        }

        // 1) initialise parser/compiler
        Compiler.init(args[0]);

        // 2) parse & compile whole regexp → returns (start,end) of NFA
        Compiler.Frag machine = Compiler.expression();

        // 2a) make sure we consumed the entire string
        if (Compiler.pos < Compiler.re.length() || Compiler.parenCount != 0) {
            throw new RuntimeException("Unmatched parentheses or extra input at pos " 
                                       + Compiler.pos);
        }

        // 3) create explicit end state (BR, -1, -1)
        int endState = Compiler.newEndState();
        Compiler.patch(machine.end, endState);

        // 4) wrap with state 0 branching to real start
        Compiler.setstate(0, ' ', machine.start, machine.start); // Branch only to start

        // 5) emit states 0 … (nextState-1)
        Compiler.printFSM();
    }
}
