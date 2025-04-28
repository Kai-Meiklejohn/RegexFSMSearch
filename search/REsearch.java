package search;

// REsearch.java
// Riley Cooney
// 1632444

import java.io.*;
import java.util.*;

public class REsearch {
    public static void main(String[] args) {
        // checking correct amount of arguments are parsed
        if (args.length != 1) {
            System.err.println("Usage: java search.REsearch <filename>");
            System.exit(1);
        }
        //storing file name from argument
        String filename = args[0];

        //reading the fsm from standard input
        Scanner fsmInput = new Scanner(System.in);
        List<String> fsmLines = new ArrayList<>();

        //storing all lines from the fsm input
        while (fsmInput.hasNextLine()) {
            fsmLines.add(fsmInput.nextLine());
        }

        fsmInput.close();


        //opening the file to search
        try (BufferedReader fileReader = new BufferedReader(new FileReader(filename))) {
            //variable to store each line of the file
            String line;

            while ((line = fileReader.readLine()) != null) {
                //do real searching not this
                System.out.println(line);
            }
        } catch (IOException e) {
            // handle file errors
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }
    }
}
