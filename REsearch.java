

import java.io.*;
import java.util.*;

/**
 * REsearch.java
 * Author: Riley Cooney (1632444)
 * 
 * handles the search of a file using a finite state machine (FSM) built from a regular expression
 */
public class REsearch {
    // variables to hold FSM data
    private static char[] ch;
    private static int[] next1;
    private static int[] next2;
    private static boolean[] isWildcard;  // Added to distinguish wildcards from literal periods
    private static final int SCAN = -2;

    public static void main(String[] args) {
        // Check for correct number of arguments
        if (args.length != 1) {
            // Print usage message and exit
            System.err.println("Usage: java search.REsearch <filename>");
            System.exit(1);
        }
        // store file name from argument
        String filename = args[0];

        // Read FSM from standard input
        Scanner fsmInput = new Scanner(System.in);
        List<String> fsmLines = new ArrayList<>();
        // Read lines from standard input until file has ended
        while (fsmInput.hasNextLine()) {
            String line = fsmInput.nextLine();
            // store each line in the list
            fsmLines.add(line);
        }
        fsmInput.close();

        // parsing fsm to find max state number
        int maxState = -1;
        for (String line : fsmLines) {
            // split the line by commas and parse the state number
            String[] parts = line.split(",");
            int stateNum = Integer.parseInt(parts[0]);
            if (stateNum > maxState) {
                // update maxState to largest state number found
                maxState = stateNum;
            }
        }
        // initialise arrays with size maxState + 1
        ch    = new char[maxState + 1];
        next1 = new int [maxState + 1];
        next2 = new int [maxState + 1];
        isWildcard = new boolean[maxState + 1];  // Initialize isWildcard array

        for (String line : fsmLines) {
            String[] parts = line.split(",");
            int stateNum = Integer.parseInt(parts[0]);
            if (parts[1].equals("BR")) {
                ch[stateNum] = '\0';
                isWildcard[stateNum] = false;
            } else if (parts[1].equals("WC")) {
                ch[stateNum] = '.';  // Optional, kept for consistency
                isWildcard[stateNum] = true;
            } else if (!parts[1].isEmpty()) {
                // handle literal character, including escaped characters like '\'
                ch[stateNum] = parts[1].charAt(0);
                isWildcard[stateNum] = false;
            } else {
                // Accepting state, do nothing for ch and isWildcard
            }
            
            // setting first and second next states
            next1[stateNum] = Integer.parseInt(parts[2]);
            next2[stateNum] = Integer.parseInt(parts[3]);
        }

        // search through the file for matches using fsm
        try (BufferedReader fileReader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                if (searchLine(line)) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            // handle file reading errors
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
    }

    // method to search through each line of the file using the fsm
    private static boolean searchLine(String line) {
        // variables to hold current and next states
        Dequeue current = new Dequeue();
        Dequeue next = new Dequeue();
        current.addBack(0);

        // loop through each character in the line
        for (int i = 0; i < line.length(); i++) {
            // get the current character
            char c = line.charAt(i);
            // mark state as visited
            Set<Integer> visited = new HashSet<>();

            while (!current.isEmpty()) {
                // get the next state from the current queue
                int state = current.removeFront();
                // check if state is already visited
                if (visited.contains(state)) continue;
                // mark state as visited
                visited.add(state);

                if (next1[state] == -1 && next2[state] == -1) {
                    // return true if accepting state is reached
                    return true;
                }

                // handle branching states
                if (ch[state] == '\0') {
                    // add first branch
                    if (next1[state] != -1) current.addBack(next1[state]);
                    // add second branch
                    if (next2[state] != -1) current.addBack(next2[state]);
                // handle character match
                } else if (isWildcard[state] || ch[state] == c) {  // Updated condition
                    // add next state
                    if (next1[state] != -1) next.addBack(next1[state]);
                }
            }

            // move to next character by switching queues and allowing new matches
            current = next;
            // move next states to current
            next = new Dequeue();
            // add start state for new match possibility
            current.addBack(0);
        }
        // set to track visited states
        Set<Integer> visited = new HashSet<>();
        Dequeue closure = new Dequeue();
        // move all states from current to closure
        while (!current.isEmpty()) {
            closure.addBack(current.removeFront());
        }
        // explore all reachable states from the closure
        while (!closure.isEmpty()) {
            int s = closure.removeFront();
            // if already visited, skip
            if (visited.contains(s)) continue;
            // mark state as visited
            visited.add(s);
            //handle branching states
            if (ch[s] == '\0') {
                // add first branch
                if (next1[s] != -1) closure.addBack(next1[s]);
                // add second branch
                if (next2[s] != -1) closure.addBack(next2[s]);
            }
            current.addBack(s);
        }

        // check for accepting states in final queue
        while (!current.isEmpty()) {
            int state = current.removeFront();
            if (next1[state] == -1 && next2[state] == -1) {
                // return true if accepting state is reached
                return true;
            }
        }
        // if no accepting state is reached, return false
        return false;
    }

    // dequeue class to manage the states during the search
    public static class Dequeue {
        // node class for a double linked list
        private static class Node {
            int state;
            Node prev, next;
            Node(int state) { this.state = state; }
        }
        // variables to hold the head and tail of the queue
        private Node head;
        private Node tail;
        private int size = 0;

        // method to add a state to the front of the queue
        public void addFront(int state) {
            Node node = new Node(state);
            if (head == null) {
                head = tail = node;
            } else {
                node.next = head;
                head.prev = node;
                head = node;
            }
            size++;
        }

        // method to add a state to the back of the queue
        public void addBack(int state) {
            Node node = new Node(state);
            if (tail == null) {
                head = tail = node;
            } else {
                tail.next = node;
                node.prev = tail;
                tail = node;
            }
            size++;
        }

        // method to remove a state from the front of the queue
        public int removeFront() {
            if (head == null) throw new NoSuchElementException();
            int val = head.state;
            head = head.next;
            if (head != null) {
                head.prev = null;
            } else {
                tail = null;
            }
            size--;
            return val;
        }

        // method to check if queue is empty
        public boolean isEmpty() {
            return size == 0;
        }
    }
}
