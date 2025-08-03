# Implementation Plan

- [x] 1. Analyze quest folder dependencies and create file usage map


  - Search for all imports and references to quest package classes throughout the codebase
  - Create a comprehensive list of which files are actually used vs potentially unused
  - Document the dependency chain from Origins.java to all quest components
  - _Requirements: 1.1, 1.2_

- [x] 2. Fix duplicate chat messages in quest ticket handling


  - Locate the source of duplicate chat messages when clicking quest tickets
  - Identify all places where chat messages are sent during quest ticket interactions
  - Consolidate message sending to a single point to eliminate duplicates
  - Test the fix by compiling with `compileJava`
  - _Requirements: 3.1, 3.3, 4.1, 4.2_

- [x] 3. Fix class restriction message logic and formatting


  - Locate the class compatibility check logic in quest ticket handling
  - Fix the logic that incorrectly shows "wrong class" messages for correct classes
  - Update message format to include class name: "билет для другого класса: (название класса)"
  - Ensure only one message is shown per click interaction
  - Test the fix by compiling with `compileJava`
  - _Requirements: 3.2, 3.4, 4.1, 4.2_

- [x] 4. Identify and remove unused quest files


  - Based on dependency analysis, create list of files with no incoming references
  - Remove unused files one by one, compiling after each removal
  - Update any remaining imports that reference removed files
  - Verify no runtime errors occur from missing dependencies
  - _Requirements: 1.1, 1.3, 4.1, 4.2, 4.3_

- [x] 5. Optimize code structure in remaining quest files


  - Identify large methods that can be broken down into smaller methods
  - Extract common functionality between similar board implementations
  - Remove duplicate code patterns across different quest components
  - Compile after each optimization to ensure no errors
  - _Requirements: 2.1, 2.2, 2.3, 4.1, 4.2_

- [x] 6. Clean up imports and unused methods


  - Remove unused import statements from all quest files
  - Remove unused private methods and fields within quest classes
  - Consolidate similar utility methods into shared helper classes
  - Compile after cleanup to verify all references are resolved
  - _Requirements: 2.1, 2.3, 4.1, 4.2_

- [x] 7. Final compilation and functionality verification



  - Run complete compilation with `compileJava` to ensure no errors
  - Test quest ticket clicking behavior to verify single messages
  - Test class restriction logic with different player classes
  - Verify all bounty board functionality still works correctly
  - _Requirements: 4.3, 4.4_