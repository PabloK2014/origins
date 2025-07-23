package io.github.apace100.origins.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.apace100.origins.Origins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Advanced JSON repair and regeneration service
 * Provides sophisticated repair mechanisms for corrupted JSON files
 */
public class JsonRepairService {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    /**
     * Repair result containing information about the repair operation
     */
    public static class RepairResult {
        public final boolean success;
        public final String originalContent;
        public final String repairedContent;
        public final List<String> appliedFixes;
        public final Path backupPath;
        
        public RepairResult(boolean success, String originalContent, String repairedContent, 
                          List<String> appliedFixes, Path backupPath) {
            this.success = success;
            this.originalContent = originalContent;
            this.repairedContent = repairedContent;
            this.appliedFixes = appliedFixes != null ? appliedFixes : new ArrayList<>();
            this.backupPath = backupPath;
        }
    }
    
    /**
     * Attempts comprehensive repair of a JSON file
     */
    public static RepairResult repairJsonFile(Path filePath) {
        Origins.LOGGER.info("Attempting comprehensive repair of JSON file: {}", filePath);
        
        try {
            // Read original content
            String originalContent = Files.readString(filePath);
            List<String> appliedFixes = new ArrayList<>();
            
            // Create backup
            Path backupPath = createTimestampedBackup(filePath);
            appliedFixes.add("Created backup: " + backupPath.getFileName());
            
            // Apply repair strategies
            String repairedContent = originalContent;
            
            // Strategy 1: Fix common syntax issues
            repairedContent = fixCommonSyntaxIssues(repairedContent, appliedFixes);
            
            // Strategy 2: Complete truncated JSON
            repairedContent = completeTruncatedJson(repairedContent, appliedFixes);
            
            // Strategy 3: Fix encoding issues
            repairedContent = fixEncodingIssues(repairedContent, appliedFixes);
            
            // Strategy 4: Validate and format
            repairedContent = validateAndFormat(repairedContent, appliedFixes);
            
            // Strategy 5: Generate default content if all else fails
            if (repairedContent == null || repairedContent.trim().isEmpty()) {
                repairedContent = generateDefaultContent(filePath, appliedFixes);
            }
            
            // Write repaired content
            Files.writeString(filePath, repairedContent);
            
            Origins.LOGGER.info("Successfully repaired JSON file: {}", filePath);
            Origins.LOGGER.info("Applied fixes: {}", String.join(", ", appliedFixes));
            
            return new RepairResult(true, originalContent, repairedContent, appliedFixes, backupPath);
            
        } catch (Exception e) {
            Origins.LOGGER.error("Failed to repair JSON file {}: {}", filePath, e.getMessage());
            return new RepairResult(false, null, null, null, null);
        }
    }
    
    /**
     * Creates a timestamped backup of the file
     */
    private static Path createTimestampedBackup(Path filePath) throws IOException {
        Path backupDir = filePath.getParent().resolve("json_backups");
        Files.createDirectories(backupDir);
        
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String backupName = filePath.getFileName().toString() + ".backup." + timestamp;
        Path backupPath = backupDir.resolve(backupName);
        
        Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        return backupPath;
    }
    
    /**
     * Fixes common JSON syntax issues
     */
    private static String fixCommonSyntaxIssues(String content, List<String> appliedFixes) {
        String original = content;
        
        // Remove trailing commas
        content = content.replaceAll(",\\s*([}\\]])", "$1");
        if (!content.equals(original)) {
            appliedFixes.add("Removed trailing commas");
            original = content;
        }
        
        // Fix single quotes to double quotes
        content = content.replaceAll("'([^']*)'\\s*:", "\"$1\":");
        if (!content.equals(original)) {
            appliedFixes.add("Fixed single quotes to double quotes");
            original = content;
        }
        
        // Fix unquoted keys
        content = content.replaceAll("([{,]\\s*)([a-zA-Z_][a-zA-Z0-9_]*)\\s*:", "$1\"$2\":");
        if (!content.equals(original)) {
            appliedFixes.add("Added quotes to unquoted keys");
            original = content;
        }
        
        // Fix common escape sequence issues
        content = content.replace("\\\\", "\\");
        if (!content.equals(original)) {
            appliedFixes.add("Fixed escape sequences");
            original = content;
        }
        
        // Remove comments (not valid in JSON)
        content = content.replaceAll("//.*$", "");
        content = content.replaceAll("/\\*.*?\\*/", "");
        if (!content.equals(original)) {
            appliedFixes.add("Removed comments");
        }
        
        return content;
    }
    
    /**
     * Completes truncated JSON structures
     */
    private static String completeTruncatedJson(String content, List<String> appliedFixes) {
        content = content.trim();
        
        if (content.isEmpty()) {
            return content;
        }
        
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
        
        StringBuilder result = new StringBuilder(content);
        boolean modified = false;
        
        // Close any unclosed strings
        if (inString) {
            result.append('"');
            appliedFixes.add("Closed unclosed string");
            modified = true;
        }
        
        // Close brackets and braces
        int missingCloseBrackets = openBrackets - closeBrackets;
        int missingCloseBraces = openBraces - closeBraces;
        
        for (int i = 0; i < missingCloseBrackets; i++) {
            result.append(']');
            modified = true;
        }
        for (int i = 0; i < missingCloseBraces; i++) {
            result.append('}');
            modified = true;
        }
        
        if (modified) {
            appliedFixes.add("Completed truncated JSON structure");
        }
        
        return result.toString();
    }
    
    /**
     * Fixes encoding issues
     */
    private static String fixEncodingIssues(String content, List<String> appliedFixes) {
        String original = content;
        
        // Remove BOM if present
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
            appliedFixes.add("Removed BOM");
        }
        
        // Fix common encoding issues
        content = content.replace("â€™", "'");
        content = content.replace("â€œ", "\"");
        content = content.replace("â€", "\"");
        
        if (!content.equals(original)) {
            appliedFixes.add("Fixed encoding issues");
        }
        
        return content;
    }
    
    /**
     * Validates and formats JSON
     */
    private static String validateAndFormat(String content, List<String> appliedFixes) {
        try {
            // Try to parse and reformat
            Object parsed = JsonParser.parseString(content);
            String formatted = GSON.toJson(parsed);
            
            if (!formatted.equals(content)) {
                appliedFixes.add("Reformatted JSON");
            }
            
            return formatted;
        } catch (Exception e) {
            Origins.LOGGER.warn("Could not validate/format JSON: {}", e.getMessage());
            return content;
        }
    }
    
    /**
     * Generates default content based on file type
     */
    private static String generateDefaultContent(Path filePath, List<String> appliedFixes) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        String defaultContent;
        
        if (fileName.contains("fabric.mod.json")) {
            defaultContent = generateDefaultFabricModJson();
        } else if (fileName.contains("mixins.json")) {
            defaultContent = generateDefaultMixinsJson();
        } else if (fileName.contains("lang")) {
            defaultContent = "{}";
        } else if (fileName.contains("model")) {
            defaultContent = generateDefaultModelJson();
        } else if (fileName.contains("recipe")) {
            defaultContent = generateDefaultRecipeJson();
        } else if (fileName.contains("advancement")) {
            defaultContent = generateDefaultAdvancementJson();
        } else if (fileName.contains("loot_table")) {
            defaultContent = generateDefaultLootTableJson();
        } else {
            defaultContent = "{}";
        }
        
        appliedFixes.add("Generated default content for " + fileName);
        return defaultContent;
    }
    
    // Default JSON templates
    private static String generateDefaultFabricModJson() {
        JsonObject mod = new JsonObject();
        mod.addProperty("schemaVersion", 1);
        mod.addProperty("id", "origins");
        mod.addProperty("version", "1.0.0");
        mod.addProperty("name", "Origins");
        mod.addProperty("environment", "*");
        
        JsonObject entrypoints = new JsonObject();
        mod.add("entrypoints", entrypoints);
        
        JsonObject depends = new JsonObject();
        depends.addProperty("fabricloader", ">=0.14.0");
        depends.addProperty("minecraft", "1.20.1");
        mod.add("depends", depends);
        
        return GSON.toJson(mod);
    }
    
    private static String generateDefaultMixinsJson() {
        JsonObject mixins = new JsonObject();
        mixins.addProperty("required", true);
        mixins.addProperty("package", "io.github.apace100.origins.mixin");
        mixins.addProperty("compatibilityLevel", "JAVA_17");
        mixins.addProperty("refmap", "origins.refmap.json");
        mixins.addProperty("minVersion", "0.8");
        
        return GSON.toJson(mixins);
    }
    
    private static String generateDefaultModelJson() {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "item/generated");
        
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "origins:item/default");
        model.add("textures", textures);
        
        return GSON.toJson(model);
    }
    
    private static String generateDefaultRecipeJson() {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        
        return GSON.toJson(recipe);
    }
    
    private static String generateDefaultAdvancementJson() {
        JsonObject advancement = new JsonObject();
        
        JsonObject display = new JsonObject();
        JsonObject icon = new JsonObject();
        icon.addProperty("item", "minecraft:stone");
        display.add("icon", icon);
        display.addProperty("title", "Default Advancement");
        display.addProperty("description", "Default advancement description");
        advancement.add("display", display);
        
        JsonObject criteria = new JsonObject();
        JsonObject trigger = new JsonObject();
        trigger.addProperty("trigger", "minecraft:tick");
        criteria.add("default", trigger);
        advancement.add("criteria", criteria);
        
        return GSON.toJson(advancement);
    }
    
    private static String generateDefaultLootTableJson() {
        JsonObject lootTable = new JsonObject();
        lootTable.addProperty("type", "minecraft:empty");
        
        return GSON.toJson(lootTable);
    }
    
    /**
     * Batch repair multiple JSON files
     */
    public static Map<Path, RepairResult> batchRepair(List<Path> jsonFiles) {
        Origins.LOGGER.info("Starting batch repair of {} JSON files", jsonFiles.size());
        
        Map<Path, RepairResult> results = new HashMap<>();
        int successCount = 0;
        
        for (Path jsonFile : jsonFiles) {
            RepairResult result = repairJsonFile(jsonFile);
            results.put(jsonFile, result);
            
            if (result.success) {
                successCount++;
            }
        }
        
        Origins.LOGGER.info("Batch repair completed: {}/{} files repaired successfully", 
                          successCount, jsonFiles.size());
        
        return results;
    }
    
    /**
     * Restores a file from backup
     */
    public static boolean restoreFromBackup(Path filePath, Path backupPath) {
        try {
            Files.copy(backupPath, filePath, StandardCopyOption.REPLACE_EXISTING);
            Origins.LOGGER.info("Successfully restored {} from backup {}", filePath, backupPath);
            return true;
        } catch (IOException e) {
            Origins.LOGGER.error("Failed to restore {} from backup {}: {}", 
                               filePath, backupPath, e.getMessage());
            return false;
        }
    }
}