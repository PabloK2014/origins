package io.github.apace100.origins.util;

import io.github.apace100.origins.Origins;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Comprehensive keybinding diagnostic system
 * Validates keybinding registration, detects conflicts, and provides debugging information
 */
public class KeybindingDiagnostic {
    
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static class KeyConflict {
        public final String modId;
        public final String keybindingName;
        public final int keyCode;
        public final List<String> conflictingMods;
        
        public KeyConflict(String modId, String keybindingName, int keyCode, List<String> conflictingMods) {
            this.modId = modId;
            this.keybindingName = keybindingName;
            this.keyCode = keyCode;
            this.conflictingMods = new ArrayList<>(conflictingMods);
        }
    }
    
    public static class DiagnosticReport {
        public final List<KeyBinding> registeredKeybindings = new ArrayList<>();
        public final List<KeyConflict> conflicts = new ArrayList<>();
        public final Map<String, Boolean> originsKeybindings = new HashMap<>();
        public final List<String> issues = new ArrayList<>();
        
        public boolean hasIssues() {
            return !conflicts.isEmpty() || !issues.isEmpty();
        }
    }
    
    /**
     * Runs comprehensive keybinding diagnostic
     */
    public static DiagnosticReport runDiagnostic() {
        Origins.LOGGER.info("Starting keybinding diagnostic...");
        
        DiagnosticReport report = new DiagnosticReport();
        
        // Check if client is available
        if (client == null) {
            report.issues.add("MinecraftClient is not available");
            return report;
        }
        
        // Get all registered keybindings
        report.registeredKeybindings.addAll(getAllKeybindings());
        
        // Check Origins-specific keybindings
        checkOriginsKeybindings(report);
        
        // Detect conflicts
        report.conflicts.addAll(detectConflicts(report.registeredKeybindings));
        
        // Log results
        logDiagnosticResults(report);
        
        return report;
    }
    
    /**
     * Gets all registered keybindings using reflection
     */
    private static List<KeyBinding> getAllKeybindings() {
        List<KeyBinding> keybindings = new ArrayList<>();
        
        try {
            // Access the keybindings map from KeyBindingHelper
            Field keybindingsField = KeyBindingHelper.class.getDeclaredField("KEYBINDINGS");
            keybindingsField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<String, KeyBinding> keybindingsMap = (Map<String, KeyBinding>) keybindingsField.get(null);
            
            if (keybindingsMap != null) {
                keybindings.addAll(keybindingsMap.values());
            }
        } catch (Exception e) {
            Origins.LOGGER.warn("Could not access keybindings via reflection: " + e.getMessage());
            
            // Fallback: get keybindings from client options
            if (client.options != null) {
                keybindings.addAll(Arrays.asList(client.options.allKeys));
            }
        }
        
        return keybindings;
    }
    
    /**
     * Checks Origins-specific keybindings
     */
    private static void checkOriginsKeybindings(DiagnosticReport report) {
        // Define expected Origins keybindings
        Map<String, Integer> expectedKeybindings = new HashMap<>();
        expectedKeybindings.put("key.origins.activate_global_skill", GLFW.GLFW_KEY_G);
        expectedKeybindings.put("key.origins.activate_active_skill", GLFW.GLFW_KEY_K);
        expectedKeybindings.put("key.origins.open_skill_selection", GLFW.GLFW_KEY_L);
        
        for (KeyBinding keybinding : report.registeredKeybindings) {
            String translationKey = keybinding.getTranslationKey();
            
            if (expectedKeybindings.containsKey(translationKey)) {
                int expectedKey = expectedKeybindings.get(translationKey);
                int actualKey = keybinding.getDefaultKey().getCode();
                
                boolean isCorrect = actualKey == expectedKey;
                report.originsKeybindings.put(translationKey, isCorrect);
                
                if (!isCorrect) {
                    report.issues.add("Keybinding " + translationKey + " has wrong key: expected " + 
                                    getKeyName(expectedKey) + ", got " + getKeyName(actualKey));
                }
                
                Origins.LOGGER.info("Found Origins keybinding: " + translationKey + " -> " + getKeyName(actualKey));
            }
        }
        
        // Check for missing keybindings
        for (String expectedKey : expectedKeybindings.keySet()) {
            if (!report.originsKeybindings.containsKey(expectedKey)) {
                report.issues.add("Missing Origins keybinding: " + expectedKey);
                report.originsKeybindings.put(expectedKey, false);
            }
        }
    }
    
    /**
     * Detects keybinding conflicts
     */
    private static List<KeyConflict> detectConflicts(List<KeyBinding> keybindings) {
        List<KeyConflict> conflicts = new ArrayList<>();
        Map<Integer, List<KeyBinding>> keyMap = new HashMap<>();
        
        // Group keybindings by key code
        for (KeyBinding keybinding : keybindings) {
            int keyCode = keybinding.getDefaultKey().getCode();
            keyMap.computeIfAbsent(keyCode, k -> new ArrayList<>()).add(keybinding);
        }
        
        // Find conflicts
        for (Map.Entry<Integer, List<KeyBinding>> entry : keyMap.entrySet()) {
            int keyCode = entry.getKey();
            List<KeyBinding> bindingsForKey = entry.getValue();
            
            if (bindingsForKey.size() > 1) {
                // Check if any Origins keybindings are involved
                boolean hasOriginsKeybinding = bindingsForKey.stream()
                    .anyMatch(kb -> kb.getTranslationKey().startsWith("key.origins."));
                
                if (hasOriginsKeybinding) {
                    List<String> conflictingMods = new ArrayList<>();
                    String originsKeybinding = null;
                    
                    for (KeyBinding kb : bindingsForKey) {
                        String translationKey = kb.getTranslationKey();
                        if (translationKey.startsWith("key.origins.")) {
                            originsKeybinding = translationKey;
                        } else {
                            // Extract mod ID from translation key
                            String[] parts = translationKey.split("\\.");
                            if (parts.length > 1) {
                                conflictingMods.add(parts[1]);
                            } else {
                                conflictingMods.add("unknown");
                            }
                        }
                    }
                    
                    if (originsKeybinding != null) {
                        conflicts.add(new KeyConflict("origins", originsKeybinding, keyCode, conflictingMods));
                    }
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * Gets human-readable key name
     */
    private static String getKeyName(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return "UNKNOWN";
        }
        
        // Common key mappings
        switch (keyCode) {
            case GLFW.GLFW_KEY_G: return "G";
            case GLFW.GLFW_KEY_K: return "K";
            case GLFW.GLFW_KEY_L: return "L";
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_ENTER: return "ENTER";
            case GLFW.GLFW_KEY_ESCAPE: return "ESCAPE";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LEFT_SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LEFT_CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LEFT_ALT";
            default:
                // For letter keys
                if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
                    return String.valueOf((char) keyCode);
                }
                // For number keys
                if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
                    return String.valueOf((char) keyCode);
                }
                return "KEY_" + keyCode;
        }
    }
    
    /**
     * Logs diagnostic results
     */
    private static void logDiagnosticResults(DiagnosticReport report) {
        Origins.LOGGER.info("Keybinding diagnostic complete:");
        Origins.LOGGER.info("- Total keybindings found: " + report.registeredKeybindings.size());
        Origins.LOGGER.info("- Origins keybindings found: " + report.originsKeybindings.size());
        Origins.LOGGER.info("- Conflicts detected: " + report.conflicts.size());
        Origins.LOGGER.info("- Issues found: " + report.issues.size());
        
        // Log Origins keybindings status
        Origins.LOGGER.info("Origins keybindings status:");
        for (Map.Entry<String, Boolean> entry : report.originsKeybindings.entrySet()) {
            String status = entry.getValue() ? "OK" : "ISSUE";
            Origins.LOGGER.info("  " + entry.getKey() + ": " + status);
        }
        
        // Log conflicts
        if (!report.conflicts.isEmpty()) {
            Origins.LOGGER.warn("Keybinding conflicts found:");
            for (KeyConflict conflict : report.conflicts) {
                Origins.LOGGER.warn("  " + conflict.keybindingName + " (" + getKeyName(conflict.keyCode) + 
                                  ") conflicts with mods: " + String.join(", ", conflict.conflictingMods));
            }
        }
        
        // Log issues
        if (!report.issues.isEmpty()) {
            Origins.LOGGER.warn("Keybinding issues found:");
            for (String issue : report.issues) {
                Origins.LOGGER.warn("  " + issue);
            }
        }
    }
    
    /**
     * Attempts to repair keybinding issues
     */
    public static boolean repairKeybindings(DiagnosticReport report) {
        Origins.LOGGER.info("Attempting to repair keybinding issues...");
        
        boolean repaired = false;
        
        // Try to resolve conflicts by suggesting alternative keys
        for (KeyConflict conflict : report.conflicts) {
            if (conflict.modId.equals("origins")) {
                int alternativeKey = findAlternativeKey(conflict.keyCode, report.registeredKeybindings);
                if (alternativeKey != GLFW.GLFW_KEY_UNKNOWN) {
                    Origins.LOGGER.info("Suggested alternative key for " + conflict.keybindingName + 
                                      ": " + getKeyName(alternativeKey));
                    // Note: Actual key reassignment would require more complex implementation
                }
            }
        }
        
        // Check if SkillKeybinds class is properly initialized
        if (report.originsKeybindings.isEmpty()) {
            Origins.LOGGER.warn("No Origins keybindings found - SkillKeybinds may not be initialized");
            Origins.LOGGER.info("Ensure SkillKeybinds implements ClientModInitializer and is registered in fabric.mod.json");
        }
        
        return repaired;
    }
    
    /**
     * Finds an alternative key that's not in use
     */
    private static int findAlternativeKey(int conflictingKey, List<KeyBinding> existingKeybindings) {
        Set<Integer> usedKeys = new HashSet<>();
        for (KeyBinding kb : existingKeybindings) {
            usedKeys.add(kb.getDefaultKey().getCode());
        }
        
        // Try some common alternative keys
        int[] alternatives = {
            GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P,
            GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD
        };
        
        for (int alt : alternatives) {
            if (!usedKeys.contains(alt)) {
                return alt;
            }
        }
        
        return GLFW.GLFW_KEY_UNKNOWN;
    }
    
    /**
     * Tests if keybinding system is working properly
     */
    public static boolean testKeybindingSystem() {
        Origins.LOGGER.info("Testing keybinding system...");
        
        try {
            // Test basic keybinding functionality
            if (client == null || client.options == null) {
                Origins.LOGGER.error("Client or options not available for keybinding test");
                return false;
            }
            
            // Check if we can access keybindings
            KeyBinding[] allKeys = client.options.allKeys;
            if (allKeys == null || allKeys.length == 0) {
                Origins.LOGGER.error("No keybindings found in client options");
                return false;
            }
            
            Origins.LOGGER.info("Keybinding system test passed - found " + allKeys.length + " keybindings");
            return true;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Keybinding system test failed: " + e.getMessage());
            return false;
        }
    }
}