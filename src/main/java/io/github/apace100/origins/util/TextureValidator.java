package io.github.apace100.origins.util;

import io.github.apace100.origins.Origins;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive texture validation and fallback system
 * Validates texture assets and provides fallback mechanisms for missing textures
 */
public class TextureValidator {
    
    private static final Map<Identifier, Boolean> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, Identifier> FALLBACK_TEXTURES = new HashMap<>();
    private static boolean initialized = false;
    
    // Common fallback textures
    static {
        // GUI fallbacks
        FALLBACK_TEXTURES.put(
            new Identifier(Origins.MODID, "textures/gui/resource_bar2.png"),
            new Identifier(Origins.MODID, "textures/gui/resource_bar.png")
        );
        FALLBACK_TEXTURES.put(
            new Identifier(Origins.MODID, "textures/gui/skill_background.png"),
            new Identifier("minecraft", "textures/gui/options_background.png")
        );
        FALLBACK_TEXTURES.put(
            new Identifier(Origins.MODID, "textures/gui/choose_origin.png"),
            new Identifier("minecraft", "textures/gui/demo_background.png")
        );
    }
    
    /**
     * Validates that a texture exists and can be loaded
     */
    public static boolean validateTexture(Identifier textureId) {
        if (textureId == null) {
            return false;
        }
        
        // Check cache first
        Boolean cached = TEXTURE_CACHE.get(textureId);
        if (cached != null) {
            return cached;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }
        
        ResourceManager resourceManager = client.getResourceManager();
        if (resourceManager == null) {
            return false;
        }
        
        try {
            Optional<Resource> resource = resourceManager.getResource(textureId);
            boolean exists = resource.isPresent();
            
            if (exists) {
                // Additional validation - try to read the resource
                try (var inputStream = resource.get().getInputStream()) {
                    // Just check if we can open the stream
                    inputStream.read();
                    TEXTURE_CACHE.put(textureId, true);
                                        return true;
                }
            }
            
            TEXTURE_CACHE.put(textureId, false);
            Origins.LOGGER.warn("Texture not found or invalid: " + textureId);
            return false;
            
        } catch (IOException e) {
            TEXTURE_CACHE.put(textureId, false);
            Origins.LOGGER.error("Error validating texture " + textureId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets a valid texture identifier, using fallback if necessary
     */
    public static Identifier getValidTexture(Identifier preferredTexture) {
        if (validateTexture(preferredTexture)) {
            return preferredTexture;
        }
        
        // Try fallback texture
        Identifier fallback = FALLBACK_TEXTURES.get(preferredTexture);
        if (fallback != null && validateTexture(fallback)) {
                        return fallback;
        }
        
        // Try generic Minecraft fallbacks
        Identifier genericFallback = getGenericFallback(preferredTexture);
        if (genericFallback != null && validateTexture(genericFallback)) {
                        return genericFallback;
        }
        
        Origins.LOGGER.error("No valid texture found for " + preferredTexture);
        return preferredTexture; // Return original and let Minecraft handle the error
    }
    
    /**
     * Gets a generic Minecraft fallback texture based on texture type
     */
    private static Identifier getGenericFallback(Identifier textureId) {
        String path = textureId.getPath().toLowerCase();
        
        if (path.contains("gui")) {
            if (path.contains("button") || path.contains("widget")) {
                return new Identifier("minecraft", "textures/gui/widgets.png");
            } else if (path.contains("background")) {
                return new Identifier("minecraft", "textures/gui/options_background.png");
            } else {
                return new Identifier("minecraft", "textures/gui/icons.png");
            }
        } else if (path.contains("item")) {
            return new Identifier("minecraft", "textures/item/barrier.png");
        } else if (path.contains("block")) {
            return new Identifier("minecraft", "textures/block/stone.png");
        }
        
        return null;
    }
    
    /**
     * Validates all Origins mod textures
     */
    public static ValidationReport validateAllTextures() {
                
        ValidationReport report = new ValidationReport();
        
        // Define all known Origins textures to validate
        List<Identifier> texturesToValidate = Arrays.asList(
            // GUI textures
            new Identifier(Origins.MODID, "textures/gui/resource_bar.png"),
            new Identifier(Origins.MODID, "textures/gui/resource_bar2.png"),
            new Identifier(Origins.MODID, "textures/gui/choose_origin.png"),
            new Identifier(Origins.MODID, "textures/gui/energy.png"),
            new Identifier(Origins.MODID, "textures/gui/exp.png"),
            new Identifier(Origins.MODID, "textures/gui/extra_inventory.png"),
            new Identifier(Origins.MODID, "textures/gui/icons.png"),
            new Identifier(Origins.MODID, "textures/gui/inventory_overlay.png"),
            new Identifier(Origins.MODID, "textures/gui/levelz_bg.png"),
            new Identifier(Origins.MODID, "textures/gui/skill_background.png"),
            
            // Item textures
            new Identifier(Origins.MODID, "textures/item/orb_of_origin.png")
        );
        
        for (Identifier texture : texturesToValidate) {
            boolean isValid = validateTexture(texture);
            if (isValid) {
                report.validTextures.add(texture);
            } else {
                report.invalidTextures.add(texture);
                
                // Check if fallback is available
                Identifier fallback = getValidTexture(texture);
                if (!fallback.equals(texture)) {
                    report.fallbacksUsed.put(texture, fallback);
                }
            }
        }
        
                                        
        if (!report.invalidTextures.isEmpty()) {
            Origins.LOGGER.warn("Invalid textures found:");
            for (Identifier invalid : report.invalidTextures) {
                Origins.LOGGER.warn("- " + invalid);
            }
        }
        
        return report;
    }
    
    /**
     * Clears the texture validation cache
     */
    public static void clearCache() {
        TEXTURE_CACHE.clear();
            }
    
    /**
     * Initializes the texture validation system
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        
                
        // Run initial validation
        ValidationReport report = validateAllTextures();
        
        // Log summary
        if (report.invalidTextures.isEmpty()) {
                    } else {
            Origins.LOGGER.warn("Found " + report.invalidTextures.size() + " invalid textures");
        }
        
        initialized = true;
    }
    
    /**
     * Report containing texture validation results
     */
    public static class ValidationReport {
        public final List<Identifier> validTextures = new ArrayList<>();
        public final List<Identifier> invalidTextures = new ArrayList<>();
        public final Map<Identifier, Identifier> fallbacksUsed = new HashMap<>();
        
        public boolean hasIssues() {
            return !invalidTextures.isEmpty();
        }
        
        public int getTotalTextures() {
            return validTextures.size() + invalidTextures.size();
        }
        
        public double getValidationRate() {
            int total = getTotalTextures();
            return total > 0 ? (double) validTextures.size() / total : 1.0;
        }
    }
    
    /**
     * Creates missing texture files by copying from existing ones
     */
    public static void generateMissingTextures() {
                
        ValidationReport report = validateAllTextures();
        int generated = 0;
        
        for (Identifier missingTexture : report.invalidTextures) {
            if (attemptTextureGeneration(missingTexture)) {
                generated++;
            }
        }
        
                
        // Clear cache and re-validate
        clearCache();
        validateAllTextures();
    }
    
    /**
     * Attempts to generate a missing texture by copying from similar textures
     */
    private static boolean attemptTextureGeneration(Identifier missingTexture) {
        // This would require file system operations and image processing
        // For now, just log the attempt
                
        // In a real implementation, this would:
        // 1. Find a similar existing texture
        // 2. Copy it to the missing location
        // 3. Optionally modify it (resize, recolor, etc.)
        
        return false; // Not implemented in this version
    }
}