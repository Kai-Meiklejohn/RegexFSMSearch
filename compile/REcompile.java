package compile;

// REcompile.java
// Author: Kai Meiklejohn (1632448)
// Entry point: read one regexp, build FSM, print it.

public class REcompile {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java REcompile \"<regexp>\"");
            System.exit(1);
        }

        // 1) initialise parser/compiler
        Compiler.init(args[0]);

        // 2) parse & compile whole regexp → returns start state of machine
        int entry = Compiler.expression();

        // 3) make state 0 branch to real start
        Compiler.setstate(0, ' ', entry, entry);

        // 4) print all states 0…(nextState-1)
        Compiler.printFSM();
    }
}
