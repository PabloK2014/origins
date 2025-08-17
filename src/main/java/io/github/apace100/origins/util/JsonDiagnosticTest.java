package io.github.apace100.origins.util;

import io.github.apace100.origins.Origins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test utility for JSON diagnostic and repair functionality
 * Creates test cases and validates the repair system
 */
public class JsonDiagnosticTest {
    
    /**
     * Runs comprehensive tests of JSON validation and repair
     */
    public static void runTests() {
                
        try {
            // Create test directory
            Path testDir = Paths.get("test_json_files");
            Files.createDirectories(testDir);
            
            // Test 1: Empty file
            testEmptyFile(testDir);
            
            // Test 2: Syntax errors
            testSyntaxErrors(testDir);
            
            // Test 3: Truncated JSON
            testTruncatedJson(testDir);
            
            // Test 4: Encoding issues
            testEncodingIssues(testDir);
            
            // Test 5: Valid JSON (should pass)
            testValidJson(testDir);
            
            // Test 6: Batch repair
            testBatchRepair(testDir);
            
                        
        } catch (Exception e) {
            Origins.LOGGER.error("JSON diagnostic tests failed: " + e.getMessage(), e);
        }
    }
    
    private static void testEmptyFile(Path testDir) throws IOException {
                
        Path emptyFile = testDir.resolve("empty.json");
        Files.writeString(emptyFile, "");
        
        JsonDiagnostic.ValidationResult result = JsonDiagnostic.validateJsonFile(emptyFile);
        
        if (!result.isValid && result.errorType == JsonDiagnostic.ValidationError.EMPTY_FILE) {
                        
            // Test repair
            boolean repaired = JsonDiagnostic.repairJsonFile(emptyFile, result);
            if (repaired) {
                                
                // Validate repair
                JsonDiagnostic.ValidationResult repairedResult = JsonDiagnostic.validateJsonFile(emptyFile);
                if (repairedResult.isValid) {
                                    } else {
                    Origins.LOGGER.error("✗ Repaired file is still invalid");
                }
            } else {
                Origins.LOGGER.error("✗ Failed to repair empty file");
            }
        } else {
            Origins.LOGGER.error("✗ Empty file not correctly detected");
        }
    }
    
    private static void testSyntaxErrors(Path testDir) throws IOException {
                
        // Test trailing comma
        Path trailingCommaFile = testDir.resolve("trailing_comma.json");
        Files.writeString(trailingCommaFile, "{\n  \"key1\": \"value1\",\n  \"key2\": \"value2\",\n}");
        
        JsonDiagnostic.ValidationResult result = JsonDiagnostic.validateJsonFile(trailingCommaFile);
        
        if (!result.isValid && result.errorType == JsonDiagnostic.ValidationError.SYNTAX_ERROR) {
                        
            boolean repaired = JsonDiagnostic.repairJsonFile(trailingCommaFile, result);
            if (repaired) {
                                
                JsonDiagnostic.ValidationResult repairedResult = JsonDiagnostic.validateJsonFile(trailingCommaFile);
                if (repairedResult.isValid) {
                                    } else {
                    Origins.LOGGER.error("✗ Repaired syntax is still invalid: " + repairedResult.errorMessage);
                }
            } else {
                Origins.LOGGER.error("✗ Failed to repair syntax error");
            }
        } else {
            Origins.LOGGER.error("✗ Syntax error not correctly detected");
        }
        
        // Test unquoted keys
        Path unquotedKeysFile = testDir.resolve("unquoted_keys.json");
        Files.writeString(unquotedKeysFile, "{\n  key1: \"value1\",\n  key2: \"value2\"\n}");
        
        result = JsonDiagnostic.validateJsonFile(unquotedKeysFile);
        if (!result.isValid) {
                        
            boolean repaired = JsonDiagnostic.repairJsonFile(unquotedKeysFile, result);
            if (repaired) {
                            }
        }
    }
    
    private static void testTruncatedJson(Path testDir) throws IOException {
                
        Path truncatedFile = testDir.resolve("truncated.json");
        Files.writeString(truncatedFile, "{\n  \"key1\": \"value1\",\n  \"key2\": {\n    \"nested\": \"value\"");
        
        JsonDiagnostic.ValidationResult result = JsonDiagnostic.validateJsonFile(truncatedFile);
        
        if (!result.isValid) {
                        
            boolean repaired = JsonDiagnostic.repairJsonFile(truncatedFile, result);
            if (repaired) {
                                
                JsonDiagnostic.ValidationResult repairedResult = JsonDiagnostic.validateJsonFile(truncatedFile);
                if (repairedResult.isValid) {
                                    } else {
                    Origins.LOGGER.warn("⚠ Repaired truncated JSON still has issues: " + repairedResult.errorMessage);
                }
            } else {
                Origins.LOGGER.error("✗ Failed to repair truncated JSON");
            }
        } else {
            Origins.LOGGER.error("✗ Truncated JSON not correctly detected");
        }
    }
    
    private static void testEncodingIssues(Path testDir) throws IOException {
                
        Path encodingFile = testDir.resolve("encoding_issues.json");
        // Simulate encoding issues with special characters
        Files.writeString(encodingFile, "{\n  \"key1\": \"valueâ€™with issues\",\n  \"key2\": \"normalvalue\"\n}");
        
        JsonDiagnostic.ValidationResult result = JsonDiagnostic.validateJsonFile(encodingFile);
        
        if (!result.isValid) {
                        
            boolean repaired = JsonDiagnostic.repairJsonFile(encodingFile, result);
            if (repaired) {
                            }
        } else {
                    }
    }
    
    private static void testValidJson(Path testDir) throws IOException {
                
        Path validFile = testDir.resolve("valid.json");
        Files.writeString(validFile, "{\n  \"key1\": \"value1\",\n  \"key2\": {\n    \"nested\": \"value\"\n  }\n}");
        
        JsonDiagnostic.ValidationResult result = JsonDiagnostic.validateJsonFile(validFile);
        
        if (result.isValid) {
                    } else {
            Origins.LOGGER.error("✗ Valid JSON incorrectly marked as invalid: " + result.errorMessage);
        }
    }
    
    private static void testBatchRepair(Path testDir) throws IOException {
                
        // Create multiple test files
        Path batch1 = testDir.resolve("batch1.json");
        Path batch2 = testDir.resolve("batch2.json");
        Path batch3 = testDir.resolve("batch3.json");
        
        Files.writeString(batch1, ""); // Empty
        Files.writeString(batch2, "{\n  \"key\": \"value\",\n}"); // Trailing comma
        Files.writeString(batch3, "{\n  \"key\": \"value\"\n}"); // Valid
        
        List<Path> batchFiles = List.of(batch1, batch2, batch3);
        
        // Test batch repair using JsonRepairService
        var results = JsonRepairService.batchRepair(batchFiles);
        
        int successCount = 0;
        for (var result : results.values()) {
            if (result.success) {
                successCount++;
            }
        }
        
                
        if (successCount >= 2) { // At least 2 should be repairable
                    } else {
            Origins.LOGGER.error("✗ Batch repair functionality failed");
        }
    }
    
    /**
     * Creates test files with various JSON issues for manual testing
     */
    public static void createTestFiles() {
                
        try {
            Path testDir = Paths.get("json_test_cases");
            Files.createDirectories(testDir);
            
            // Test case 1: Empty file
            Files.writeString(testDir.resolve("test_empty.json"), "");
            
            // Test case 2: Trailing commas
            Files.writeString(testDir.resolve("test_trailing_comma.json"), 
                "{\n  \"schemaVersion\": 1,\n  \"id\": \"origins\",\n  \"version\": \"1.0.0\",\n}");
            
            // Test case 3: Unquoted keys
            Files.writeString(testDir.resolve("test_unquoted_keys.json"), 
                "{\n  schemaVersion: 1,\n  id: \"origins\",\n  version: \"1.0.0\"\n}");
            
            // Test case 4: Truncated JSON
            Files.writeString(testDir.resolve("test_truncated.json"), 
                "{\n  \"schemaVersion\": 1,\n  \"entrypoints\": {\n    \"main\": [");
            
            // Test case 5: Mixed quotes
            Files.writeString(testDir.resolve("test_mixed_quotes.json"), 
                "{\n  'schemaVersion': 1,\n  \"id\": 'origins',\n  \"version\": \"1.0.0\"\n}");
            
            // Test case 6: Comments (invalid in JSON)
            Files.writeString(testDir.resolve("test_comments.json"), 
                "{\n  // This is a comment\n  \"schemaVersion\": 1,\n  \"id\": \"origins\" /* inline comment */\n}");
            
                                    
        } catch (IOException e) {
            Origins.LOGGER.error("Failed to create test files: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cleans up test files
     */
    public static void cleanupTestFiles() {
        try {
            Path testDir = Paths.get("test_json_files");
            if (Files.exists(testDir)) {
                Files.walk(testDir)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            Origins.LOGGER.warn("Failed to delete test file: " + path);
                        }
                    });
                            }
        } catch (IOException e) {
            Origins.LOGGER.error("Failed to cleanup test files: " + e.getMessage(), e);
        }
    }
}