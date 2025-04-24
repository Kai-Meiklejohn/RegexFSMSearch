# Pattern Search

## Team
- **Student 1**: Kai Meiklejohn, 1632448  
- **Student 2**: Riley Cooney, 1632444  

## Compile

### Overview
This part of the project involves building a non-deterministic finite-state machine (FSM) from a given regular expression (regexp). The FSM is printed in plain-text form, one state per line, and can be used to scan text files for matching substrings.

### Files

- `REcompile.java`: Main driver that reads a regular expression, invokes the compiler, and prints the FSM.  
- `Compiler.java`: Contains methods for parsing the regular expression and constructing the FSM.  
- `sample.txt`: A small example text file for testing purposes.

### Compiling

```bash
cd compile
javac REcompile.java Compiler.java
```

### Running

```bash
# Build FSM from your regexp and save to fsm.txt
java REcompile "(a|b)*abb" > fsm.txt

# View the first few states
head -n 5 fsm.txt
```

Each line of `fsm.txt` has the form:

```<state#>,<type>,<next1>,<next2>```

type is either:

- a single literal character (e.g. a, b, +, \(, …),
- `WC` for wildcard `.` (matches any character), or
- `BR` for a branching state (splits into two paths).

### What it Does (in Simple Steps)

1. **Initialise**
   - Collapse any `**` into `*`.
   - Store the regexp in `pattern`, set read-pointer `j = 0`, and reserve `state = 1` (state 0 is used later).

2. **Parse & Compile**
   - Follow the grammar from lectures:

```text
expression → term ( '|' term )*
term       → factor+
factor     → atom ( '*' | '+' | '?' )*
atom       → literal | '.' | '\'literal | '(' expression ')'
```

   - `expression()`: Handles `|` by creating a `BR` state that splits to each alternative.
   - `term()`: Chains factors (implicit concatenation) by linking end-states.
   - `factor()`: Builds loops or optional paths for `*`, `+`, `?`.
   - `atom()`: Creates 2-state machines for literals, `.`, or parenthesised sub-expressions.

3. **Wrap & Print**
   - After parsing, let `entry` be the start of the machine.
   - Create state 0: `BR → entry, entry`.
   - Print states 0 through `state-1` in order.

### Pseudocode

```text
init(pattern):
    pattern = pattern.replace("**", "*")
    j = 0
    state = 1    // reserve 0 for wrapper

setstate(s, c, n1, n2):
    ch[s]    = c
    next1[s] = n1
    next2[s] = n2

expression():
    start1 = term()
    while (peek() == '|'):
        consume('|')
        start2 = term()
        b = state++
        setstate(b, ' ', start1, start2)
        start1 = b
    return start1

term():
    s = factor()
    while (canStartAtom(peek())):
        s2 = factor()
        patchAcceptOf(s) to startOf(s2)
    return s

factor():
    s = atom()
    while (peek() in {'*','+','?'}):
        op = consume()
        if op == '*':  buildStarAround(s)
        if op == '+':  buildPlusAround(s)
        if op == '?':  buildQuestionAround(s)
        s = newBranchState
    return s

atom():
    if peek() == '(':
        consume('(')
        s = expression()
        consume(')')
        return s
    else if peek() == '.':
        consume('.')
        s = state++; e = state++
        setstate(s, '.', e, e)
        return s
    else if peek() == '\\':
        consume('\\')
        lit = consume()
        s = state++; e = state++
        setstate(s, lit, e, e)
        return s
    else:
        lit = consume()
        s = state++; e = state++
        setstate(s, lit, e, e)
        return s

main():
    init(args[0])
    entry = expression()
    setstate(0, ' ', entry, entry)
    for i in 0…state-1:
        print i, typeOf(ch[i]), next1[i], next2[i]
```

### Examples of Legal & Illegal Inputs

| Input       | Legal? | Notes                          |
|-------------|--------|--------------------------------|
| `a`         | Yes    | single literal                |
| `.`         | Yes    | wildcard                      |
| `ab*`       | Yes    | concatenation + closure       |
| `(a|b)+c?`  | Yes    | alternation, closure, optional|
| `\*a\?`    | Yes    | literals `*` and `?` escaped  |
| `a**`       | Yes    | collapsed to `a*`             |
| `a+b`       | Yes    | alternation                   |
| `a++`       | No     | double `+` is invalid         |
| `(`         | No     | unmatched parenthesis         |
| `a(b`       | No     | missing `)`                  |
| ``a`        | No     |                              |
| `*a`        | No     | `*` without preceding atom    |
| `a\`        | No     | trailing backslash            |

---

## Search

*(To be completed by Riley Cooney)*
