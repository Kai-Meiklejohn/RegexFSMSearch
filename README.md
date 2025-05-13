# Pattern Search

## Overview

This program is a two-part system for working with regular expressions and searching text files. It consists of:

1. **Compile**: Converts a regular expression into a finite-state machine (FSM) using `REcompile.java`. The FSM is printed in plain-text format, with each line representing a state and its transitions.
2. **Search**: Uses the FSM to search for matches in a text file using `REsearch.java`. The program reads the FSM from standard input and scans the file for matching lines.

### How to Use

1. **Compile the Programs**:
   - Navigate to the respective directories and compile the Java files:
     ```bash
     cd compile
     javac compile/REcompile.java compile/Compiler.java
     cd ../search
     javac compile/REsearch.java
     ```

2. **Run the Programs Together**:
   - Use a pipe to connect the output of `REcompile` to the input of `REsearch`:
     ```bash
     java compile.REcompile "<regular_expression>" | java search.REsearch <filename>
     ```
   - Example:
     ```bash
     java compile.REcompile "hugged" | java search.REsearch examples/simple.txt
     ```

3. **What Happens**:
   - `REcompile` builds the FSM for the given regular expression and outputs it.
   - `REsearch` reads the FSM and scans the specified file for lines that match the regular expression.
   - Matching lines are printed to the terminal.

### Example Workflow

- To search for the word "hugged" in `examples/simple.txt`:
  ```bash
  java compile.REcompile "hug*ed" | java search.REsearch examples/simple.txt
  ```
  Output:
  ```
  the mouse was hugged by the cat . 
  the jumbo bird hugged the mouse . 
  the mouse hugged a cat .
  a mouse hugged the dog .
  the cat was hugged by the bird .
  the fish hugged the fish .
  the mouse hugged the dog .
  the bird was hugged by a cat .
  the cat hugged a dog .
  a cat was hugged by the bird .
  ```

## Team
- **Student 1**: Kai Meiklejohn  
- **Student 2**: Riley Cooney  

## 

## Compile

### Overview
This part of the project involves building a non-deterministic finite-state machine (FSM) from a given regular expression (regex). The FSM is printed in plain-text form, one state per line, and can be used to scan text files for matching substrings.

### Files

- `REcompile.java`: Main driver that reads a regular expression, invokes the compiler, and prints the FSM.  
- `Compiler.java`: Contains methods for parsing the regular expression and constructing the FSM.  
- `examples/simple.txt`: A small example text file for testing purposes.

### Compiling

```bash
javac compile/REcompile.java compile/Compiler.java
```

### Running

```bash
# Build FSM from your regex and save to fsm.txt
java compile.REcompile "(a|b)*abb"
```

Each line of the output has the form:

```<state#>,<type>,<next1>,<next2>```

Where:
- `<state#>`: The state number.
- `<type>`: The type of the state, which can be:
  - A single literal character (e.g., `a`, `b`, `+`, `(`, etc.).
  - `WC` for wildcard `.` (matches any character).
  - `BR` for a branching state (splits into two paths).
- `<next1>` and `<next2>`: The indices of the next states.

### What it Does (in Simple Steps)

1. **Initialize**
   - Store the regex in `regexString`, set the read-pointer `position = 0`, and reserve `currentStateIndex = 1` (state 0 is used later).

2. **Parse & Compile**
   - Follow the grammar:

```text
expression → term ( '|' term )*
term       → factor+
factor     → base ( '*' | '+' | '?' )*
base       → literal | '.' | '\' literal | '(' expression ')'
```

   - `expression()`: Handles `|` by creating a `BR` state that splits to each alternative.
   - `term()`: Chains factors (implicit concatenation) by linking end-states.
   - `factor()`: Builds loops or optional paths for `*`, `+`, `?`.
   - `base()`: Creates 2-state machines for literals, `.`, or parenthesized sub-expressions.

3. **Wrap & Print**
   - After parsing, let `startState` be the start of the machine.
   - Create state 0: `BR → startState, startState`.
   - Print states 0 through `currentStateIndex - 1` in order.

### Pseudocode

```text
init(regex):
    regexString = regex
    position = 0
    currentStateIndex = 1    // reserve 0 for wrapper

addState(type, next1, next2):
    ch[currentStateIndex] = type
    next1[currentStateIndex] = next1
    next2[currentStateIndex] = next2
    currentStateIndex++

expression():
    start1 = term()
    while (peek() == '|'):
        eat('|')
        start2 = term()
        branch = addState('BR', start1, start2)
        start1 = branch
    return start1

term():
    s = factor()
    while (canStartBase(peek())):
        s2 = factor()
        connectState(s.endState, s2.startState)
        s = Frag(s.startState, s2.endState)
    return s

factor():
    s = base()
    while (peek() in {'*','+','?'}):
        op = eat()
        if op == '*':  s = buildStar(s)
        if op == '+':  s = buildPlus(s)
        if op == '?':  s = buildQuestion(s)
    return s

base():
    if peek() == '(':
        eat('(')
        s = expression()
        eat(')')
        return s
    else if peek() == '.':
        eat('.')
        s = addState('WC', -1, -1)
        return Frag(s, s)
    else if peek() == '\':
        eat('\')
        lit = eat()
        s = addState(lit, -1, -1)
        return Frag(s, s)
    else:
        lit = eat()
        s = addState(lit, -1, -1)
        return Frag(s, s)

main():
    init(args[0])
    startState = expression()
    addState('BR', startState, startState)  // state 0
    for i in range(currentStateIndex):
        print(i, ch[i], next1[i], next2[i])
```

### Examples of Legal & Illegal Inputs

| Input       | Legal? | Notes                          |
|-------------|--------|--------------------------------|
| `a`         | Yes    | single literal                |
| `.`         | Yes    | wildcard                      |
| `ab*`       | Yes    | concatenation + closure       |
| `(a|b)+c?`  | Yes    | alternation, closure, optional|
| `\*a\?`     | Yes    | literals `*` and `?` escaped  |
| `a+b`       | Yes    | alternation                   |
| `a*`        | No     | can't have more than one `*`  |
| `a++`       | No     | double `+` is invalid         |
| `(`         | No     | unmatched parenthesis         |
| `a(b`       | No     | missing `)`                   |
| `a)b`       | No     | missing `(`                   |
| `*a`        | No     | `*` without preceding base    |
| `a\`        | No     | trailing backslash            |

### Regular Expression Symbol Cheat Sheet

| Symbol        | Meaning                                        | Example    | Matches                       | Doesn’t Match           |
|---------------|------------------------------------------------|------------|-------------------------------|-------------------------|
| **Literal**   | Any non-special character matches itself       | `a`        | `"a"`                         | `"b"`, `""`             |
| `.`           | Wildcard: matches **any one** character        | `.`        | `"x"`, `"9"`, `"@"`           | `""` (empty string)     |
| **Concatenation**<br>(juxtaposition) | Adjacent patterns match in sequence      | `ab`       | `"ab"`                        | `"a"`, `"b"`, `"abc"`   |
| `*`           | Closure: **0 or more** of the preceding item   | `a*`       | `""`, `"a"`, `"aaaa"`         | `"b"`                   |
| `+`           | **1 or more** of the preceding item            | `a+`       | `"a"`, `"aa"`, `"aaaaa"`      | `""`                    |
| `?`           | **0 or 1** of the preceding item               | `a?`       | `""`, `"a"`                   | `"aa"`                  |
| `|`           | Alternation: match **either** side             | `a|b`      | `"a"`, `"b"`                  | `"ab"`                  |
| `(` `)`       | Grouping: treat sub-expression as one unit     | `(ab)*`    | `""`, `"ab"`, `"abab"`        | `"a"`, `"ba"`           |
| `\` (escape)  | Next character is **literal**, not special     | `\*`       | `"*"`                         | `"a"`, `"**"`           |

---

### Operator Precedence (highest to lowest)

| Precedence | Operators            | Description                                      |
|------------|----------------------|--------------------------------------------------|
| 1          | Escaped (`\x`)       | Makes `x` a literal, even if it’s normally special |
| 2          | Grouping (`(` `)`)   | Controls order of evaluation                     |
| 3          | Repetition (`*`, `+`, `?`) | Apply to the immediately preceding item       |
| 4          | Concatenation        | Juxtaposition (e.g. `ab` means `a` then `b`)     |
| 5          | Alternation (`|`)    | Lowest: split into two separate paths            |

---

## Search

### Overview

This part of the project implements the **pattern searcher**, which reads an FSM from standard input (such as one produced by `REcompile`) and scans a text file line-by-line. It prints each line that contains a substring **recognised by the FSM**.

### Files

- `REsearch.java`: The searcher program.

---

### Compiling

```bash
cd RegexFSMSearch
javac -d . search/REsearch.java
```
### Running

#### from file

```bash
Get-Content fsm.txt | java -cp . search.REsearch simple.txt
```

#### from pipe

```bash
java compile.REcompile "(a|b)*abb" | java search.REsearch simple.txt
```

### What it does

Reads FSM from standard input and loads each state into arrays ch, next1, and next2.

Parses input file line by line.

For each line:

Runs an NFA-style simulation (using a custom deque) from every character position.

Follows all possible epsilon-transitions (BR) and literal matches.

If an accepting state (where both next1 and next2 are -1) is reached, prints the line once.

## Pseudocode

```bash
main(args):
    if args.length != 1:
        print error and exit

    filename = args[0]
    fsmLines = read from stdin

    maxState = find highest state number
    init ch[], next1[], next2[] arrays

    for each line in fsmLines:
        parse and populate state arrays

    for each line in input file:
        if searchLine(line):
            print line

searchLine(line):
    for each character position in line:
        current = {0}
        while current has states:
            if accepting state in current:
                return true
            match current states with current char
            move to next states
        add state 0 again for next starting position
    return false
```

### Example

given this FSM (fsm.txt)

```bash
0,BR,9,9
1,a,2,2
2,a,3,3
3,r,4,4
4,d,5,5
5,v,6,6
6,a,7,7
7,r,8,8
8,k,15,15
9,BR,1,10
10,z,11,11
11,e,12,12
12,b,13,13
13,r,14,14
14,a,15,15
15,BR,16,16
16,BR,-1,-1
```

and this input file (simple.txt)

```bash
the cat chased a fish . 
the bird was found by the fish . 
the fish found a bird . 
an aardvark was cheated by the fish . 
the mouse was hugged by the cat . 
a cat found the fish . 
a cat cheated a dog . 
a zebra was found by the dog . 
the fish may cheat the king . 
the dog was loved by a jumbo fish . 
the bird cheats the fish . 
the gold was panned by the king . 
the cat found the wish . 
the cat was cheated by the dog . 
the fish was cooked in a pan . 
a cat loved the bird . 
the dish was found by the cat . 
the bird was chased by a cat . 
the cat cheated the mouse . 
a fish cheated a dog . 
the mouse was found by the dog . 
the jumbo bird hugged the mouse . 
the fish chased the dog . 
the cat or bird was cheated by the dog . 
the dog was kissed by the bird . 
a cat was found by the dog . 
the fish was cheated by an aardvark . 
a bird cheated the fish . 
the fish found a mouse . 
the bird chased a bird . 
a mouse was chased by the cat . 
the bird was chased by the bird . 
the dog found the bird . 
the cat was chased by the cat . 
the dog was found by the fish . 
the fish loved the dog . 
the fish kissed a mouse . 
the bird was kissed by the fish . 
the cat was cheated by the mouse . 
a cat cheated the mouse . 
a bird was loved by the dog . 
a fish found the mouse . 
a kingfisher will chase a fish . 
a ruler loved the king . 
the kingdom was ruled by a bird . 
the mouse hugged a cat . 
the bird kissed a cat . 
a mouse hugged the dog . 
the fish was panned by a dog . 
a bird kissed the cat . 
the dog will chase a bird . 
the fish was cheated by a fish . 
the cat kissed a fish . 
a bird was chased by a bird . 
the mouse was loved by the bird . 
the mouse found the house . 
a bird was kissed by a fish . 
the bird was kissed by the mouse . 
the dog was cheated by a cat . 
a cat loved the mouse . 
the dog was kissed by the dog . 
the cat was hugged by the bird . 
the fish hugged the fish . 
a dog was found by a cat . 
the dog was found by the moose . 
the mouse hugged the dog . 
the bird loved a cat . 
a bird was chased by a fish . 
the cat cheated a dog . 
a bird cheated the cat . 
the bird was hugged by a cat . 
the cat hugged a dog . 
a cat was hugged by the bird . 
the fish chased the mouse . 
the kiss chased the dog . 
the fish loved the cat . 
a bird kissed the mouse . 
a jumbo dog kissed the zebra . 
```

Output:
```bash
an aardvark was cheated by the fish . 
a zebra was found by the dog . 
the fish was cheated by an aardvark .
a jumbo dog kissed the zebra .
```

### Notes

- The **accept state** is defined as any state where both `next1` and `next2` are `-1`.
- You can test without a working `REcompile` by hand-writing an FSM file.
- The NFA simulation uses a custom `Dequeue` structure and supports:
  - **epsilon transitions** (`BR`)
  - **literal character matches** (`a`, `b`, etc.)
