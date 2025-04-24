package compile;

// REcompile.java
// Author: Kai Meiklejohn (1632448)// Compiler.java
// Holds our 3 arrays and grammar methods: expression(), term(), factor(), atom()

public class Compiler {
    // max number of states we can build
    private static final int MAX = 1000;

    // three arrays as in lectures
    private static char   ch[]    = new char[MAX];
    private static int    next1[] = new int[MAX];
    private static int    next2[] = new int[MAX];
    private static int    state;        // next free state index
    private static String pattern;      // the regexp
    private static int    j;            // current position in pattern

    /** Initialise before parsing */
    public static void init(String raw) {
        // collapse ** → * if you like
        pattern = raw.replace("**", "*");
        j = 0;
        state = 1;         // reserve 0 for the automatic start-branch
    }

    /** Fill in one state record */
    public static void setstate(int s, char c, int n1, int n2) {
        ch[s]    = c;
        next1[s] = n1;
        next2[s] = n2;
    }

    /** Print all states 0…state-1 */
    public static void printFSM() {
        for (int i = 0; i < state; i++) {
            String type = (ch[i] == '.') ? "WC"
                         : (ch[i] == ' ') ? "BR"
                         : Character.toString(ch[i]);
            System.out.println(i + "," + type + "," + next1[i] + "," + next2[i]);
        }
    }

    //
    // Grammar from lectures / spec:
    //   expression → term ( '|' term )*
    //   term       → factor+
    //   factor     → atom ( '*' | '+' | '?' )*
    //   atom       → literal | '.' | '\'literal | '(' expression ')'
    //

    /** expression → term ( '|' term )* */
    public static int expression() {
        int start1 = term();
        while (j < pattern.length() && pattern.charAt(j) == '|') {
            j++;                      // skip '|'
            int start2 = term();      // parse right side
            int b = state++;          // new branch state
            setstate(b, ' ', start1, start2);
            start1 = b;               // branch is now entry
        }
        return start1;
    }

    /** term → factor factor … (at least one) */
    public static int term() {
        int s = factor();
        // as long as next char can start an atom
        while (j < pattern.length()
               && pattern.charAt(j) != ')'
               && pattern.charAt(j) != '|') {
            int s2 = factor();
            // concatenation = just link the end of s → start of s2
            // (in a full impl you’d patch the accept state of s to s2)
            // for skeleton we’ll pretend every factor is 2-state
            // and s is its start:
            int a = state++;        // new accept state
            setstate(a, ' ', s2, s2);
            setstate(s+1, ' ', s2, s2);
            s = s;  // start of the combined machine
        }
        return s;
    }

    /** factor → atom ( '*' | '+' | '?' )* */
    public static int factor() {
        int s = atom();
        while (j < pattern.length()) {
            char c = pattern.charAt(j);
            if (c == '*') {
                j++;
                int b = state++;
                int e = state++;
                setstate(b, ' ', s, e);  // branch: 0 or more
                setstate(e, ' ', s, e);
                s = b;
            }
            else if (c == '+') {
                j++;
                // same as a a*  ⇒ concat(s, star(s))
                int starB = state++;
                int starE = state++;
                setstate(starB, ' ', s, starE);
                setstate(starE, ' ', s, starE);
                // link s's accept to starB and reuse s
                s = s;  // start stays same
            }
            else if (c == '?') {
                j++;
                int b = state++;
                int e = state++;
                setstate(b, ' ', s, e); // zero or one
                setstate(e, ' ', e, e);
                s = b;
            }
            else break;
        }
        return s;
    }

    /** atom → literal | '.' | '\'literal | '(' expression ')' */
    public static int atom() {
        if (j >= pattern.length())
            throw new RuntimeException("Unexpected end of pattern");

        char c = pattern.charAt(j++);
        if (c == '(') {
            int s = expression();
            if (j>=pattern.length() || pattern.charAt(j) != ')')
                throw new RuntimeException("Missing ')'");
            j++;
            return s;
        }
        else if (c == '.') {
            int s = state++;
            int e = state++;
            setstate(s, '.', e, e);
            return s;
        }
        else if (c == '\\') {
            if (j>=pattern.length())
                throw new RuntimeException("Trailing '\\'");
            char lit = pattern.charAt(j++);
            int s = state++;
            int e = state++;
            setstate(s, lit, e, e);
            return s;
        }
        else {
            // literal
            int s = state++;
            int e = state++;
            setstate(s, c, e, e);
            return s;
        }
    }
}
