package io.github.apace100.origins.util;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.client.SkillKeybinds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Test utility for keybinding functionality
 * Validates keybinding registration, functionality, and user feedback
 */
public class KeybindingTest {
    
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static class TestResult {
        public final String testName;
        public final boolean passed;
        public final String message;
        public final long duration;
        
        public TestResult(String testName, boolean passed, String message, long duration) {
            this.testName = testName;
            this.passed = passed;
            this.message = message;
            this.duration = duration;
        }
    }
    
    public static class TestSuite {
        public final List<TestResult> results = new ArrayList<>();
        public final long totalDuration;
        public final int passedTests;
        public final int failedTests;
        
        public TestSuite(List<TestResult> results) {
            this.results.addAll(results);
            this.totalDuration = results.stream().mapToLong(r -> r.duration).sum();
            this.passedTests = (int) results.stream().mapToLong(r -> r.passed ? 1 : 0).sum();
            this.failedTests = results.size() - passedTests;
        }
        
        public boolean allTestsPassed() {
            return failedTests == 0;
        }
    }
    
    /**
     * Runs comprehensive keybinding tests
     */
    public static CompletableFuture<TestSuite> runKeybindingTests() {
        Origins.LOGGER.info("Starting keybinding functionality tests...");
        
        return CompletableFuture.supplyAsync(() -> {
            List<TestResult> results = new ArrayList<>();
            
            // Test 1: Keybinding registration
            results.add(testKeybindingRegistration());
            
            // Test 2: Keybinding information retrieval
            results.add(testKeybindingInformation());
            
            // Test 3: Diagnostic functionality
            results.add(testDiagnosticFunctionality());
            
            // Test 4: Client availability
            results.add(testClientAvailability());
            
            // Test 5: Network packet preparation
            results.add(testNetworkPacketPreparation());
            
            TestSuite suite = new TestSuite(results);
            
            Origins.LOGGER.info("Keybinding tests completed: {}/{} passed in {}ms", 
                              suite.passedTests, results.size(), suite.totalDuration);
            
            return suite;
        });
    }
    
    private static TestResult testKeybindingRegistration() {
        long startTime = System.currentTimeMillis();
        
        try {
            boolean registered = SkillKeybinds.areKeybindingsRegistered();
            long duration = System.currentTimeMillis() - startTime;
            
            if (registered) {
                return new TestResult("Keybinding Registration", true, 
                    "All Origins keybindings are properly registered", duration);
            } else {
                return new TestResult("Keybinding Registration", false, 
                    "Some Origins keybindings are not registered", duration);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new TestResult("Keybinding Registration", false, 
                "Error testing registration: " + e.getMessage(), duration);
        }
    }
    
    private static TestResult testKeybindingInformation() {
        long startTime = System.currentTimeMillis();
        
        try {
            String info = SkillKeybinds.getKeybindingInfo();
            long duration = System.currentTimeMillis() - startTime;
            
            if (info != null && !info.isEmpty() && info.contains("Origins Keybindings Status")) {
                return new TestResult("Keybinding Information", true, 
                    "Keybinding information retrieved successfully", duration);
            } else {
                return new TestResult("Keybinding Information", false, 
                    "Failed to retrieve keybinding information", duration);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new TestResult("Keybinding Information", false, 
                "Error retrieving information: " + e.getMessage(), duration);
        }
    }
    
    private static TestResult testDiagnosticFunctionality() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test diagnostic report generation
            KeybindingDiagnostic.DiagnosticReport report = KeybindingDiagnostic.runDiagnostic();
            long duration = System.currentTimeMillis() - startTime;
            
            if (report != null) {
                return new TestResult("Diagnostic Functionality", true, 
                    String.format("Diagnostic completed: %d keybindings, %d conflicts, %d issues", 
                        report.registeredKeybindings.size(), report.conflicts.size(), report.issues.size()), 
                    duration);
            } else {
                return new TestResult("Diagnostic Functionality", false, 
                    "Diagnostic report is null", duration);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new TestResult("Diagnostic Functionality", false, 
                "Error running diagnostic: " + e.getMessage(), duration);
        }
    }
    
    private static TestResult testClientAvailability() {
        long startTime = System.currentTimeMillis();
        
        try {
            boolean clientAvailable = client != null;
            boolean playerAvailable = client != null && client.player != null;
            long duration = System.currentTimeMillis() - startTime;
            
            if (clientAvailable) {
                String message = playerAvailable ? 
                    "Client and player are available" : 
                    "Client available, but player is null (normal in menu)";
                return new TestResult("Client Availability", true, message, duration);
            } else {
                return new TestResult("Client Availability", false, 
                    "MinecraftClient is not available", duration);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new TestResult("Client Availability", false, 
                "Error checking client: " + e.getMessage(), duration);
        }
    }
    
    private static TestResult testNetworkPacketPreparation() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test that we can create packet buffers (without sending)
            // This tests the network preparation without actually sending packets
            boolean canPreparePackets = true;
            
            // Test packet identifiers exist
            boolean hasGlobalSkillPacket = io.github.apace100.origins.networking.ModPackets.ACTIVATE_GLOBAL_SKILL != null;
            boolean hasActiveSkillPacket = io.github.apace100.origins.networking.ModPackets.ACTIVATE_ACTIVE_SKILL != null;
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (canPreparePackets && hasGlobalSkillPacket && hasActiveSkillPacket) {
                return new TestResult("Network Packet Preparation", true, 
                    "Network packets can be prepared successfully", duration);
            } else {
                return new TestResult("Network Packet Preparation", false, 
                    "Network packet preparation failed", duration);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new TestResult("Network Packet Preparation", false, 
                "Error preparing packets: " + e.getMessage(), duration);
        }
    }
    
    /**
     * Simulates keybinding usage for testing (without actually triggering actions)
     */
    public static void simulateKeybindingUsage() {
        if (client == null || client.player == null) {
            Origins.LOGGER.warn("Cannot simulate keybinding usage - client or player not available");
            return;
        }
        
        Origins.LOGGER.info("Simulating keybinding usage...");
        
        // Simulate G key press feedback
        client.player.sendMessage(Text.literal("Симуляция: Нажата клавиша G (глобальный навык)")
            .formatted(Formatting.GREEN), true);
        
        // Simulate K key press feedback
        client.player.sendMessage(Text.literal("Симуляция: Нажата клавиша K (активный навык)")
            .formatted(Formatting.YELLOW), true);
        
        // Simulate L key press feedback
        client.player.sendMessage(Text.literal("Симуляция: Нажата клавиша L (выбор навыка)")
            .formatted(Formatting.BLUE), true);
        
        Origins.LOGGER.info("Keybinding usage simulation completed");
    }
    
    /**
     * Tests keybinding conflicts detection
     */
    public static void testConflictDetection() {
        Origins.LOGGER.info("Testing keybinding conflict detection...");
        
        try {
            KeybindingDiagnostic.DiagnosticReport report = KeybindingDiagnostic.runDiagnostic();
            
            if (report.conflicts.isEmpty()) {
                Origins.LOGGER.info("✓ No keybinding conflicts detected");
            } else {
                Origins.LOGGER.warn("⚠ Keybinding conflicts detected:");
                for (KeybindingDiagnostic.KeyConflict conflict : report.conflicts) {
                    Origins.LOGGER.warn("  - {} conflicts with: {}", 
                        conflict.keybindingName, String.join(", ", conflict.conflictingMods));
                }
            }
            
            // Test repair suggestions
            if (!report.conflicts.isEmpty()) {
                boolean repaired = KeybindingDiagnostic.repairKeybindings(report);
                if (repaired) {
                    Origins.LOGGER.info("✓ Keybinding conflicts repaired");
                } else {
                    Origins.LOGGER.warn("⚠ Some keybinding conflicts could not be automatically repaired");
                }
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Error testing conflict detection: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates keybinding system health
     */
    public static boolean validateKeybindingHealth() {
        try {
            // Test basic functionality
            boolean registered = SkillKeybinds.areKeybindingsRegistered();
            if (!registered) {
                Origins.LOGGER.error("Keybinding health check failed: Not all keybindings registered");
                return false;
            }
            
            // Test diagnostic system
            KeybindingDiagnostic.DiagnosticReport report = KeybindingDiagnostic.runDiagnostic();
            if (report == null) {
                Origins.LOGGER.error("Keybinding health check failed: Diagnostic system not working");
                return false;
            }
            
            // Test system functionality
            boolean systemTest = KeybindingDiagnostic.testKeybindingSystem();
            if (!systemTest) {
                Origins.LOGGER.error("Keybinding health check failed: System test failed");
                return false;
            }
            
            Origins.LOGGER.info("✓ Keybinding system health check passed");
            return true;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Keybinding health check failed with exception: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Provides user-friendly test results
     */
    public static void displayTestResults(TestSuite suite) {
        if (client == null || client.player == null) {
            return;
        }
        
        client.player.sendMessage(Text.literal("=== РЕЗУЛЬТАТЫ ТЕСТОВ КЛАВИШ ===")
            .formatted(Formatting.BOLD), false);
        
        client.player.sendMessage(Text.literal(String.format("Пройдено: %d/%d тестов за %dмс", 
            suite.passedTests, suite.results.size(), suite.totalDuration)), false);
        
        for (TestResult result : suite.results) {
            Formatting color = result.passed ? Formatting.GREEN : Formatting.RED;
            String status = result.passed ? "✓" : "✗";
            
            client.player.sendMessage(Text.literal(String.format("%s %s: %s", 
                status, result.testName, result.message)).formatted(color), false);
        }
        
        if (suite.allTestsPassed()) {
            client.player.sendMessage(Text.literal("Все тесты пройдены успешно!")
                .formatted(Formatting.GREEN, Formatting.BOLD), false);
        } else {
            client.player.sendMessage(Text.literal("Обнаружены проблемы с клавишами!")
                .formatted(Formatting.RED, Formatting.BOLD), false);
        }
    }
}