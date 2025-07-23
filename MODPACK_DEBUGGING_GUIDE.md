# Origins Modpack Debugging Guide

## Overview

This guide provides comprehensive solutions for debugging and fixing common issues in Minecraft 1.20.1 Fabric modpacks using the Origins mod. The solutions address three critical problems:

1. **JSON Parsing EOFException Errors** - Empty or corrupted JSON files causing crashes
2. **Non-functional Global Skill Keybinding (G key)** - Keybinding system issues
3. **Texture Loading Failures** - Missing or corrupted texture files

## Quick Fix Summary

### 1. JSON Issues
- **Problem**: `java.io.EOFException: End of input at line 1 column 1 path $`
- **Solution**: Run `/origins diagnostic json` to scan and repair JSON files
- **Prevention**: Enable debug mode in config for automatic startup validation

### 2. Keybinding Issues  
- **Problem**: G key (global skill) not responding
- **Solution**: Ensure `SkillKeybinds` is registered in `fabric.mod.json` client entrypoints
- **Testing**: Use `/origins diagnostic keybindings` to check registration

### 3. Texture Issues
- **Problem**: `Failed to load texture: origins:textures/gui/resource_bar2.png`
- **Solution**: Copy `resource_bar.png` to `resource_bar2.png` or run texture validation
- **Testing**: Use `/origins diagnostic textures` to validate all textures

## Detailed Solutions

### JSON Parsing Errors

#### Symptoms
```
Caused by: java.io.EOFException: End of input at line 1 column 1 path $
```

#### Root Causes
- Empty JSON files
- Truncated JSON files (incomplete downloads/saves)
- Syntax errors (trailing commas, unquoted keys)
- Encoding issues (BOM, special characters)

#### Diagnostic Commands
```
/origins diagnostic json          # Scan and repair JSON files
/origins diagnostic test json     # Run comprehensive JSON tests
/origins diagnostic test create   # Create test files for manual testing
```

#### Manual Fixes

1. **Empty Files**: Delete or add default content
2. **Syntax Errors**: Remove trailing commas, quote keys properly
3. **Truncated Files**: Restore from backup or regenerate
4. **Encoding Issues**: Save as UTF-8 without BOM

#### Automatic Repair
The system includes automatic repair for:
- Empty files → Generate appropriate defaults
- Trailing commas → Remove automatically  
- Unquoted keys → Add quotes
- Truncated JSON → Complete missing brackets/braces
- Encoding issues → Fix common character problems

### Keybinding System Issues

#### Symptoms
- G key press does nothing
- No visual/audio feedback
- Keybinding conflicts with other mods

#### Root Causes
- `SkillKeybinds` not registered in `fabric.mod.json`
- Network packet handlers not registered
- Keybinding conflicts
- Client/server synchronization issues

#### Diagnostic Commands
```
/origins diagnostic keybindings        # Check keybinding status
/origins diagnostic test keybindings   # Run keybinding tests
```

#### Required Configuration

Ensure `fabric.mod.json` includes:
```json
{
  "entrypoints": {
    "client": [
      "io.github.apace100.origins.OriginsClient",
      "io.github.apace100.origins.client.SkillKeybinds"
    ]
  }
}
```

#### Network Packet Registration

Required packets in `ModPackets.java`:
```java
public static final Identifier ACTIVATE_GLOBAL_SKILL = new Identifier(Origins.MODID, "activate_global_skill");
public static final Identifier ACTIVATE_ACTIVE_SKILL = new Identifier(Origins.MODID, "activate_active_skill");
```

#### Keybinding Mappings
- **G Key**: Global skill activation (class-specific ability)
- **K Key**: Active skill activation (selected skill)
- **L Key**: Skill selection GUI

### Texture Loading Issues

#### Symptoms
```
[Render thread/WARN] (Minecraft) Failed to load texture: origins:textures/gui/resource_bar2.png
```

#### Root Causes
- Missing texture files
- Incorrect file paths
- Corrupted texture files
- Resource pack conflicts

#### Diagnostic Commands
```
/origins diagnostic textures        # Validate all textures
/origins diagnostic test textures   # Run texture tests
```

#### Required Textures

Critical Origins textures:
- `textures/gui/resource_bar2.png` - HUD overlay bars
- `textures/gui/resource_bar.png` - Alternative HUD bars
- `textures/gui/choose_origin.png` - Origin selection screen
- `textures/gui/icons.png` - UI icons
- `textures/gui/inventory_overlay.png` - Inventory overlay

#### Texture Generation

Missing textures can be generated automatically:
```
/origins diagnostic textures
```

Or manually copy existing textures:
```bash
copy "src\main\resources\assets\origins\textures\gui\resource_bar.png" "src\main\resources\assets\origins\textures\gui\resource_bar2.png"
```

## Comprehensive Diagnostic System

### Available Commands

#### Basic Diagnostics
```
/origins diagnostic json          # JSON file validation
/origins diagnostic textures      # Texture validation  
/origins diagnostic keybindings   # Keybinding validation
/origins diagnostic all           # Run all diagnostics
```

#### Advanced Diagnostics
```
/origins diagnostic report        # Generate full diagnostic report
/origins diagnostic quick         # Quick system health check
```

#### Ongoing Validation
```
/origins diagnostic validation status  # Check validation service status
/origins diagnostic validation force   # Force immediate validation
```

#### Testing System
```
/origins diagnostic test json          # Test JSON repair system
/origins diagnostic test keybindings   # Test keybinding system
/origins diagnostic test textures      # Test texture system
/origins diagnostic test create        # Create test files
```

### Automatic Validation

The system includes ongoing validation that:
- Runs initial checks 30 seconds after startup
- Performs periodic health checks every 5 minutes
- Validates textures every 10 minutes
- Checks keybindings every 15 minutes
- Generates emergency reports when issues detected

### Configuration Options

Enable debug mode in Origins config:
```json
{
  "debugMode": true,
  "showHudOverlay": true
}
```

## Troubleshooting Workflow

### Step 1: Quick Health Check
```
/origins diagnostic quick
```

### Step 2: Identify Problem Area
- JSON issues → `/origins diagnostic json`
- Texture problems → `/origins diagnostic textures`  
- Keybinding issues → `/origins diagnostic keybindings`

### Step 3: Run Targeted Tests
```
/origins diagnostic test [json|textures|keybindings]
```

### Step 4: Generate Full Report
```
/origins diagnostic report
```

### Step 5: Apply Fixes
- Follow specific solutions above
- Re-run diagnostics to verify fixes
- Check logs for detailed information

## Prevention Strategies

### 1. Regular Validation
- Enable ongoing validation service
- Run periodic health checks
- Monitor diagnostic logs

### 2. Backup Strategy
- Automatic backups created before repairs
- Located in `json_backups/` directories
- Timestamped for easy identification

### 3. Configuration Management
- Validate JSON files before deployment
- Use proper encoding (UTF-8)
- Avoid manual JSON editing when possible

### 4. Mod Compatibility
- Check compatibility before adding new mods
- Monitor for known incompatibilities
- Update mods regularly

## Common Error Patterns

### JSON Errors
```
JsonSyntaxException: Expected name at line X column Y
→ Fix: Add quotes around object keys

EOFException: End of input
→ Fix: Complete truncated JSON or regenerate

Malformed JSON
→ Fix: Remove trailing commas, fix syntax
```

### Keybinding Errors
```
Key not registered
→ Fix: Add to fabric.mod.json client entrypoints

No response to key press
→ Fix: Check network packet registration

Keybinding conflicts
→ Fix: Resolve conflicts or reassign keys
```

### Texture Errors
```
Failed to load texture
→ Fix: Verify file exists and path is correct

Resource not found
→ Fix: Generate missing textures or use fallbacks
```

## Advanced Configuration

### Custom Validation Intervals

Modify `OngoingValidationService` constants:
```java
private static final int PERIODIC_VALIDATION_INTERVAL = 300; // 5 minutes
private static final int TEXTURE_CHECK_INTERVAL = 600;       // 10 minutes
private static final int KEYBINDING_CHECK_INTERVAL = 900;    // 15 minutes
```

### Custom Fallback Textures

Add to `TextureValidator` fallback map:
```java
FALLBACK_TEXTURES.put(
    new Identifier(Origins.MODID, "textures/gui/custom.png"),
    new Identifier(Origins.MODID, "textures/gui/fallback.png")
);
```

### Custom JSON Templates

Add to `JsonRepairService` generation methods:
```java
private static String generateCustomJson() {
    // Custom JSON template
    return "{}";
}
```

## Support and Maintenance

### Log Locations
- Main logs: `logs/latest.log`
- Diagnostic reports: `origins_diagnostics/`
- JSON backups: `json_backups/`

### Performance Impact
- Validation runs in background threads
- Minimal impact on gameplay
- Can be disabled if needed

### Updates and Compatibility
- System designed for Origins 1.10.0+
- Compatible with Minecraft 1.20.1
- Fabric Loader 0.14.0+ required

## Conclusion

This comprehensive debugging system provides:
- **Automatic detection** of common issues
- **Intelligent repair** of corrupted files
- **Ongoing monitoring** for system health
- **Detailed reporting** for troubleshooting
- **Prevention strategies** for future issues

For additional support, check the diagnostic logs and generated reports for detailed information about any issues detected.