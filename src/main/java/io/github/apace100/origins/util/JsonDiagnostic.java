package io.github.apace100.origins.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.github.apace100.origins.Origins;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;


/**
 * Comprehensive JSON diagnostic and validation utility
 * Scans, validates, and repairs JSON files to prevent EOFException errors
 */
public class JsonDiagnostic {
    
    private static final Gson GSON = new Gson();
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
        "run", "build", ".gradle", ".git", ".idea", "gradle", 
        "logs", "crash-reports", "screenshots", "saves", "resourcepacks",
        "shaderpacks", "mods", "config", "json_backups"
    );
    
    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;
        public final ValidationError errorType;
        public final List<String> suggestions;
        public final Path filePath;
        
        public ValidationResult(Path filePath, boolean isValid, String errorMessage, 
                              ValidationError errorType, List<String> suggestions) {
            this.filePath = filePath;
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.errorType = errorType;
            this.suggestions = suggestions != null ? suggestions : new ArrayList<>();
        }
        
        public static ValidationResult valid(Path filePath) {
            return new ValidationResult(filePath, true, null, null, null);
        }
        
        public static ValidationResult invalid(Path filePath, String error, ValidationError type, List<String> suggestions) {
            return new ValidationResult(filePath, false, error, type, suggestions);
        }
    }
    
    public enum ValidationError {
        EMPTY_FILE,
        SYNTAX_ERROR,
        EOF_ERROR,
        ENCODING_ERROR,
        PERMISSION_ERROR,
        CORRUPTED_STRUCTURE
    }
    
    /**
     * Scans for all JSON files in the project, excluding build and run directories
     */
    public static List<Path> scanForJsonFiles() {
        List<Path> jsonFiles = new ArrayList<>();
        Path rootPath = FabricLoader.getInstance().getGameDir().getParent();
        
        // Проверяем, что rootPath существует
        if (rootPath == null || !Files.exists(rootPath)) {
            Origins.LOGGER.warn("Root path not found or doesn't exist: " + rootPath);
            return jsonFiles;
        }
        
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String dirName = dir.getFileName().toString();
                    if (EXCLUDED_DIRECTORIES.contains(dirName)) {
                        Origins.LOGGER.info("Skipping directory: " + dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().toLowerCase().endsWith(".json")) {
                        jsonFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Origins.LOGGER.error("Error scanning for JSON files: " + e.getMessage());
        }
        
        Origins.LOGGER.info("Found " + jsonFiles.size() + " JSON files to validate");
        return jsonFiles;
    }
    
    /**
     * Validates a single JSON file for syntax and structure
     */
    public static ValidationResult validateJsonFile(Path filePath) {
        if (!Files.exists(filePath)) {
            return ValidationResult.invalid(filePath, "File does not exist", 
                ValidationError.PERMISSION_ERROR, List.of("Check file path", "Verify file permissions"));
        }
        
        try {
            // Check if file is empty
            if (Files.size(filePath) == 0) {
                return ValidationResult.invalid(filePath, "JSON file is empty", 
                    ValidationError.EMPTY_FILE, List.of("Delete empty file or add default content"));
            }
            
            // Read file content
            String content = Files.readString(filePath);
            
            // Check for common EOF issues
            if (content.trim().isEmpty()) {
                return ValidationResult.invalid(filePath, "JSON file contains only whitespace", 
                    ValidationError.EMPTY_FILE, List.of("Add valid JSON content or delete file"));
            }
            
            // Validate JSON syntax
            try {
                GSON.fromJson(content, Object.class);
                return ValidationResult.valid(filePath);
            } catch (JsonSyntaxException e) {
                List<String> suggestions = generateSuggestions(content, e);
                return ValidationResult.invalid(filePath, "JSON syntax error: " + e.getMessage(), 
                    ValidationError.SYNTAX_ERROR, suggestions);
            }
            
        } catch (IOException e) {
            if (e instanceof EOFException) {
                return ValidationResult.invalid(filePath, "Unexpected end of file", 
                    ValidationError.EOF_ERROR, List.of("File may be truncated", "Restore from backup"));
            }
            return ValidationResult.invalid(filePath, "IO error: " + e.getMessage(), 
                ValidationError.PERMISSION_ERROR, List.of("Check file permissions", "Verify disk space"));
        }
    }
    
    /**
     * Generates helpful suggestions based on JSON syntax errors
     */
    private static List<String> generateSuggestions(String content, JsonSyntaxException e) {
        List<String> suggestions = new ArrayList<>();
        String errorMsg = e.getMessage().toLowerCase();
        
        if (errorMsg.contains("expected") && errorMsg.contains("but was")) {
            suggestions.add("Check for missing or extra commas, brackets, or quotes");
        }
        if (errorMsg.contains("unterminated")) {
            suggestions.add("Check for unclosed strings or missing closing quotes");
        }
        if (errorMsg.contains("malformed")) {
            suggestions.add("Verify JSON structure follows proper format");
        }
        if (content.contains("\\")) {
            suggestions.add("Check for proper escaping of backslashes in strings");
        }
        if (!content.trim().startsWith("{") && !content.trim().startsWith("[")) {
            suggestions.add("JSON should start with { or [");
        }
        
        suggestions.add("Use online JSON validator to identify specific issues");
        suggestions.add("Compare with working JSON files of same type");
        
        return suggestions;
    }
    
    /**
     * Attempts to repair common JSON issues
     */
    public static boolean repairJsonFile(Path filePath, ValidationResult result) {
        if (result.isValid) {
            return true;
        }
        
        Origins.LOGGER.info("Attempting repair of JSON file: {}", filePath);
        
        try {
            // Create backup before attempting repair
            Path backupPath = createBackup(filePath);
            Origins.LOGGER.info("Created backup: " + backupPath);
            
            switch (result.errorType) {
                case EMPTY_FILE:
                    return handleEmptyFile(filePath);
                case EOF_ERROR:
                    return handleEofError(filePath);
                case SYNTAX_ERROR:
                    return handleSyntaxError(filePath);
                default:
                    Origins.LOGGER.warn("Cannot auto-repair error type: " + result.errorType);
                    return false;
            }
        } catch (IOException e) {
            Origins.LOGGER.error("Failed to repair JSON file " + filePath + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a backup of the file before attempting repairs
     */
    private static Path createBackup(Path filePath) throws IOException {
        Path backupDir = filePath.getParent().resolve("json_backups");
        Files.createDirectories(backupDir);
        
        String timestamp = String.valueOf(System.currentTimeMillis());
        String backupName = filePath.getFileName().toString() + ".backup." + timestamp;
        Path backupPath = backupDir.resolve(backupName);
        
        Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        return backupPath;
    }
    
    /**
     * Handles empty JSON files by generating appropriate defaults
     */
    private static boolean handleEmptyFile(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();
        String pathStr = filePath.toString().toLowerCase();
        String defaultContent = "{}";
        
        // Generate appropriate default content based on file type and location
        if (fileName.contains("fabric.mod.json")) {
            defaultContent = generateDefaultFabricModJson();
        } else if (fileName.contains("mixins.json")) {
            defaultContent = generateDefaultMixinsJson();
        } else if (pathStr.contains("lang") || pathStr.contains("language")) {
            defaultContent = "{}";
        } else if (pathStr.contains("model")) {
            defaultContent = generateDefaultModelJson();
        } else if (pathStr.contains("origins") && fileName.endsWith(".json")) {
            // Origins-specific JSON files
            if (pathStr.contains("powers")) {
                defaultContent = generateDefaultPowerJson();
            } else if (pathStr.contains("origins")) {
                defaultContent = generateDefaultOriginJson();
            }
        }
        
        Files.writeString(filePath, defaultContent);
        Origins.LOGGER.info("Generated default content for empty file: " + filePath);
        return true;
    }
    
    /**
     * Handles EOF errors by attempting to complete truncated JSON
     */
    private static boolean handleEofError(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        String repairedContent = attemptJsonCompletion(content);
        
        if (!repairedContent.equals(content)) {
            Files.writeString(filePath, repairedContent);
            Origins.LOGGER.info("Attempted to complete truncated JSON: " + filePath);
            return true;
        }
        
        return false;
    }
    
    /**
     * Handles basic syntax errors by attempting common fixes
     */
    private static boolean handleSyntaxError(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        String repairedContent = attemptSyntaxRepair(content);
        
        if (!repairedContent.equals(content)) {
            Files.writeString(filePath, repairedContent);
            Origins.LOGGER.info("Attempted syntax repair for: " + filePath);
            return true;
        }
        
        return false;
    }
    
    /**
     * Attempts to complete truncated JSON structures
     */
    private static String attemptJsonCompletion(String content) {
        content = content.trim();
        
        // Count brackets and braces
        int openBraces = 0, closeBraces = 0;
        int openBrackets = 0, closeBrackets = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (char c : content.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                switch (c) {
                    case '{': openBraces++; break;
                    case '}': closeBraces++; break;
                    case '[': openBrackets++; break;
                    case ']': closeBrackets++; break;
                }
            }
        }
        
        // Add missing closing characters
        StringBuilder result = new StringBuilder(content);
        
        // Close any unclosed strings
        if (inString) {
            result.append('"');
        }
        
        // Close brackets and braces
        for (int i = 0; i < (openBrackets - closeBrackets); i++) {
            result.append(']');
        }
        for (int i = 0; i < (openBraces - closeBraces); i++) {
            result.append('}');
        }
        
        return result.toString();
    }
    
    /**
     * Attempts basic syntax repairs
     */
    private static String attemptSyntaxRepair(String content) {
        // Remove trailing commas
        content = content.replaceAll(",\\s*([}\\]])", "$1");
        
        // Fix common quote issues
        content = content.replaceAll("'([^']*)':", "\"$1\":");
        
        // Fix missing quotes around keys
        content = content.replaceAll("([{,]\\s*)([a-zA-Z_][a-zA-Z0-9_]*)\\s*:", "$1\"$2\":");
        
        return content;
    }
    
    // Default JSON templates
    private static String generateDefaultFabricModJson() {
        return "{\n" +
               "  \"schemaVersion\": 1,\n" +
               "  \"id\": \"origins\",\n" +
               "  \"version\": \"${version}\",\n" +
               "  \"name\": \"Origins\",\n" +
               "  \"description\": \"Choose your origin! Become a creature with unique abilities, benefits and drawbacks.\",\n" +
               "  \"authors\": [\"apace100\"],\n" +
               "  \"contact\": {\n" +
               "    \"homepage\": \"https://github.com/apace100/Origins\",\n" +
               "    \"sources\": \"https://github.com/apace100/Origins\",\n" +
               "    \"issues\": \"https://github.com/apace100/Origins/issues\"\n" +
               "  },\n" +
               "  \"license\": \"MIT\",\n" +
               "  \"icon\": \"assets/origins/icon.png\",\n" +
               "  \"environment\": \"*\",\n" +
               "  \"entrypoints\": {\n" +
               "    \"main\": [\"io.github.apace100.origins.Origins\"],\n" +
               "    \"client\": [\"io.github.apace100.origins.OriginsClient\"]\n" +
               "  },\n" +
               "  \"mixins\": [\"origins.mixins.json\"],\n" +
               "  \"depends\": {\n" +
               "    \"fabricloader\": \">=0.14.0\",\n" +
               "    \"minecraft\": \"~1.20.1\",\n" +
               "    \"java\": \">=17\",\n" +
               "    \"fabric-api\": \"*\"\n" +
               "  },\n" +
               "  \"suggests\": {\n" +
               "    \"modmenu\": \"*\"\n" +
               "  }\n" +
               "}";
    }
    
    private static String generateDefaultMixinsJson() {
        return "{\n" +
               "  \"required\": true,\n" +
               "  \"package\": \"io.github.apace100.origins.mixin\",\n" +
               "  \"compatibilityLevel\": \"JAVA_17\",\n" +
               "  \"refmap\": \"origins.refmap.json\",\n" +
               "  \"mixins\": [],\n" +
               "  \"client\": [],\n" +
               "  \"server\": [],\n" +
               "  \"minVersion\": \"0.8\"\n" +
               "}";
    }
    
    private static String generateDefaultModelJson() {
        return "{\n" +
               "  \"parent\": \"item/generated\",\n" +
               "  \"textures\": {\n" +
               "    \"layer0\": \"origins:item/default\"\n" +
               "  }\n" +
               "}";
    }
    
    /**
     * Generates default power JSON structure
     */
    private static String generateDefaultPowerJson() {
        return "{\n" +
               "  \"type\": \"origins:empty\",\n" +
               "  \"name\": \"Default Power\",\n" +
               "  \"description\": \"A default power with no effects\"\n" +
               "}";
    }
    
    /**
     * Generates default origin JSON structure
     */
    private static String generateDefaultOriginJson() {
        return "{\n" +
               "  \"powers\": [],\n" +
               "  \"icon\": {\n" +
               "    \"item\": \"minecraft:player_head\"\n" +
               "  },\n" +
               "  \"order\": 0,\n" +
               "  \"impact\": 0,\n" +
               "  \"name\": \"Default Origin\",\n" +
               "  \"description\": \"A default origin with no special abilities\"\n" +
               "}";
    }
    
    /**
     * Runs comprehensive JSON validation on the entire project
     */
    public static void runFullDiagnostic() {
        Origins.LOGGER.info("Starting comprehensive JSON diagnostic...");
        
        List<Path> jsonFiles = scanForJsonFiles();
        List<ValidationResult> failures = new ArrayList<>();
        int repairedCount = 0;
        int validCount = 0;
        
        for (Path jsonFile : jsonFiles) {
            ValidationResult result = validateJsonFile(jsonFile);
            
            if (!result.isValid) {
                Origins.LOGGER.warn("JSON validation failed for: " + jsonFile);
                Origins.LOGGER.warn("Error: " + result.errorMessage);
                Origins.LOGGER.warn("Suggestions: " + String.join(", ", result.suggestions));
                
                failures.add(result);
                
                // Attempt repair
                if (repairJsonFile(jsonFile, result)) {
                    repairedCount++;
                    Origins.LOGGER.info("Successfully repaired: " + jsonFile);
                    
                    // Re-validate after repair
                    ValidationResult revalidation = validateJsonFile(jsonFile);
                    if (revalidation.isValid) {
                        validCount++;
                        Origins.LOGGER.info("Repair verified for: " + jsonFile);
                    } else {
                        Origins.LOGGER.error("Repair failed verification for: " + jsonFile);
                    }
                } else {
                    Origins.LOGGER.error("Failed to repair: " + jsonFile);
                }
            } else {
                validCount++;
            }
        }
        
        Origins.LOGGER.info("=== JSON Diagnostic Summary ===");
        Origins.LOGGER.info("- Total files scanned: " + jsonFiles.size());
        Origins.LOGGER.info("- Valid files: " + validCount);
        Origins.LOGGER.info("- Files with issues: " + failures.size());
        Origins.LOGGER.info("- Files repaired: " + repairedCount);
        Origins.LOGGER.info("- Success rate: " + String.format("%.1f%%", 
            (double) validCount / jsonFiles.size() * 100));
        
        if (!failures.isEmpty()) {
            Origins.LOGGER.warn("Files requiring manual attention:");
            for (ValidationResult failure : failures) {
                Origins.LOGGER.warn("- " + failure.filePath + ": " + failure.errorMessage);
            }
        }
        
        Origins.LOGGER.info("JSON diagnostic complete!");
    }
    
    /**
     * Quick validation for Origins-specific JSON files only
     */
    public static void runOriginsJsonDiagnostic() {
        Origins.LOGGER.info("Starting Origins-specific JSON diagnostic...");
        
        List<Path> jsonFiles = scanForJsonFiles().stream()
            .filter(path -> path.toString().contains("origins") || 
                           path.toString().contains("powers") ||
                           path.toString().contains("fabric.mod.json") ||
                           path.toString().contains("mixins.json"))
            .toList();
        
        Origins.LOGGER.info("Found " + jsonFiles.size() + " Origins-related JSON files");
        
        int issues = 0;
        for (Path jsonFile : jsonFiles) {
            ValidationResult result = validateJsonFile(jsonFile);
            if (!result.isValid) {
                issues++;
                Origins.LOGGER.warn("Issue in " + jsonFile + ": " + result.errorMessage);
                repairJsonFile(jsonFile, result);
            }
        }
        
        if (issues == 0) {
            Origins.LOGGER.info("All Origins JSON files are valid!");
        } else {
            Origins.LOGGER.warn("Found and attempted to fix " + issues + " issues in Origins JSON files");
        }
    }
}