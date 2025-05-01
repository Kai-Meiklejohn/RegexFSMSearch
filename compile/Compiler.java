package compile;

import java.util.*;

/**
 * Compiler.java
 * Handles parsing and compiling regexps into FSMs.
 */
public class Compiler {
    // Max number of states (arbitrary, can be increased)
    static final int MAX = 256;
    // FSM state arrays
    static String[] type = new String[MAX]; // "BR", "WC", or literal
    static int[] next1 = new int[MAX];
    static int[] next2 = new int[MAX];
    static int nextState = 1; // 0 is reserved for the initial branch

    // Input regexp and position
    static String re;
    static int pos;

    // Fragment class for NFA fragments
    public static class Frag {
        public int start;
        public int end;
        public Frag(int s, int e) { start = s; end = e; }
    }

    // Initialize parser/compiler
    public static void init(String regexp) {
        re = regexp;
        pos = 0;
        nextState = 1;
        Arrays.fill(type, null);
        Arrays.fill(next1, -1);
        Arrays.fill(next2, -1);
    }

    // Print FSM in required format
    public static void printFSM() {
        for (int i = 0; i < nextState; i++) {
            System.out.printf("%d,%s,%d,%d\n", i, type[i], next1[i], next2[i]);
        }
    }

    // Set a state
    public static void setstate(int s, String t, int n1, int n2) {
        type[s] = t;
        next1[s] = n1;
        next2[s] = n2;
    }

    // Overload for char type
    public static void setstate(int s, char t, int n1, int n2) {
        if (t == ' ') setstate(s, "BR", n1, n2);
        else if (t == '.') setstate(s, "WC", n1, n2);
        else setstate(s, String.valueOf(t), n1, n2);
    }

    // Parse and compile the whole regexp
    public static Frag expression() {
        Frag left = term();
        while (peek() == '|') {
            eat('|');
            // Create the branch state *after* the left term, *before* the right term
            int s = newState("BR", left.start, -1); // Placeholder for right term start
            Frag right = term();
            next2[s] = right.start; // Set the second branch target

            // Create a common end state for the alternation
            int e = newState("BR", -1, -1);
            patch(left.end, e); // Patch end of left fragment to the new end state
            patch(right.end, e); // Patch end of right fragment to the new end state
            left = new Frag(s, e); // Resulting fragment uses the branch as start, new state as end
        }
        return left;
    }

    // term: sequence of factors
    private static Frag term() {
        Frag left = null;
        while (true) {
            if (endOfTerm()) break;
            Frag f = factor();
            if (left == null) left = f;
            else {
                patch(left.end, f.start);
                left = new Frag(left.start, f.end);
            }
        }
        if (left == null) {
            // Should not happen for valid regexps
            int s = newState("BR", -1, -1);
            return new Frag(s, s);
        }
        return left;
    }

    // factor: base + closure/option
    private static Frag factor() {
        Frag f = base();
        while (true) {
            char c = peek();
            if (c == '*') {
                eat('*');
                int s = newState("BR", f.start, -1);
                patch(f.end, s);
                f = new Frag(s, s);
            } else if (c == '+') {
                eat('+');
                int s = newState("BR", f.start, -1);
                patch(f.end, s);
                f = new Frag(f.start, s);
            } else if (c == '?') {
                eat('?');
                int s = newState("BR", f.start, -1);
                int e = newState("BR", -1, -1);
                patch(f.end, e);
                patch(s, e, false); // patch only the branch
                f = new Frag(s, e);
            } else {
                break;
            }
        }
        return f;
    }

    // base: literal, wildcard, parenthesis, escaped
    private static Frag base() {
        char c = peek();
        if (c == '(') {
            eat('(');
            Frag f = expression();
            eat(')');
            return f;
        } else if (c == '.') {
            eat('.');
            int s = newState("WC", -1, -1);
            return new Frag(s, s);
        } else if (c == '\\') {
            eat('\\');
            char lit = next();
            int s = newState(String.valueOf(lit), -1, -1);
            return new Frag(s, s);
        } else if (c != 0 && !isSpecial(c)) {
            eat(c);
            int s = newState(String.valueOf(c), -1, -1);
            return new Frag(s, s);
        } else {
            // Should not happen for valid regexps
            int s = newState("BR", -1, -1);
            return new Frag(s, s);
        }
    }

    // Utility: create a new state
    private static int newState(String t, int n1, int n2) {
        int s = nextState++;
        setstate(s, t, n1, n2);
        return s;
    }

    // Utility: create a new end state (BR, -1, -1)
    public static int newEndState() {
        return newState("BR", -1, -1);
    }

    // Patch the end state(s) of a fragment to point to target
    public static void patch(int s, int target) {
        if (s == -1) return; // Avoid patching null states

        if (type[s] != null && type[s].equals("BR")) {
            // For BR states, patch any dangling (-1) transitions.
            // This ensures that states like the end of an alternation (which is BR)
            // correctly point to the final machine end state with both transitions.
            if (next1[s] == -1) {
                next1[s] = target;
            }
            if (next2[s] == -1) {
                next2[s] = target;
            }
        } else { // Literal or WC state
            // Non-BR states have a single logical exit transition.
            next1[s] = target;
            next2[s] = target;
        }
    }

    // Patch only the branch (for ? operator)
    private static void patch(int s, int target, boolean both) {
        if (type[s] != null && type[s].equals("BR")) {
            if (both) {
                next1[s] = target;
                next2[s] = target;
            } else {
                if (next2[s] == -1) next2[s] = target;
                else next1[s] = target;
            }
        } else {
            next1[s] = target;
            next2[s] = target;
        }
    }

    // Parsing helpers
    private static char peek() {
        if (pos >= re.length()) return (char)0;
        return re.charAt(pos);
    }
    private static char next() {
        if (pos >= re.length()) return (char)0;
        return re.charAt(pos++);
    }
    private static void eat(char c) {
        if (peek() == c) pos++;
        else throw new RuntimeException("Expected '" + c + "' at pos " + pos);
    }
    private static boolean isSpecial(char c) {
        return c == '(' || c == ')' || c == '*' || c == '+' || c == '?' || c == '|' || c == '.' || c == '\\';
    }
    private static boolean endOfTerm() {
        char c = peek();
        return c == 0 || c == ')' || c == '|';
    }
}
