# Quest Board UI Fixes Design

## Overview

This design addresses critical UI issues in the quest board system identified through log analysis. The main problems are: quest selection highlighting not working, missing tooltip information, and quest acceptance logic failures. The solution involves fixing the UI state management, improving visual feedback, and enhancing error handling.

## Architecture

The quest board UI consists of three main components that need fixes:

1. **BountyBoardScreen** - Client-side UI rendering and interaction handling
2. **QuestButton** - Individual quest display and selection logic  
3. **BountyBoardScreenHandler** - Server-side quest management and synchronization

## Components and Interfaces

### 1. Quest Selection System Enhancement

**Problem:** Quest selection highlighting is not working properly when clicking on quests in the left panel.

**Solution:** Implement a robust selection state management system:

```java
// Enhanced selection tracking in BountyBoardScreen
private int selectedQuestIndex = -1;
private Quest selectedQuest = null;

// Selection highlighting in quest rendering
private void renderQuestSelection(DrawContext context, int questIndex, int x, int y) {
    if (questIndex == selectedQuestIndex) {
        // Draw selection highlight
        context.fill(x-2, y-2, x+WIDTH+2, y+HEIGHT+2, 0x80FFFF00);
    }
}
```

**Key Changes:**
- Add proper selection state tracking in BountyBoardScreen
- Implement visual selection indicators in quest rendering
- Synchronize selection state between client and server
- Add selection change callbacks for UI updates

### 2. Tooltip Information System

**Problem:** Many quests show no tooltip information when hovered.

**Solution:** Implement comprehensive tooltip fallback system:

```java
// Enhanced tooltip rendering in QuestTooltipRenderer
public static void renderQuestTooltip(DrawContext context, Quest quest, int mouseX, int mouseY) {
    List<Text> tooltipLines = new ArrayList<>();
    
    // Always show basic quest info
    tooltipLines.add(Text.literal(quest.getTitle()).formatted(Formatting.YELLOW));
    
    // Add objective info with fallbacks
    QuestObjective objective = quest.getObjective();
    if (objective != null) {
        tooltipLines.add(getObjectiveDescription(objective));
    } else {
        tooltipLines.add(Text.literal("No objective information").formatted(Formatting.GRAY));
    }
    
    // Add reward info with fallbacks
    QuestReward reward = quest.getReward();
    if (reward != null) {
        tooltipLines.add(getRewardDescription(reward));
    } else {
        tooltipLines.add(Text.literal("No reward information").formatted(Formatting.GRAY));
    }
    
    context.drawTooltip(MinecraftClient.getInstance().textRenderer, tooltipLines, mouseX, mouseY);
}
```

**Key Changes:**
- Add fallback text for missing quest information
- Implement comprehensive tooltip content generation
- Add proper formatting and color coding
- Handle null/empty quest data gracefully

### 3. Quest Acceptance Logic Fixes

**Problem:** Quest acceptance fails due to profession mismatches and state synchronization issues.

**Solution:** Implement robust quest acceptance validation:

```java
// Enhanced quest acceptance in QuestTicketAcceptanceHandler
public boolean canAcceptQuest(PlayerEntity player, Quest quest) {
    // Check player profession
    String playerClass = getPlayerOriginClass(player);
    String questClass = quest.getPlayerClass();
    
    if (!playerClass.equals(questClass)) {
        Origins.LOGGER.warn("Player class {} does not match quest class {}", playerClass, questClass);
        return false;
    }
    
    // Check if player already has active quest
    if (hasActiveQuest(player)) {
        Origins.LOGGER.warn("Player already has an active quest");
        return false;
    }
    
    // Check quest availability
    if (!isQuestAvailable(quest)) {
        Origins.LOGGER.warn("Quest {} is not available", quest.getId());
        return false;
    }
    
    return true;
}
```

**Key Changes:**
- Add comprehensive quest acceptance validation
- Implement proper error messages and logging
- Add profession compatibility checking
- Handle edge cases and error states

### 4. UI State Synchronization

**Problem:** Client and server quest states become desynchronized.

**Solution:** Implement proper state synchronization:

```java
// Enhanced synchronization in BountyBoardScreenHandler
public void syncQuestState(ServerPlayerEntity player) {
    // Send quest list update
    QuestSyncPacket questPacket = new QuestSyncPacket(getAvailableQuests());
    ServerPlayNetworking.send(player, QuestSyncPacket.ID, questPacket.toPacketByteBuf());
    
    // Send selection state update
    SelectionSyncPacket selectionPacket = new SelectionSyncPacket(selectedQuestIndex);
    ServerPlayNetworking.send(player, SelectionSyncPacket.ID, selectionPacket.toPacketByteBuf());
    
    // Send mask state update
    MaskSyncPacket maskPacket = new MaskSyncPacket(questInventory.getMaskedSlots());
    ServerPlayNetworking.send(player, MaskSyncPacket.ID, maskPacket.toPacketByteBuf());
}
```

**Key Changes:**
- Add dedicated synchronization packets
- Implement proper client-server state reconciliation
- Add automatic sync on state changes
- Handle network errors gracefully

## Data Models

### Enhanced Quest Selection State

```java
public class QuestSelectionState {
    private int selectedIndex = -1;
    private String selectedQuestId = null;
    private long selectionTimestamp = 0;
    
    public void selectQuest(int index, Quest quest) {
        this.selectedIndex = index;
        this.selectedQuestId = quest != null ? quest.getId() : null;
        this.selectionTimestamp = System.currentTimeMillis();
    }
    
    public boolean isSelected(int index, Quest quest) {
        return selectedIndex == index && 
               quest != null && 
               quest.getId().equals(selectedQuestId);
    }
}
```

### Tooltip Content Model

```java
public class QuestTooltipContent {
    private final List<Text> lines = new ArrayList<>();
    private final Map<String, Object> metadata = new HashMap<>();
    
    public static QuestTooltipContent fromQuest(Quest quest) {
        QuestTooltipContent content = new QuestTooltipContent();
        
        // Add title
        content.addLine(Text.literal(quest.getTitle()).formatted(Formatting.YELLOW));
        
        // Add objective with fallback
        if (quest.getObjective() != null) {
            content.addObjectiveLine(quest.getObjective());
        } else {
            content.addLine(Text.literal("No objective").formatted(Formatting.GRAY));
        }
        
        // Add reward with fallback
        if (quest.getReward() != null) {
            content.addRewardLine(quest.getReward());
        } else {
            content.addLine(Text.literal("No reward").formatted(Formatting.GRAY));
        }
        
        return content;
    }
}
```

## Error Handling

### Quest Acceptance Error Types

```java
public enum QuestAcceptanceError {
    PROFESSION_MISMATCH("Player profession does not match quest requirements"),
    ALREADY_HAS_QUEST("Player already has an active quest"),
    QUEST_UNAVAILABLE("Quest is no longer available"),
    NETWORK_ERROR("Network communication failed"),
    UNKNOWN_ERROR("Unknown error occurred");
    
    private final String message;
    
    QuestAcceptanceError(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
}
```

### Error Recovery Mechanisms

```java
public class QuestErrorRecovery {
    public static void handleQuestAcceptanceFailure(QuestAcceptanceError error, Quest quest, PlayerEntity player) {
        // Log the error
        Origins.LOGGER.warn("Quest acceptance failed: {} for quest {} and player {}", 
            error.getMessage(), quest.getId(), player.getName().getString());
        
        // Send user-friendly message
        Text errorMessage = switch (error) {
            case PROFESSION_MISMATCH -> Text.translatable("gui.origins.quest.error.profession_mismatch");
            case ALREADY_HAS_QUEST -> Text.translatable("gui.origins.quest.error.already_has_quest");
            case QUEST_UNAVAILABLE -> Text.translatable("gui.origins.quest.error.unavailable");
            default -> Text.translatable("gui.origins.quest.error.generic");
        };
        
        player.sendMessage(errorMessage, false);
        
        // Attempt recovery
        switch (error) {
            case NETWORK_ERROR -> retryNetworkOperation(quest, player);
            case QUEST_UNAVAILABLE -> refreshQuestList(player);
            default -> {} // No recovery action needed
        }
    }
}
```

## Testing Strategy

### Unit Tests

1. **Quest Selection Tests**
   - Test selection state changes
   - Test visual highlighting
   - Test selection persistence

2. **Tooltip Content Tests**
   - Test tooltip generation with valid data
   - Test fallback content for missing data
   - Test tooltip positioning and rendering

3. **Quest Acceptance Tests**
   - Test profession validation
   - Test active quest checking
   - Test error handling scenarios

### Integration Tests

1. **Client-Server Synchronization Tests**
   - Test quest state sync
   - Test selection state sync
   - Test error state handling

2. **UI Interaction Tests**
   - Test quest clicking and selection
   - Test tooltip display on hover
   - Test drag-and-drop functionality

### Manual Testing Scenarios

1. **Quest Selection Flow**
   - Click on different quests in left panel
   - Verify highlighting appears in center
   - Test with masked/unmasked quests

2. **Tooltip Display**
   - Hover over quests with complete data
   - Hover over quests with missing data
   - Test tooltip positioning at screen edges

3. **Quest Acceptance**
   - Test with matching profession
   - Test with mismatched profession
   - Test with existing active quest

## Implementation Priority

1. **High Priority** - Quest selection highlighting fix
2. **High Priority** - Tooltip fallback system
3. **Medium Priority** - Quest acceptance validation
4. **Medium Priority** - Error handling improvements
5. **Low Priority** - Advanced synchronization features