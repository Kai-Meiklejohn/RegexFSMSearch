package compile;

/**
 * REcompile.java
 * Author: Kai Meiklejohn (1632448)
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

        // 2) parse & compile whole regexp → returns (start,end) pair
        Compiler.Frag machine = Compiler.expression();

        // 3) wrap with state 0 branching to real start
        Compiler.setstate(0, ' ', machine.start, machine.start);

        // 4) emit states 0 … (nextState-1)
        Compiler.printFSM();
    }
}
