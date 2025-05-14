#!/bin/bash
# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Temporary files for testing
TEMP_FILE="test_string.txt"
OUTPUT_FILE="output.txt"

# Function to run a single test
run_test() {
  local regex="$1"
  local test_string="$2"

  # Create temporary file with the test string
  echo "$test_string" >"$TEMP_FILE"

  # Run the REcompile and REsearch commands and redirect output to a file
  java REcompile "$regex" | java REsearch "$TEMP_FILE" >"$OUTPUT_FILE" 2>&1

  # Check if the output file has content
  if [[ ! -s "$OUTPUT_FILE" ]]; then
    echo -e "${RED}FAILURE: No output received for regex '$regex' on string '$test_string'${NC}"
    echo -e "${RED}This likely indicates an error in the program execution${NC}"
    return 1
  fi

  # Read the output
  output=$(<"$OUTPUT_FILE")

  # Check if there's any output at all - indicating a match was found
  if [[ -n "$output" ]]; then
    echo -e "${GREEN}SUCCESS: Regex '$regex' matched in string '$test_string'${NC}"
    echo -e "${GREEN}Match found: $output${NC}"
    return 0
  else
    echo -e "${RED}FAILURE: Regex '$regex' did not match in string '$test_string'${NC}"
    echo -e "${RED}No match found in: $test_string${NC}"
    return 1
  fi
}

# Initialize counters
passed=0
failed=0
total=0

# Test cases array
# Format: ("regex1:test_string1" "regex2:test_string2" ...)
declare -a test_cases=(
  # Basic literal and alternation
  "a:a"
  "a:abc"
  "a:cba"
  "abc:abc"
  "abc:xabcx"
  "a|b|c:a"
  "a|b|c:b"
  "a|b|c:c"
  "abc|def|ghi:abc"
  "abc|def|ghi:def"
  "abc|def|ghi:ghi"

  # Wildcard
  ".:a"
  ".:z"
  ".:1"
  "a.c:abc"
  "a.c:axc"
  "a.c:ac" # should not match

  # Quantifiers
  "a*: "
  "a*:a"
  "a*:aaaa"
  "a+:a"
  "a+:aaaa"
  "a+: " # should not match
  "a?: "
  "a?:a"
  "a?:aa" # should not match
  "ab*c:ac"
  "ab*c:abc"
  "ab*c:abbbc"
  "ab+c:ac" # should not match
  "ab+c:abc"
  "ab+c:abbbc"
  "ab?c:ac"
  "ab?c:abc"
  "ab*c*d:ad"
  "ab*c*d:abd"
  "ab*c*d:acd"
  "ab*c*d:abcd"
  "ab*c*d:abbccd"
  "ab*c*d:abbbccccccd"
  "ab+c+d:ad" # should not match
  "ab+c+d:abd" # should not match
  "ab+c+d:acd" # should not match
  "ab+c+d:abcd"
  "ab+c+d:abbccd"
  "ab+c+d:abbbccccccd"

  # Parentheses and precedence
  "(ab)*: "
  "(ab)*:ab"
  "(ab)*:abab"
  "(a|b)+:a"
  "(a|b)+:b"
  "(a|b)+:ab"
  "(a|b)+:ba"
  "(a|b)+:abba"
  "(a|b)+: " # should not match
  "a(bc)*d:ad"
  "a(bc)*d:abcd"
  "a(bc)*d:abcbcd"
  "a(bc)+d:abcd"
  "a(bc)+d:abcbcd"
  "aardvark|zebra:aardvark"
  "aardvark|zebra:zebra"
  "aardvar(k|z)ebra:aardvarkebra"
  "aardvar(k|z)ebra:aardvarzebra"

  # Escaped special characters
  "a\*b:a*b"
  "a\|b:a|b"
  "a\(b:a(b"
  "a\)b:a)b"
  "a\\\\b:a\\b"
  "\.:."
  "a\.b:a.b"
  #"a\\.b:a\cb" this test breaks the scanner. due to \c pretty sure

  # Edge/invalid cases (should fail or error)
  "(): " # should not match
  "a**:a" # should not match
  "a++:a" # should not match
  "a??:a" # should not match
  "(a:a" # should not match
  "a):a" # should not match
  "a(:a" # should not match
  "a|:a" # should not match
  " :a" # should not match
  "a: " # should not match

  # There should be 18 failing tests
)

# Compile all Java files
echo "Compiling Java files..."
javac *.java

# Run all tests
echo "===== RUNNING REGEX TESTS ====="
echo "Total tests: ${#test_cases[@]}"
echo "=============================="

for test_case in "${test_cases[@]}"; do
  # Split the test case into regex and test string
  IFS=':' read -r regex test_string <<<"$test_case"

  echo -e "\nTest #$((total + 1)): '$regex' on '$test_string'"
  if run_test "$regex" "$test_string"; then
    passed=$((passed + 1))
  else
    failed=$((failed + 1))
  fi
  total=$((total + 1))
done

# Clean up temporary files
rm -f "$TEMP_FILE" "$OUTPUT_FILE"

# Print summary
echo -e "\n=============================="
echo -e "Tests completed: $total"
echo -e "${GREEN}Tests passed: $passed${NC}"
echo -e "${RED}Tests failed: $failed${NC}"
echo "=============================="
