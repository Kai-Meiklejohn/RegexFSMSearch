package compile;

/**
 * Compiler.java
 * Author: Kai Meiklejohn (1632448)
 * Holds our three arrays and the grammar routines:
 *   expression(), term(), factor(), atom()
 * Builds an NFA in the form of three parallel arrays.
 */
public class Compiler {
    /** simple pair to hold entry and exit states of a submachine */
    public static class Frag {
        public final int start, end;
        public Frag(int s, int e) { start = s; end = e; }
    }

    private static final int MAX = 10_000;  // adjust as needed

    // the three arrays, indexed by state number
    private static char ch[]    = new char[MAX];
    private static int  next1[] = new int[MAX];
    private static int  next2[] = new int[MAX];

    private static String pattern;  // the regexp string
    private static int    j;        // current parse position
    private static int    state;    // next free state index

    /** Initialise everything before parsing */
    public static void init(String raw) {
        // collapse "**" → "*"
        pattern = raw.replace("**", "*");
        j       = 0;
        state   = 1;   // reserve state 0 for the wrapper
    }

    /** Record one state in the arrays */
    public static void setstate(int s, char c, int n1, int n2) {
        ch[s]    = c;
        next1[s] = n1;
        next2[s] = n2;
    }

    /** Print out states 0 through state-1 in the required format */
    public static void printFSM() {
        for (int i = 0; i < state; i++) {
            String type;
            if      (ch[i] == '.') type = "WC";
            else if (ch[i] == ' ') type = "BR";
            else                   type = Character.toString(ch[i]);
            System.out.println(i + "," + type + "," + next1[i] + "," + next2[i]);
        }
    }

    //
    // Grammar from spec:
    //   expression → term ( '|' term )*
    //   term       → factor+
    //   factor     → atom ( '*' | '+' | '?' )*
    //   atom       → literal | '.' | '\'literal | '(' expression ')'
    //

    /** expression → term ( '|' term )* */
    public static Frag expression() {
        Frag left = term();
        while (j < pattern.length() && pattern.charAt(j) == '|') {
            j++;                    // skip '|'
            Frag right = term();
            // build new branch + accept
            int b = state++;
            int e = state++;
            setstate(b, ' ', left.start, right.start);
            setstate(e, ' ', e, e);
            // patch former ends → new accept
            setstate(left.end,  ' ', e, e);
            setstate(right.end, ' ', e, e);
            left = new Frag(b, e);
        }
        return left;
    }

    /** term → factor factor … (at least one) */
    public static Frag term() {
        Frag f = factor();
        // implicit concatenation: as long as next token can start an atom
        while (j < pattern.length() && canStartAtom(pattern.charAt(j))) {
            Frag g = factor();
            // patch f’s end → g’s start
            setstate(f.end, ' ', g.start, g.start);
            f = new Frag(f.start, g.end);
        }
        return f;
    }

    /** factor → atom ( '*' | '+' | '?' )* */
    public static Frag factor() {
        Frag f = atom();
        while (j < pattern.length()) {
            char op = pattern.charAt(j);
            if (op == '*') {
                j++;
                // closure: new branch+accept
                int b = state++;
                int e = state++;
                setstate(b, ' ', f.start, e);
                setstate(e, ' ', f.start, e);
                f = new Frag(b, e);
            }
            else if (op == '+') {
                j++;
                // plus = one occurrence (f) then closure
                // build closure on f:
                int b = state++;
                int e = state++;
                setstate(b, ' ', f.start, e);
                setstate(e, ' ', f.start, e);
                // patch original end → loop
                setstate(f.end, ' ', f.start, e);
                f = new Frag(f.start, e);
            }
            else if (op == '?') {
                j++;
                // zero-or-one
                int b = state++;
                int e = state++;
                setstate(b, ' ', f.start, e);
                setstate(e, ' ', e, e);
                f = new Frag(b, e);
            }
            else break;
        }
        return f;
    }

    /** atom → literal | '.' | '\'literal | '(' expression ')' */
    public static Frag atom() {
        if (j >= pattern.length())
            throw new RuntimeException("Unexpected end of pattern");

        char c = pattern.charAt(j++);
        // grouping
        if (c == '(') {
            Frag sub = expression();
            if (j >= pattern.length() || pattern.charAt(j) != ')')
                throw new RuntimeException("Missing ')'");
            j++;
            return sub;
        }
        // wildcard
        else if (c == '.') {
            int s = state++;
            int e = state++;
            setstate(s, '.', e, e);
            setstate(e, ' ', e, e);
            return new Frag(s, e);
        }
        // escape
        else if (c == '\\') {
            if (j >= pattern.length())
                throw new RuntimeException("Trailing '\\'");
            char lit = pattern.charAt(j++);
            int s = state++;
            int e = state++;
            setstate(s, lit, e, e);
            setstate(e, ' ', e, e);
            return new Frag(s, e);
        }
        // literal
        else {
            int s = state++;
            int e = state++;
            setstate(s, c, e, e);
            setstate(e, ' ', e, e);
            return new Frag(s, e);
        }
    }

    /** Helper: which chars can start an atom? */
    private static boolean canStartAtom(char c) {
        return c == '(' || c == '.' || c == '\\' || (c != ')' && c != '|' 
               && c != '*' && c != '+' && c != '?');
    }
}
