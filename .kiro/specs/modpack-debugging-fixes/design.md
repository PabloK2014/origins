# Design Document

## Overview

This design addresses three critical issues in the Minecraft 1.20.1 Fabric modpack with Origins mod:
1. JSON parsing EOFException errors during modpack launch
2. Non-functional global skill keybinding (G key) in Origins mod
3. Missing texture file `resource_bar2.png` causing GUI rendering failures

The solution involves systematic debugging, file validation, keybinding verification, and texture asset management to ensure stable modpack operation.

## Architecture

### Component Structure
```
Modpack Debugging System
├── JSON Validation Module
│   ├── File Scanner
│   ├── JSON Parser Validator
│   └── Error Reporter
├── Keybinding Diagnostic Module
│   ├── Keybinding Registry Checker
│   ├── Conflict Detector
│   └── Network Handler Validator
└── Texture Asset Manager
    ├── Asset Validator
    ├── Missing File Detector
    └── Texture Generator/Copier
```

### Key Findings from Research

1. **JSON Issue**: The EOFException typically occurs when JSON files are empty, truncated, or corrupted. Common locations include mod configuration files, data files, and resource pack definitions.

2. **Keybinding Issue**: The `SkillKeybinds.java` class shows proper G key registration for global skills, but the functionality may fail due to:
   - Network packet handling issues
   - Server-side handler not registered
   - Keybinding conflicts with other mods

3. **Texture Issue**: The code references `resource_bar2.png` but only `resource_bar.png` exists in the assets. This is a missing asset issue.

## Components and Interfaces

### 1. JSON Validation Component

**Purpose**: Identify and fix malformed JSON files causing EOFException

**Interface**:
```java
public interface JsonValidator {
    ValidationResult validateJsonFile(Path filePath);
    List<Path> scanForJsonFiles(Path rootDirectory);
    void repairOrRegenerate(Path filePath, ValidationResult result);
}
```

**Implementation Strategy**:
- Scan all JSON files in mod directories, data packs, and configuration folders
- Use try-catch blocks to identify specific files causing parsing errors
- Implement backup and regeneration for corrupted files
- Validate JSON syntax and structure

### 2. Keybinding Diagnostic Component

**Purpose**: Diagnose and fix G key global skill functionality

**Interface**:
```java
public interface KeybindingDiagnostic {
    boolean verifyKeybindingRegistration();
    List<KeyConflict> detectConflicts();
    boolean testNetworkHandlers();
    void repairKeybindingSystem();
}
```

**Implementation Strategy**:
- Verify `SkillKeybinds` class initialization
- Check network packet registration for `ACTIVATE_GLOBAL_SKILL_PACKET`
- Test server-side handler existence
- Identify and resolve keybinding conflicts

### 3. Texture Asset Manager

**Purpose**: Fix missing texture files and ensure proper GUI rendering

**Interface**:
```java
public interface TextureAssetManager {
    boolean validateTexture(Identifier textureId);
    void generateMissingTextures();
    void copyFromExistingAssets();
}
```

**Implementation Strategy**:
- Create missing `resource_bar2.png` from existing `resource_bar.png`
- Validate all texture references in GUI classes
- Implement fallback texture loading

## Data Models

### ValidationResult
```java
public class ValidationResult {
    private boolean isValid;
    private String errorMessage;
    private ValidationError errorType;
    private List<String> suggestions;
}
```

### KeyConflict
```java
public class KeyConflict {
    private String modId;
    private String keybindingName;
    private int keyCode;
    private List<String> conflictingMods;
}
```

### TextureAsset
```java
public class TextureAsset {
    private Identifier identifier;
    private Path filePath;
    private boolean exists;
    private String expectedLocation;
}
```

## Error Handling

### JSON Parsing Errors
- **Detection**: Catch `EOFException` and `JsonSyntaxException`
- **Recovery**: Backup corrupted files and regenerate from defaults
- **Logging**: Detailed error messages with file paths and line numbers
- **Prevention**: Validate JSON files during mod loading

### Keybinding Failures
- **Detection**: Monitor keybinding registration and network packet sending
- **Recovery**: Re-register keybindings and network handlers
- **Logging**: Debug information about keybinding states and conflicts
- **Prevention**: Conflict detection during mod initialization

### Texture Loading Failures
- **Detection**: Catch texture loading exceptions in GUI rendering
- **Recovery**: Generate missing textures or use fallbacks
- **Logging**: Clear messages about missing texture files
- **Prevention**: Asset validation during resource pack loading

## Testing Strategy

### Unit Tests
1. **JSON Validator Tests**
   - Test with valid JSON files
   - Test with malformed JSON files
   - Test with empty files
   - Test file regeneration functionality

2. **Keybinding Tests**
   - Test keybinding registration
   - Test network packet creation and sending
   - Test conflict detection algorithms
   - Mock server-side handlers

3. **Texture Manager Tests**
   - Test texture existence validation
   - Test texture generation from templates
   - Test fallback texture loading
   - Test GUI rendering with missing textures

### Integration Tests
1. **Full Modpack Launch Test**
   - Launch modpack with fixed JSON files
   - Verify no EOFException errors occur
   - Test complete mod loading sequence

2. **Keybinding Functionality Test**
   - Test G key press in-game
   - Verify global skill activation
   - Test with multiple players
   - Test keybinding persistence

3. **GUI Rendering Test**
   - Open Origins GUI screens
   - Verify all textures load correctly
   - Test HUD overlay rendering
   - Test with different screen resolutions

### Manual Testing Procedures
1. **JSON Issue Testing**
   - Intentionally corrupt JSON files
   - Launch modpack and verify error handling
   - Test automatic repair functionality

2. **Keybinding Testing**
   - Test G key in various game states
   - Test with different Origins and professions
   - Verify visual and audio feedback

3. **Texture Testing**
   - Remove texture files and test fallbacks
   - Test GUI rendering in different contexts
   - Verify texture quality and scaling

## Implementation Phases

### Phase 1: Diagnostic Tools
- Create JSON file scanner and validator
- Implement keybinding diagnostic utilities
- Build texture asset verification system

### Phase 2: Issue Resolution
- Fix identified JSON parsing issues
- Resolve keybinding registration problems
- Generate missing texture assets

### Phase 3: Testing and Validation
- Run comprehensive test suite
- Perform manual testing procedures
- Validate fixes in live modpack environment

### Phase 4: Prevention and Monitoring
- Implement ongoing validation systems
- Add diagnostic logging for future issues
- Create maintenance procedures