package compile;

/**
 * Compiler.java
 * Author: Kai Meiklejohn (1632448)
 * Holds our three arrays and the grammar routines:
 *   expression(), term(), factor(), atom()
 * Builds a nondeterministic FSM in three parallel arrays.
 */
public class Compiler {
    /** simple pair to hold entry and exit states of a submachine */
    public static class Frag {
        public final int start, end;
        public Frag(int s, int e) { start = s; end = e; }
    }

    private static final int MAX = 10_000;

    // the three parallel arrays, indexed by state #
    private static char ch[]    = new char[MAX];
    private static int  next1[] = new int[MAX];
    private static int  next2[] = new int[MAX];

    private static String pattern;  // the regexp string
    private static int    j;        // current parse position
    private static int    state;    // next free state index

    /** initialise before parsing */
    public static void init(String raw) {
        // collapse "**" into "*"
        pattern = raw.replace("**", "*");
        j       = 0;
        state   = 1;   // reserve state 0 for the top-level branch
    }

    /** Record one state in the arrays */
    public static void setstate(int s, char c, int n1, int n2) {
        ch[s]    = c;
        next1[s] = n1;
        next2[s] = n2;
    }

    /** print states 0 … state-1 as "id,type,next1,next2" */
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
            j++;                     // skip '|'
            Frag right = term();

            // make new branch + accept
            int b = state++;
            int e = state++;
            setstate(b, ' ', left.start, right.start);
            // patch both sub-machines to go to e
            setstate(left.end,  ' ', e, e);
            setstate(right.end, ' ', e, e);
            // accept state has no outgoing transitions
            setstate(e, ' ', -1, -1);

            left = new Frag(b, e);
        }
        return left;
    }

    /** term → factor factor … (at least one) */
    public static Frag term() {
        Frag f = factor();
        while (j < pattern.length() && canStartAtom(pattern.charAt(j))) {
            Frag g = factor();
            // concatenate: patch f's accept to g.start
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
                // closure: new branch b, accept e
                int b = state++;
                int e = state++;
                setstate(b, ' ', f.start, e);
                setstate(f.end, ' ', f.start, e);
                setstate(e, ' ', -1, -1);
                f = new Frag(b, e);
            }
            else if (op == '+') {
                j++;
                // plus = one occurrence then zero-or-more
                // patch f.end → loop back & fall through to new accept
                int e = state++;
                setstate(f.end, ' ', f.start, e);
                setstate(e, ' ', -1, -1);
                f = new Frag(f.start, e);
            }
            else if (op == '?') {
                j++;
                // zero-or-one: branch b to f.start or e
                int b = state++;
                int e = state++;
                setstate(b, ' ', f.start, e);
                setstate(e, ' ', -1, -1);
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
            setstate(e, ' ', -1, -1);
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
            setstate(e, ' ', -1, -1);
            return new Frag(s, e);
        }
        // literal
        else {
            int s = state++;
            int e = state++;
            setstate(s, c, e, e);
            setstate(e, ' ', -1, -1);
            return new Frag(s, e);
        }
    }

    /** Helper: which chars can start an atom? */
    private static boolean canStartAtom(char c) {
        return c == '(' || c == '.' || c == '\\'
            || (c != '|' && c != ')' && c != '*' && c != '+' && c != '?');
    }
}
