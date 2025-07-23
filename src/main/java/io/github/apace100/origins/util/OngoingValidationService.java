package io.github.apace100.origins.util;

import io.github.apace100.origins.Origins;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ongoing validation service for continuous monitoring of Origins mod health
 * Performs periodic checks and maintenance tasks to ensure system stability
 */
public class OngoingValidationService {
    
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);
    private static final AtomicLong LAST_VALIDATION_TIME = new AtomicLong(0);
    private static final AtomicLong VALIDATION_COUNT = new AtomicLong(0);
    
    // Validation intervals (in seconds)
    private static final int STARTUP_VALIDATION_DELAY = 30; // 30 seconds after startup
    private static final int PERIODIC_VALIDATION_INTERVAL = 300; // 5 minutes
    private static final int TEXTURE_CHECK_INTERVAL = 600; // 10 minutes
    private static final int KEYBINDING_CHECK_INTERVAL = 900; // 15 minutes
    
    private static boolean initialized = false;
    private static boolean serverRunning = false;
    
    /**
     * Initializes the ongoing validation service
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        Origins.LOGGER.info("Initializing ongoing validation service...");
        
        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(OngoingValidationService::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(OngoingValidationService::onServerStopping);
        
        // Register tick events for periodic checks
        ServerTickEvents.END_SERVER_TICK.register(OngoingValidationService::onServerTick);
        
        // Schedule initial validation
        scheduleInitialValidation();
        
        // Schedule periodic validations
        schedulePeriodicValidations();
        
        initialized = true;
        Origins.LOGGER.info("Ongoing validation service initialized");
    }
    
    /**
     * Called when server starts
     */
    private static void onServerStarted(MinecraftServer server) {
        serverRunning = true;
        Origins.LOGGER.info("Server started - enabling ongoing validation");
        
        // Log diagnostic event
        DiagnosticReporter.logEvent("VALIDATION", "INFO", "Server started - validation service active");
        
        // Schedule startup validation
        SCHEDULER.schedule(() -> {
            try {
                runStartupValidation();
            } catch (Exception e) {
                Origins.LOGGER.error("Error during startup validation: " + e.getMessage(), e);
                DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Startup validation failed: " + e.getMessage());
            }
        }, STARTUP_VALIDATION_DELAY, TimeUnit.SECONDS);
    }
    
    /**
     * Called when server is stopping
     */
    private static void onServerStopping(MinecraftServer server) {
        serverRunning = false;
        Origins.LOGGER.info("Server stopping - disabling ongoing validation");
        
        // Log diagnostic event
        DiagnosticReporter.logEvent("VALIDATION", "INFO", "Server stopping - validation service disabled");
        
        // Run final validation
        try {
            runShutdownValidation();
        } catch (Exception e) {
            Origins.LOGGER.error("Error during shutdown validation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Called on each server tick for lightweight checks
     */
    private static void onServerTick(MinecraftServer server) {
        // Perform very lightweight checks every tick
        // This is called 20 times per second, so keep it minimal
        
        long currentTime = System.currentTimeMillis();
        long lastValidation = LAST_VALIDATION_TIME.get();
        
        // Check if it's been too long since last validation (emergency check)
        if (currentTime - lastValidation > 1800000) { // 30 minutes
            Origins.LOGGER.warn("Emergency validation triggered - too long since last check");
            SCHEDULER.submit(() -> {
                try {
                    runEmergencyValidation();
                } catch (Exception e) {
                    Origins.LOGGER.error("Emergency validation failed: " + e.getMessage(), e);
                }
            });
        }
    }
    
    /**
     * Schedules initial validation after mod loading
     */
    private static void scheduleInitialValidation() {
        SCHEDULER.schedule(() -> {
            try {
                runInitialValidation();
            } catch (Exception e) {
                Origins.LOGGER.error("Error during initial validation: " + e.getMessage(), e);
                DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Initial validation failed: " + e.getMessage());
            }
        }, 10, TimeUnit.SECONDS); // 10 seconds after mod loading
    }
    
    /**
     * Schedules periodic validation tasks
     */
    private static void schedulePeriodicValidations() {
        // Main periodic validation
        SCHEDULER.scheduleAtFixedRate(() -> {
            if (serverRunning) {
                try {
                    runPeriodicValidation();
                } catch (Exception e) {
                    Origins.LOGGER.error("Error during periodic validation: " + e.getMessage(), e);
                    DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Periodic validation failed: " + e.getMessage());
                }
            }
        }, PERIODIC_VALIDATION_INTERVAL, PERIODIC_VALIDATION_INTERVAL, TimeUnit.SECONDS);
        
        // Texture validation
        SCHEDULER.scheduleAtFixedRate(() -> {
            if (serverRunning) {
                try {
                    runTextureValidation();
                } catch (Exception e) {
                    Origins.LOGGER.error("Error during texture validation: " + e.getMessage(), e);
                    DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Texture validation failed: " + e.getMessage());
                }
            }
        }, TEXTURE_CHECK_INTERVAL, TEXTURE_CHECK_INTERVAL, TimeUnit.SECONDS);
        
        // Keybinding validation
        SCHEDULER.scheduleAtFixedRate(() -> {
            if (serverRunning) {
                try {
                    runKeybindingValidation();
                } catch (Exception e) {
                    Origins.LOGGER.error("Error during keybinding validation: " + e.getMessage(), e);
                    DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Keybinding validation failed: " + e.getMessage());
                }
            }
        }, KEYBINDING_CHECK_INTERVAL, KEYBINDING_CHECK_INTERVAL, TimeUnit.SECONDS);
    }
    
    /**
     * Runs initial validation after mod loading
     */
    private static void runInitialValidation() {
        Origins.LOGGER.info("Running initial validation...");
        LAST_VALIDATION_TIME.set(System.currentTimeMillis());
        VALIDATION_COUNT.incrementAndGet();
        
        // Log system information
        DiagnosticReporter.logEvent("VALIDATION", "INFO", "Initial validation started");
        
        // Validate critical systems
        boolean textureHealth = TextureTest.validateTextureHealth();
        boolean keybindingHealth = KeybindingTest.validateKeybindingHealth();
        boolean compatibilityHealth = ModCompatibilityChecker.isEnvironmentSuitable();
        
        // Log results
        DiagnosticReporter.logEvent("VALIDATION", "INFO", 
            String.format("Initial validation completed - Textures: %s, Keybindings: %s, Compatibility: %s",
                textureHealth ? "OK" : "ISSUES", 
                keybindingHealth ? "OK" : "ISSUES",
                compatibilityHealth ? "OK" : "ISSUES"));
        
        if (!textureHealth || !keybindingHealth || !compatibilityHealth) {
            Origins.LOGGER.warn("Initial validation detected issues - check diagnostic logs");
        } else {
            Origins.LOGGER.info("Initial validation completed successfully");
        }
    }
    
    /**
     * Runs startup validation after server starts
     */
    private static void runStartupValidation() {
        Origins.LOGGER.info("Running startup validation...");
        LAST_VALIDATION_TIME.set(System.currentTimeMillis());
        VALIDATION_COUNT.incrementAndGet();
        
        DiagnosticReporter.logEvent("VALIDATION", "INFO", "Startup validation started");
        
        // Run comprehensive checks
        try {
            // Test texture initialization
            TextureTest.testTextureInitialization();
            
            // Test keybinding conflicts
            KeybindingTest.testConflictDetection();
            
            // Generate diagnostic report
            DiagnosticReporter.DiagnosticReport report = DiagnosticReporter.generateReport();
            
            if (report.hasCriticalIssues()) {
                Origins.LOGGER.error("Startup validation detected critical issues!");
                DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Critical issues detected during startup");
            } else if (report.hasIssues()) {
                Origins.LOGGER.warn("Startup validation detected minor issues");
                DiagnosticReporter.logEvent("VALIDATION", "WARN", "Minor issues detected during startup");
            } else {
                Origins.LOGGER.info("Startup validation completed without issues");
                DiagnosticReporter.logEvent("VALIDATION", "INFO", "Startup validation successful");
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Startup validation failed: " + e.getMessage(), e);
            DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Startup validation exception: " + e.getMessage());
        }
    }
    
    /**
     * Runs periodic validation checks
     */
    private static void runPeriodicValidation() {
        Origins.LOGGER.debug("Running periodic validation...");
        LAST_VALIDATION_TIME.set(System.currentTimeMillis());
        VALIDATION_COUNT.incrementAndGet();
        
        // Lightweight health checks
        boolean systemHealthy = true;
        
        try {
            // Check texture system
            if (!TextureTest.validateTextureHealth()) {
                Origins.LOGGER.warn("Periodic validation: Texture system issues detected");
                systemHealthy = false;
            }
            
            // Check keybinding system
            if (!KeybindingTest.validateKeybindingHealth()) {
                Origins.LOGGER.warn("Periodic validation: Keybinding system issues detected");
                systemHealthy = false;
            }
            
            // Check mod compatibility
            if (!ModCompatibilityChecker.isEnvironmentSuitable()) {
                Origins.LOGGER.warn("Periodic validation: Mod compatibility issues detected");
                systemHealthy = false;
            }
            
            if (systemHealthy) {
                Origins.LOGGER.debug("Periodic validation: All systems healthy");
                DiagnosticReporter.logEvent("VALIDATION", "DEBUG", "Periodic validation successful");
            } else {
                Origins.LOGGER.warn("Periodic validation: Issues detected");
                DiagnosticReporter.logEvent("VALIDATION", "WARN", "Periodic validation detected issues");
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Periodic validation failed: " + e.getMessage(), e);
            DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Periodic validation exception: " + e.getMessage());
        }
    }
    
    /**
     * Runs texture-specific validation
     */
    private static void runTextureValidation() {
        Origins.LOGGER.debug("Running texture validation...");
        
        try {
            TextureValidator.ValidationReport report = TextureValidator.validateAllTextures();
            
            if (report.hasIssues()) {
                Origins.LOGGER.warn("Texture validation: {} invalid textures detected", 
                                  report.invalidTextures.size());
                DiagnosticReporter.logEvent("VALIDATION", "WARN", 
                    "Texture validation found " + report.invalidTextures.size() + " invalid textures");
                
                // Attempt to generate missing textures
                TextureValidator.generateMissingTextures();
            } else {
                Origins.LOGGER.debug("Texture validation: All textures valid");
                DiagnosticReporter.logEvent("VALIDATION", "DEBUG", "Texture validation successful");
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Texture validation failed: " + e.getMessage(), e);
            DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Texture validation exception: " + e.getMessage());
        }
    }
    
    /**
     * Runs keybinding-specific validation
     */
    private static void runKeybindingValidation() {
        Origins.LOGGER.debug("Running keybinding validation...");
        
        try {
            KeybindingDiagnostic.DiagnosticReport report = KeybindingDiagnostic.runDiagnostic();
            
            if (report.hasIssues()) {
                Origins.LOGGER.warn("Keybinding validation: {} conflicts, {} issues detected", 
                                  report.conflicts.size(), report.issues.size());
                DiagnosticReporter.logEvent("VALIDATION", "WARN", 
                    String.format("Keybinding validation found %d conflicts and %d issues", 
                        report.conflicts.size(), report.issues.size()));
                
                // Attempt to repair issues
                KeybindingDiagnostic.repairKeybindings(report);
            } else {
                Origins.LOGGER.debug("Keybinding validation: All keybindings healthy");
                DiagnosticReporter.logEvent("VALIDATION", "DEBUG", "Keybinding validation successful");
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Keybinding validation failed: " + e.getMessage(), e);
            DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Keybinding validation exception: " + e.getMessage());
        }
    }
    
    /**
     * Runs emergency validation when issues are suspected
     */
    private static void runEmergencyValidation() {
        Origins.LOGGER.warn("Running emergency validation...");
        LAST_VALIDATION_TIME.set(System.currentTimeMillis());
        
        DiagnosticReporter.logEvent("VALIDATION", "WARN", "Emergency validation triggered");
        
        try {
            // Run all validation checks immediately
            runPeriodicValidation();
            runTextureValidation();
            runKeybindingValidation();
            
            // Generate emergency diagnostic report
            DiagnosticReporter.DiagnosticReport report = DiagnosticReporter.generateReport();
            DiagnosticReporter.saveReportToFile(report);
            
            Origins.LOGGER.warn("Emergency validation completed - diagnostic report generated");
            DiagnosticReporter.logEvent("VALIDATION", "WARN", "Emergency validation completed");
            
        } catch (Exception e) {
            Origins.LOGGER.error("Emergency validation failed: " + e.getMessage(), e);
            DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Emergency validation exception: " + e.getMessage());
        }
    }
    
    /**
     * Runs shutdown validation before server stops
     */
    private static void runShutdownValidation() {
        Origins.LOGGER.info("Running shutdown validation...");
        
        DiagnosticReporter.logEvent("VALIDATION", "INFO", "Shutdown validation started");
        
        try {
            // Generate final diagnostic report
            DiagnosticReporter.DiagnosticReport report = DiagnosticReporter.generateReport();
            DiagnosticReporter.saveReportToFile(report);
            DiagnosticReporter.saveReportAsText(report);
            
            // Log validation statistics
            Origins.LOGGER.info("Validation statistics: {} validations performed", VALIDATION_COUNT.get());
            DiagnosticReporter.logEvent("VALIDATION", "INFO", 
                "Shutdown validation completed - " + VALIDATION_COUNT.get() + " total validations performed");
            
        } catch (Exception e) {
            Origins.LOGGER.error("Shutdown validation failed: " + e.getMessage(), e);
            DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Shutdown validation exception: " + e.getMessage());
        }
    }
    
    /**
     * Gets validation service status
     */
    public static String getValidationStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Ongoing Validation Service Status:\n");
        status.append("- Initialized: ").append(initialized).append("\n");
        status.append("- Server Running: ").append(serverRunning).append("\n");
        status.append("- Total Validations: ").append(VALIDATION_COUNT.get()).append("\n");
        status.append("- Last Validation: ");
        
        long lastValidation = LAST_VALIDATION_TIME.get();
        if (lastValidation > 0) {
            long timeSince = System.currentTimeMillis() - lastValidation;
            status.append(timeSince / 1000).append(" seconds ago\n");
        } else {
            status.append("Never\n");
        }
        
        return status.toString();
    }
    
    /**
     * Forces an immediate validation check
     */
    public static void forceValidation() {
        Origins.LOGGER.info("Forcing immediate validation...");
        
        SCHEDULER.submit(() -> {
            try {
                runPeriodicValidation();
                runTextureValidation();
                runKeybindingValidation();
                
                Origins.LOGGER.info("Forced validation completed");
                DiagnosticReporter.logEvent("VALIDATION", "INFO", "Forced validation completed");
                
            } catch (Exception e) {
                Origins.LOGGER.error("Forced validation failed: " + e.getMessage(), e);
                DiagnosticReporter.logEvent("VALIDATION", "ERROR", "Forced validation exception: " + e.getMessage());
            }
        });
    }
    
    /**
     * Shuts down the validation service
     */
    public static void shutdown() {
        Origins.LOGGER.info("Shutting down ongoing validation service...");
        
        try {
            SCHEDULER.shutdown();
            if (!SCHEDULER.awaitTermination(10, TimeUnit.SECONDS)) {
                SCHEDULER.shutdownNow();
            }
            Origins.LOGGER.info("Ongoing validation service shut down successfully");
        } catch (InterruptedException e) {
            SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
            Origins.LOGGER.warn("Validation service shutdown interrupted");
        }
    }
}