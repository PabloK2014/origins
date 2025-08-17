package io.github.apace100.origins.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.apace100.origins.Origins;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive diagnostic logging and reporting system
 * Generates detailed reports for troubleshooting and debugging
 */
public class DiagnosticReporter {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Map<String, List<DiagnosticEvent>> EVENT_LOG = new ConcurrentHashMap<>();
    
    public static class DiagnosticEvent {
        public final long timestamp;
        public final String category;
        public final String level;
        public final String message;
        public final Map<String, Object> data;
        
        public DiagnosticEvent(String category, String level, String message, Map<String, Object> data) {
            this.timestamp = System.currentTimeMillis();
            this.category = category;
            this.level = level;
            this.message = message;
            this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        }
    }
    
    public static class DiagnosticReport {
        public final String timestamp;
        public final String originsVersion;
        public final String minecraftVersion;
        public final String fabricVersion;
        public final SystemInfo systemInfo;
        public final JsonDiagnostic.ValidationResult jsonValidation;
        public final TextureValidator.ValidationReport textureValidation;
        public final KeybindingDiagnostic.DiagnosticReport keybindingValidation;
        public final ModCompatibilityChecker.CompatibilityReport compatibilityCheck;
        public final Map<String, List<DiagnosticEvent>> eventLog;
        public final List<String> recommendations;
        
        public DiagnosticReport(SystemInfo systemInfo,
                              JsonDiagnostic.ValidationResult jsonValidation,
                              TextureValidator.ValidationReport textureValidation,
                              KeybindingDiagnostic.DiagnosticReport keybindingValidation,
                              ModCompatibilityChecker.CompatibilityReport compatibilityCheck,
                              Map<String, List<DiagnosticEvent>> eventLog) {
            this.timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            this.originsVersion = Origins.VERSION;
            this.minecraftVersion = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
            this.fabricVersion = FabricLoader.getInstance()
                .getModContainer("fabricloader")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
            this.systemInfo = systemInfo;
            this.jsonValidation = jsonValidation;
            this.textureValidation = textureValidation;
            this.keybindingValidation = keybindingValidation;
            this.compatibilityCheck = compatibilityCheck;
            this.eventLog = new HashMap<>(eventLog);
            this.recommendations = generateRecommendations();
        }
        
        private List<String> generateRecommendations() {
            List<String> recommendations = new ArrayList<>();
            
            // JSON recommendations
            if (jsonValidation != null && !jsonValidation.isValid) {
                recommendations.add("Fix JSON validation issues: " + jsonValidation.errorMessage);
            }
            
            // Texture recommendations
            if (textureValidation != null && textureValidation.hasIssues()) {
                recommendations.add("Fix texture loading issues - " + 
                                 textureValidation.invalidTextures.size() + " textures missing");
            }
            
            // Keybinding recommendations
            if (keybindingValidation != null && keybindingValidation.hasIssues()) {
                recommendations.add("Resolve keybinding conflicts - " + 
                                 keybindingValidation.conflicts.size() + " conflicts detected");
            }
            
            // Compatibility recommendations
            if (compatibilityCheck != null) {
                recommendations.addAll(compatibilityCheck.recommendations);
            }
            
            // System recommendations
            if (systemInfo != null) {
                if (systemInfo.availableMemory < 2048) {
                    recommendations.add("Consider increasing allocated memory (current: " + 
                                     systemInfo.availableMemory + "MB)");
                }
                if (systemInfo.javaVersion < 17) {
                    recommendations.add("Update to Java 17 or newer (current: Java " + 
                                     systemInfo.javaVersion + ")");
                }
            }
            
            return recommendations;
        }
        
        public boolean hasIssues() {
            return (jsonValidation != null && !jsonValidation.isValid) ||
                   (textureValidation != null && textureValidation.hasIssues()) ||
                   (keybindingValidation != null && keybindingValidation.hasIssues()) ||
                   (compatibilityCheck != null && compatibilityCheck.hasIssues());
        }
        
        public boolean hasCriticalIssues() {
            return (compatibilityCheck != null && compatibilityCheck.hasCriticalIssues());
        }
    }
    
    public static class SystemInfo {
        public final String operatingSystem;
        public final String javaVendor;
        public final int javaVersion;
        public final long totalMemory;
        public final long availableMemory;
        public final long usedMemory;
        public final int processorCount;
        public final String gameDirectory;
        
        public SystemInfo() {
            Runtime runtime = Runtime.getRuntime();
            
            this.operatingSystem = System.getProperty("os.name") + " " + System.getProperty("os.version");
            this.javaVendor = System.getProperty("java.vendor");
            this.javaVersion = getJavaVersion();
            this.totalMemory = runtime.totalMemory() / (1024 * 1024); // MB
            this.availableMemory = runtime.maxMemory() / (1024 * 1024); // MB
            this.usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024); // MB
            this.processorCount = runtime.availableProcessors();
            this.gameDirectory = FabricLoader.getInstance().getGameDir().toString();
        }
        
        private int getJavaVersion() {
            String version = System.getProperty("java.version");
            if (version.startsWith("1.")) {
                version = version.substring(2, 3);
            } else {
                int dot = version.indexOf(".");
                if (dot != -1) {
                    version = version.substring(0, dot);
                }
            }
            try {
                return Integer.parseInt(version);
            } catch (NumberFormatException e) {
                return 8; // fallback
            }
        }
    }
    
    /**
     * Logs a diagnostic event
     */
    public static void logEvent(String category, String level, String message, Map<String, Object> data) {
        DiagnosticEvent event = new DiagnosticEvent(category, level, message, data);
        EVENT_LOG.computeIfAbsent(category, k -> new ArrayList<>()).add(event);
        
        // Also log to Origins logger
        switch (level.toUpperCase()) {
            case "ERROR":
                Origins.LOGGER.error("[{}] {}", category, message);
                break;
            case "WARN":
                Origins.LOGGER.warn("[{}] {}", category, message);
                break;
            case "INFO":
                                break;
            case "DEBUG":
                                break;
            default:
                                break;
        }
    }
    
    /**
     * Logs a diagnostic event with simple message
     */
    public static void logEvent(String category, String level, String message) {
        logEvent(category, level, message, null);
    }
    
    /**
     * Generates a comprehensive diagnostic report
     */
    public static DiagnosticReport generateReport() {
                
        // Collect system information
        SystemInfo systemInfo = new SystemInfo();
        
        // Run all diagnostic checks
        JsonDiagnostic.ValidationResult jsonValidation = null;
        TextureValidator.ValidationReport textureValidation = null;
        KeybindingDiagnostic.DiagnosticReport keybindingValidation = null;
        ModCompatibilityChecker.CompatibilityReport compatibilityCheck = null;
        
        try {
            // Note: JSON validation would need a specific file to validate
            // For now, we'll skip it in the report generation
            
            textureValidation = TextureValidator.validateAllTextures();
            keybindingValidation = KeybindingDiagnostic.runDiagnostic();
            compatibilityCheck = ModCompatibilityChecker.runCompatibilityCheck();
        } catch (Exception e) {
            Origins.LOGGER.error("Error during diagnostic report generation: " + e.getMessage(), e);
            logEvent("DIAGNOSTIC", "ERROR", "Failed to generate complete report: " + e.getMessage());
        }
        
        DiagnosticReport report = new DiagnosticReport(
            systemInfo,
            jsonValidation,
            textureValidation,
            keybindingValidation,
            compatibilityCheck,
            EVENT_LOG
        );
        
                return report;
    }
    
    /**
     * Saves diagnostic report to file
     */
    public static Path saveReportToFile(DiagnosticReport report) {
        try {
            Path reportsDir = FabricLoader.getInstance().getGameDir().resolve("origins_diagnostics");
            Files.createDirectories(reportsDir);
            
            String filename = "origins_diagnostic_" + report.timestamp + ".json";
            Path reportFile = reportsDir.resolve(filename);
            
            String jsonReport = GSON.toJson(report);
            Files.writeString(reportFile, jsonReport);
            
                        return reportFile;
        } catch (IOException e) {
            Origins.LOGGER.error("Failed to save diagnostic report: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Saves diagnostic report as human-readable text
     */
    public static Path saveReportAsText(DiagnosticReport report) {
        try {
            Path reportsDir = FabricLoader.getInstance().getGameDir().resolve("origins_diagnostics");
            Files.createDirectories(reportsDir);
            
            String filename = "origins_diagnostic_" + report.timestamp + ".txt";
            Path reportFile = reportsDir.resolve(filename);
            
            StringBuilder textReport = new StringBuilder();
            
            // Header
            textReport.append("=== ORIGINS DIAGNOSTIC REPORT ===\n");
            textReport.append("Generated: ").append(report.timestamp).append("\n");
            textReport.append("Origins Version: ").append(report.originsVersion).append("\n");
            textReport.append("Minecraft Version: ").append(report.minecraftVersion).append("\n");
            textReport.append("Fabric Version: ").append(report.fabricVersion).append("\n\n");
            
            // System Information
            textReport.append("=== SYSTEM INFORMATION ===\n");
            if (report.systemInfo != null) {
                textReport.append("OS: ").append(report.systemInfo.operatingSystem).append("\n");
                textReport.append("Java: ").append(report.systemInfo.javaVendor).append(" ").append(report.systemInfo.javaVersion).append("\n");
                textReport.append("Memory: ").append(report.systemInfo.usedMemory).append("/").append(report.systemInfo.availableMemory).append(" MB\n");
                textReport.append("Processors: ").append(report.systemInfo.processorCount).append("\n");
                textReport.append("Game Directory: ").append(report.systemInfo.gameDirectory).append("\n\n");
            }
            
            // Texture Validation
            textReport.append("=== TEXTURE VALIDATION ===\n");
            if (report.textureValidation != null) {
                textReport.append("Valid Textures: ").append(report.textureValidation.validTextures.size()).append("\n");
                textReport.append("Invalid Textures: ").append(report.textureValidation.invalidTextures.size()).append("\n");
                textReport.append("Fallbacks Used: ").append(report.textureValidation.fallbacksUsed.size()).append("\n");
                
                if (!report.textureValidation.invalidTextures.isEmpty()) {
                    textReport.append("Missing Textures:\n");
                    for (var texture : report.textureValidation.invalidTextures) {
                        textReport.append("  - ").append(texture).append("\n");
                    }
                }
                textReport.append("\n");
            }
            
            // Keybinding Validation
            textReport.append("=== KEYBINDING VALIDATION ===\n");
            if (report.keybindingValidation != null) {
                textReport.append("Total Keybindings: ").append(report.keybindingValidation.registeredKeybindings.size()).append("\n");
                textReport.append("Origins Keybindings: ").append(report.keybindingValidation.originsKeybindings.size()).append("\n");
                textReport.append("Conflicts: ").append(report.keybindingValidation.conflicts.size()).append("\n");
                textReport.append("Issues: ").append(report.keybindingValidation.issues.size()).append("\n");
                
                if (!report.keybindingValidation.conflicts.isEmpty()) {
                    textReport.append("Keybinding Conflicts:\n");
                    for (var conflict : report.keybindingValidation.conflicts) {
                        textReport.append("  - ").append(conflict.keybindingName).append(" conflicts with: ")
                                 .append(String.join(", ", conflict.conflictingMods)).append("\n");
                    }
                }
                textReport.append("\n");
            }
            
            // Compatibility Check
            textReport.append("=== MOD COMPATIBILITY ===\n");
            if (report.compatibilityCheck != null) {
                textReport.append("Total Mods: ").append(report.compatibilityCheck.installedMods.size()).append("\n");
                textReport.append("Version Conflicts: ").append(report.compatibilityCheck.versionConflicts.size()).append("\n");
                textReport.append("Incompatibilities: ").append(report.compatibilityCheck.incompatibilities.size()).append("\n");
                textReport.append("Missing Dependencies: ").append(report.compatibilityCheck.missingDependencies.size()).append("\n");
                
                if (!report.compatibilityCheck.incompatibilities.isEmpty()) {
                    textReport.append("Incompatible Mods:\n");
                    for (var incompatibility : report.compatibilityCheck.incompatibilities) {
                        textReport.append("  - ").append(incompatibility.mod1).append(" vs ").append(incompatibility.mod2)
                                 .append(": ").append(incompatibility.reason).append("\n");
                    }
                }
                textReport.append("\n");
            }
            
            // Recommendations
            textReport.append("=== RECOMMENDATIONS ===\n");
            if (!report.recommendations.isEmpty()) {
                for (String recommendation : report.recommendations) {
                    textReport.append("- ").append(recommendation).append("\n");
                }
            } else {
                textReport.append("No issues detected - everything looks good!\n");
            }
            textReport.append("\n");
            
            // Event Log Summary
            textReport.append("=== EVENT LOG SUMMARY ===\n");
            for (Map.Entry<String, List<DiagnosticEvent>> entry : report.eventLog.entrySet()) {
                textReport.append(entry.getKey()).append(": ").append(entry.getValue().size()).append(" events\n");
            }
            
            Files.writeString(reportFile, textReport.toString());
            
                        return reportFile;
        } catch (IOException e) {
            Origins.LOGGER.error("Failed to save text diagnostic report: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Clears the event log
     */
    public static void clearEventLog() {
        EVENT_LOG.clear();
            }
    
    /**
     * Gets events for a specific category
     */
    public static List<DiagnosticEvent> getEvents(String category) {
        return EVENT_LOG.getOrDefault(category, new ArrayList<>());
    }
    
    /**
     * Gets all events
     */
    public static Map<String, List<DiagnosticEvent>> getAllEvents() {
        return new HashMap<>(EVENT_LOG);
    }
    
    /**
     * Runs a quick diagnostic check and returns summary
     */
    public static String getQuickDiagnosticSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== QUICK DIAGNOSTIC SUMMARY ===\n");
        
        try {
            // System info
            SystemInfo systemInfo = new SystemInfo();
            summary.append("System: ").append(systemInfo.operatingSystem).append("\n");
            summary.append("Java: ").append(systemInfo.javaVersion).append("\n");
            summary.append("Memory: ").append(systemInfo.usedMemory).append("/").append(systemInfo.availableMemory).append(" MB\n");
            
            // Quick checks
            TextureValidator.ValidationReport textureReport = TextureValidator.validateAllTextures();
            summary.append("Textures: ").append(textureReport.validTextures.size()).append(" OK, ")
                   .append(textureReport.invalidTextures.size()).append(" missing\n");
            
            ModCompatibilityChecker.CompatibilityReport compatReport = ModCompatibilityChecker.runCompatibilityCheck();
            summary.append("Mods: ").append(compatReport.installedMods.size()).append(" total, ")
                   .append(compatReport.incompatibilities.size()).append(" conflicts\n");
            
            if (textureReport.hasIssues() || compatReport.hasIssues()) {
                summary.append("STATUS: Issues detected - run full diagnostic for details\n");
            } else {
                summary.append("STATUS: All systems operational\n");
            }
            
        } catch (Exception e) {
            summary.append("ERROR: Failed to generate summary - ").append(e.getMessage()).append("\n");
        }
        
        return summary.toString();
    }
}