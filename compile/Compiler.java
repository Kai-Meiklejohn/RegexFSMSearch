package compile;

import java.util.*;

/**
 * Compiler.java
 * Author: Kai Meiklejohn (1632448)
 * 
 * handles parsing and compiling regexps into FSMs.
 */
public class Compiler {
    // fsm representation using ArrayLists for dynamic sizing
    static List<String> type = new ArrayList<>(); // "BR", "WC", or literal
    static List<Integer> next1 = new ArrayList<>();
    static List<Integer> next2 = new ArrayList<>();
    static int nextState = 1; // start allocating from state 1 (state 0 is special)

    // input regexp and position
    static String re;
    static int pos;

    // fragment class for NFA fragments
    public static class Frag {
        public int start;
        public int end;
        public Frag(int s, int e) { start = s; end = e; }
    }

    // initialize parser/compiler
    public static void init(String regexp) {
        re = regexp;
        pos = 0;
        nextState = 1; // reset state counter
        // clear and prepare lists for a new compilation
        type.clear();
        next1.clear();
        next2.clear();
        // pre-allocate state 0 (will be set by REcompile)
        type.add(null);
        next1.add(-1);
        next2.add(-1);
    }

    // print FSM in required format
    public static void printFSM() {
        // iterate up to the current number of states created
        for (int i = 0; i < nextState; i++) {
            // handle potential null state 0 before it's set
            String t = type.get(i) == null ? "NULL" : type.get(i);
            System.out.printf("%d,%s,%d,%d\n", i, t, next1.get(i), next2.get(i));
        }
    }

    // set a state
    public static void setstate(int s, String t, int n1, int n2) {
        // ensure list capacity if needed (should be handled by newState)
        while (s >= type.size()) {
             type.add(null);
             next1.add(-1);
             next2.add(-1);
        }
        type.set(s, t);
        next1.set(s, n1);
        next2.set(s, n2);
    }

    // overload for char type
    public static void setstate(int s, char t, int n1, int n2) {
        if (t == ' ') setstate(s, "BR", n1, n2);
        else if (t == '.') setstate(s, "WC", n1, n2);
        else setstate(s, String.valueOf(t), n1, n2);
    }

    // parse and compile the whole regexp
    public static Frag expression() {
        Frag left = term();
        while (peek() == '|') {
            eat('|');
            // create the branch state *after* the left term, *before* the right term
            int s = newState("BR", left.start, -1); // placeholder for right term start
            Frag right = term();
            next2.set(s, right.start); // use set for existing state

            // create a common end state for the alternation
            int e = newState("BR", -1, -1);
            patch(left.end, e); // patch end of left fragment to the new end state
            patch(right.end, e); // patch end of right fragment to the new end state
            left = new Frag(s, e); // resulting fragment uses the branch as start, new state as end
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
            // handle empty term (e.g., empty parens or start/end of input)
            // create a NOP state (BR to itself, effectively)
            int s = newState("BR", -1, -1);
            patch(s, s); // point NOP to itself
            return new Frag(s, s);
        }
        return left;
    }

    // factor: base + closure/option
    private static Frag factor() {
        Frag f = null;
        while (true) {
            char c = peek();
            if (c == '*') {
                if (f == null) {
                    throw new RuntimeException("Invalid syntax: '*' cannot appear without a preceding base");
                }
                eat('*');
                if (peek() == '*') {
                    throw new RuntimeException("Invalid syntax: multiple consecutive '*' operators are not allowed");
                }
                int s = newState("BR", f.start, -1); // branch to start of f, or skip
                patch(f.end, s); // loop back from end of f to branch
                patch(s, -1, false); // placeholder for skipping (will be patched later)
                f = new Frag(s, s); // new fragment is just the branch state
            } else if (c == '+') {
                if (f == null) {
                    throw new RuntimeException("Invalid syntax: '+' cannot appear without a preceding base");
                }
                eat('+');
                // f+ is equivalent to ff*
                int s = newState("BR", f.start, -1); // branch for the loop (*) part
                patch(f.end, s); // loop back
                patch(s, -1, false); // placeholder for exiting loop
                // the fragment starts at f.start, ends at the branch s
                f = new Frag(f.start, s);
            } else if (c == '?') {
                if (f == null) {
                    throw new RuntimeException("Invalid syntax: '?' cannot appear without a preceding base");
                }
                eat('?');
                int s = newState("BR", f.start, -1); // branch to start of f, or skip
                // need an end state for the optional part
                int e = newState("BR", -1, -1);
                patch(f.end, e); // end of f goes to the new end state
                patch(s, e, false); // skipping the branch also goes to the new end state
                f = new Frag(s, e); // new fragment starts at branch, ends at new end state
            } else {
                if (f == null) {
                    f = base();
                } else {
                    break;
                }
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
            if (lit == 0) throw new RuntimeException("Invalid escape sequence at end of input");
            int s = newState(String.valueOf(lit), -1, -1);
            return new Frag(s, s);
        } else if (c != 0 && !isSpecial(c)) {
            eat(c);
            int s = newState(String.valueOf(c), -1, -1);
            return new Frag(s, s);
        } else {
            // this case should ideally not be reached with valid regex
            // if it is (e.g., empty group () ), return a NOP fragment
             int s = newState("BR", -1, -1);
             patch(s, s);
             return new Frag(s, s);
        }
    }

    // utility: create a new state
    private static int newState(String t, int n1, int n2) {
        int s = nextState++; // get current next available state index and increment
        // add the new state's data to the lists
        type.add(t);
        next1.add(n1);
        next2.add(n2);
        return s;
    }

    // utility: create a new end state (BR, -1, -1)
    public static int newEndState() {
        return newState("BR", -1, -1);
    }

    // patch the end state(s) of a fragment to point to target
    public static void patch(int s, int target) {
        if (s == -1) return; // avoid patching null states

        if (type.get(s) != null && type.get(s).equals("BR")) {
            // for BR states, patch *all* dangling (-1) transitions to the target.
            boolean patched = false;
            if (next1.get(s) == -1) {
                next1.set(s, target);
                patched = true;
            }
            if (next2.get(s) == -1) {
                next2.set(s, target);
                patched = true;
            }
        } else { // literal or WC state
            next1.set(s, target);
            next2.set(s, target);
        }
    }

    // patch only the branch (for ? operator)
    private static void patch(int s, int target, boolean both) {
         if (s == -1) return; // avoid patching null states
        if (type.get(s) != null && type.get(s).equals("BR")) {
            if (both) {
                // this case might not be needed if 'both' is only used for non-BR
                if (next1.get(s) == -1) next1.set(s, target);
                if (next2.get(s) == -1) next2.set(s, target);
            } else {
                // prefer patching next2 first for optional path (skip path)
                if (next2.get(s) == -1) next2.set(s, target);
                else if (next1.get(s) == -1) next1.set(s, target); // fallback if next2 was already patched
            }
        } else {
            // patching a non-BR state for ? doesn't make sense in Thompson's construction
            // if called, update both transitions.
            next1.set(s, target);
            next2.set(s, target);
        }
    }

    // parsing helpers
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
        else throw new RuntimeException("Expected '" + c + "' at pos " + pos + ", found '" + peek() + "'");
    }
    private static boolean isSpecial(char c) {
        return c == '(' || c == ')' || c == '*' || c == '+' || c == '?' || c == '|' || c == '.' || c == '\\';
    }
    private static boolean endOfTerm() {
        char c = peek();
        return c == 0 || c == ')' || c == '|';
    }
}
