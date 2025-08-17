package io.github.apace100.origins.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.apace100.origins.Origins;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Comprehensive mod compatibility checker
 * Analyzes installed mods for version conflicts, known incompatibilities, and potential issues
 */
public class ModCompatibilityChecker {
    
    // Known incompatible mod combinations
    private static final Map<String, Set<String>> KNOWN_INCOMPATIBILITIES = new HashMap<>();
    
    // Version patterns that are known to cause issues
    private static final Map<String, Pattern> PROBLEMATIC_VERSIONS = new HashMap<>();
    
    // Mods that require specific Origins versions
    private static final Map<String, String> ORIGINS_VERSION_REQUIREMENTS = new HashMap<>();
    
    static {
        // Initialize known incompatibilities
        KNOWN_INCOMPATIBILITIES.put("optifabric", Set.of("sodium", "iris"));
        KNOWN_INCOMPATIBILITIES.put("identity", Set.of("origins"));
        
        // Initialize problematic version patterns
        PROBLEMATIC_VERSIONS.put("optifabric", Pattern.compile("1\\.11\\.[0-9]+"));
        
        // Initialize Origins version requirements
        ORIGINS_VERSION_REQUIREMENTS.put("pehkui", "2.9.0");
        ORIGINS_VERSION_REQUIREMENTS.put("apoli", "2.9.0");
        ORIGINS_VERSION_REQUIREMENTS.put("calio", "1.11.0");
    }
    
    public static class CompatibilityReport {
        public final List<ModInfo> installedMods = new ArrayList<>();
        public final List<VersionConflict> versionConflicts = new ArrayList<>();
        public final List<IncompatibilityIssue> incompatibilities = new ArrayList<>();
        public final List<MissingDependency> missingDependencies = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public final List<String> recommendations = new ArrayList<>();
        
        public boolean hasIssues() {
            return !versionConflicts.isEmpty() || !incompatibilities.isEmpty() || !missingDependencies.isEmpty();
        }
        
        public boolean hasCriticalIssues() {
            return incompatibilities.stream().anyMatch(i -> i.severity == Severity.CRITICAL) ||
                   versionConflicts.stream().anyMatch(v -> v.severity == Severity.CRITICAL);
        }
    }
    
    public static class ModInfo {
        public final String modId;
        public final String name;
        public final String version;
        public final String description;
        public final List<String> authors;
        public final boolean isOriginsMod;
        
        public ModInfo(String modId, String name, String version, String description, 
                      List<String> authors, boolean isOriginsMod) {
            this.modId = modId;
            this.name = name;
            this.version = version;
            this.description = description;
            this.authors = authors != null ? authors : new ArrayList<>();
            this.isOriginsMod = isOriginsMod;
        }
    }
    
    public static class VersionConflict {
        public final String modId;
        public final String currentVersion;
        public final String requiredVersion;
        public final String conflictReason;
        public final Severity severity;
        
        public VersionConflict(String modId, String currentVersion, String requiredVersion, 
                             String conflictReason, Severity severity) {
            this.modId = modId;
            this.currentVersion = currentVersion;
            this.requiredVersion = requiredVersion;
            this.conflictReason = conflictReason;
            this.severity = severity;
        }
    }
    
    public static class IncompatibilityIssue {
        public final String mod1;
        public final String mod2;
        public final String reason;
        public final Severity severity;
        public final String solution;
        
        public IncompatibilityIssue(String mod1, String mod2, String reason, Severity severity, String solution) {
            this.mod1 = mod1;
            this.mod2 = mod2;
            this.reason = reason;
            this.severity = severity;
            this.solution = solution;
        }
    }
    
    public static class MissingDependency {
        public final String modId;
        public final String missingDependency;
        public final String requiredVersion;
        public final boolean isOptional;
        
        public MissingDependency(String modId, String missingDependency, String requiredVersion, boolean isOptional) {
            this.modId = modId;
            this.missingDependency = missingDependency;
            this.requiredVersion = requiredVersion;
            this.isOptional = isOptional;
        }
    }
    
    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Runs comprehensive compatibility check
     */
    public static CompatibilityReport runCompatibilityCheck() {
                
        CompatibilityReport report = new CompatibilityReport();
        
        // Collect information about all installed mods
        collectModInformation(report);
        
        // Check for version conflicts
        checkVersionConflicts(report);
        
        // Check for known incompatibilities
        checkKnownIncompatibilities(report);
        
        // Check for missing dependencies
        checkMissingDependencies(report);
        
        // Generate recommendations
        generateRecommendations(report);
        
        // Log results
        logCompatibilityResults(report);
        
        return report;
    }
    
    /**
     * Collects information about all installed mods
     */
    private static void collectModInformation(CompatibilityReport report) {
        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
        
        for (ModContainer mod : mods) {
            ModMetadata metadata = mod.getMetadata();
            
            boolean isOriginsMod = isOriginsRelatedMod(metadata.getId());
            
            ModInfo modInfo = new ModInfo(
                metadata.getId(),
                metadata.getName(),
                metadata.getVersion().getFriendlyString(),
                metadata.getDescription(),
                metadata.getAuthors().stream()
                    .map(person -> person.getName())
                    .collect(Collectors.toList()),
                isOriginsMod
            );
            
            report.installedMods.add(modInfo);
        }
        
        Origins.LOGGER.info("Found {} installed mods ({} Origins-related)", 
                          report.installedMods.size(),
                          report.installedMods.stream().mapToInt(m -> m.isOriginsMod ? 1 : 0).sum());
    }
    
    /**
     * Checks if a mod is related to Origins
     */
    private static boolean isOriginsRelatedMod(String modId) {
        return modId.equals("origins") || 
               modId.equals("apoli") || 
               modId.equals("calio") || 
               modId.equals("pehkui") ||
               modId.contains("origins") ||
               modId.contains("origin");
    }
    
    /**
     * Checks for version conflicts
     */
    private static void checkVersionConflicts(CompatibilityReport report) {
        Map<String, ModInfo> modMap = report.installedMods.stream()
            .collect(Collectors.toMap(m -> m.modId, m -> m));
        
        // Check Origins version requirements
        for (Map.Entry<String, String> requirement : ORIGINS_VERSION_REQUIREMENTS.entrySet()) {
            String modId = requirement.getKey();
            String requiredVersion = requirement.getValue();
            
            ModInfo mod = modMap.get(modId);
            if (mod != null) {
                if (!isVersionCompatible(mod.version, requiredVersion)) {
                    report.versionConflicts.add(new VersionConflict(
                        modId,
                        mod.version,
                        requiredVersion,
                        "Required for Origins compatibility",
                        Severity.HIGH
                    ));
                }
            }
        }
        
        // Check for problematic versions
        for (Map.Entry<String, Pattern> problematic : PROBLEMATIC_VERSIONS.entrySet()) {
            String modId = problematic.getKey();
            Pattern versionPattern = problematic.getValue();
            
            ModInfo mod = modMap.get(modId);
            if (mod != null && versionPattern.matcher(mod.version).matches()) {
                report.versionConflicts.add(new VersionConflict(
                    modId,
                    mod.version,
                    "newer version",
                    "Known to cause issues with Origins",
                    Severity.CRITICAL
                ));
            }
        }
        
        // Check Minecraft version compatibility
        String minecraftVersion = FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
        
        if (!minecraftVersion.startsWith("1.20.1")) {
            report.warnings.add("Origins is designed for Minecraft 1.20.1, current version: " + minecraftVersion);
        }
    }
    
    /**
     * Checks for known incompatibilities
     */
    private static void checkKnownIncompatibilities(CompatibilityReport report) {
        Set<String> installedModIds = report.installedMods.stream()
            .map(m -> m.modId)
            .collect(Collectors.toSet());
        
        for (Map.Entry<String, Set<String>> incompatibility : KNOWN_INCOMPATIBILITIES.entrySet()) {
            String mod1 = incompatibility.getKey();
            Set<String> incompatibleMods = incompatibility.getValue();
            
            if (installedModIds.contains(mod1)) {
                for (String mod2 : incompatibleMods) {
                    if (installedModIds.contains(mod2)) {
                        report.incompatibilities.add(new IncompatibilityIssue(
                            mod1,
                            mod2,
                            "Known incompatibility",
                            Severity.CRITICAL,
                            "Remove one of the conflicting mods"
                        ));
                    }
                }
            }
        }
        
        // Check for specific Origins incompatibilities
        if (installedModIds.contains("identity")) {
            String identityVersion = report.installedMods.stream()
                .filter(m -> m.modId.equals("identity"))
                .findFirst()
                .map(m -> m.version)
                .orElse("unknown");
            
            if (isVersionOlderThan(identityVersion, "1.14.2")) {
                report.incompatibilities.add(new IncompatibilityIssue(
                    "origins",
                    "identity",
                    "Identity version " + identityVersion + " is incompatible with Origins",
                    Severity.CRITICAL,
                    "Update Identity to version 1.14.2 or newer"
                ));
            }
        }
    }
    
    /**
     * Checks for missing dependencies
     */
    private static void checkMissingDependencies(CompatibilityReport report) {
        Set<String> installedModIds = report.installedMods.stream()
            .map(m -> m.modId)
            .collect(Collectors.toSet());
        
        // Check Origins dependencies
        String[] requiredDependencies = {
            "apoli", "calio", "playerabilitylib", "cardinal-components-base", 
            "cardinal-components-entity", "cloth-config2"
        };
        
        for (String dependency : requiredDependencies) {
            if (!installedModIds.contains(dependency)) {
                report.missingDependencies.add(new MissingDependency(
                    "origins",
                    dependency,
                    "latest",
                    false
                ));
            }
        }
        
        // Check optional but recommended dependencies
        String[] recommendedDependencies = {"modmenu", "rei", "emi"};
        
        for (String dependency : recommendedDependencies) {
            if (!installedModIds.contains(dependency)) {
                report.warnings.add("Recommended mod not installed: " + dependency);
            }
        }
    }
    
    /**
     * Generates recommendations based on findings
     */
    private static void generateRecommendations(CompatibilityReport report) {
        if (report.versionConflicts.isEmpty() && report.incompatibilities.isEmpty()) {
            report.recommendations.add("All mods appear to be compatible!");
        }
        
        if (!report.versionConflicts.isEmpty()) {
            report.recommendations.add("Update the following mods to resolve version conflicts:");
            for (VersionConflict conflict : report.versionConflicts) {
                report.recommendations.add("- " + conflict.modId + ": " + conflict.conflictReason);
            }
        }
        
        if (!report.incompatibilities.isEmpty()) {
            report.recommendations.add("Resolve the following incompatibilities:");
            for (IncompatibilityIssue issue : report.incompatibilities) {
                report.recommendations.add("- " + issue.solution);
            }
        }
        
        if (!report.missingDependencies.isEmpty()) {
            report.recommendations.add("Install the following missing dependencies:");
            for (MissingDependency missing : report.missingDependencies) {
                report.recommendations.add("- " + missing.missingDependency + 
                                         (missing.isOptional ? " (optional)" : " (required)"));
            }
        }
        
        // Performance recommendations
        long originsRelatedMods = report.installedMods.stream()
            .mapToInt(m -> m.isOriginsMod ? 1 : 0)
            .sum();
        
        if (originsRelatedMods > 10) {
            report.recommendations.add("Consider reducing the number of Origins-related mods for better performance");
        }
        
        // Memory recommendations
        if (report.installedMods.size() > 100) {
            report.recommendations.add("Large number of mods detected - consider increasing allocated memory");
        }
    }
    
    /**
     * Logs compatibility results
     */
    private static void logCompatibilityResults(CompatibilityReport report) {
                                                        
        if (report.hasCriticalIssues()) {
            Origins.LOGGER.error("CRITICAL ISSUES DETECTED:");
            for (IncompatibilityIssue issue : report.incompatibilities) {
                if (issue.severity == Severity.CRITICAL) {
                    Origins.LOGGER.error("- {} and {} are incompatible: {}", 
                                       issue.mod1, issue.mod2, issue.reason);
                }
            }
            for (VersionConflict conflict : report.versionConflicts) {
                if (conflict.severity == Severity.CRITICAL) {
                    Origins.LOGGER.error("- {} version {} is problematic: {}", 
                                       conflict.modId, conflict.currentVersion, conflict.conflictReason);
                }
            }
        }
        
        if (!report.warnings.isEmpty()) {
            Origins.LOGGER.warn("Warnings:");
            for (String warning : report.warnings) {
                Origins.LOGGER.warn("- {}", warning);
            }
        }
        
        if (!report.recommendations.isEmpty()) {
                        for (String recommendation : report.recommendations) {
                            }
        }
    }
    
    // Utility methods
    private static boolean isVersionCompatible(String currentVersion, String requiredVersion) {
        try {
            // Simple version comparison - can be enhanced with proper version parsing
            return currentVersion.compareTo(requiredVersion) >= 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean isVersionOlderThan(String currentVersion, String compareVersion) {
        try {
            return currentVersion.compareTo(compareVersion) < 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if the current mod environment is suitable for Origins
     */
    public static boolean isEnvironmentSuitable() {
        CompatibilityReport report = runCompatibilityCheck();
        return !report.hasCriticalIssues();
    }
    
    /**
     * Gets a summary of compatibility issues
     */
    public static String getCompatibilitySummary() {
        CompatibilityReport report = runCompatibilityCheck();
        
        StringBuilder summary = new StringBuilder();
        summary.append("Mod Compatibility Summary:\n");
        summary.append("- Total mods: ").append(report.installedMods.size()).append("\n");
        summary.append("- Version conflicts: ").append(report.versionConflicts.size()).append("\n");
        summary.append("- Incompatibilities: ").append(report.incompatibilities.size()).append("\n");
        summary.append("- Missing dependencies: ").append(report.missingDependencies.size()).append("\n");
        
        if (report.hasCriticalIssues()) {
            summary.append("\nCRITICAL ISSUES DETECTED - Check logs for details");
        } else if (report.hasIssues()) {
            summary.append("\nMinor issues detected - Check logs for details");
        } else {
            summary.append("\nAll mods appear compatible!");
        }
        
        return summary.toString();
    }
}