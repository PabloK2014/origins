# Quest Folder Dependency Analysis

## Core Entry Points (Referenced from Origins.java)
1. **QuestRegistry.register()** - Main registration point
2. **QuestResourceReloadListener** - Resource loading
3. **BountifulQuestEventHandler.registerEvents()** - Event handling
4. **QuestEventHandlers.initialize()** - Event initialization
5. **QuestTicketTimeUpdater.register()** - Time management

## Networking Dependencies (Referenced from Origins.java)
1. **QuestPackets.registerServerPackets()** - Network packet registration
2. **QuestAcceptancePacket.registerServerHandler()** - Acceptance handling

## Command Dependencies (Referenced from Origins.java)
Multiple quest-related commands are registered, indicating these files are used:
- BountifulQuestCommand
- TestQuestCommand
- CheckQuestTicketsCommand
- ClearQuestsCommand
- RefreshBountyBoardCommand
- CreateBountyBoardCommand
- TestQuestLoadingCommand
- TestBountyBoardCommand
- InitBountyBoardCommand
- TestBountyBoardFixCommand
- ForceClearBoardCommand
- FixBountyBoardCommand
- FinalFixCommand
- TestTimeUpdateCommand
- TestAutoTimeCommand
- CheckTimeStatusCommand
- ResetAllTicketTimesCommand
- TestClassRestrictionCommand
- TestProgressCommand

## Core Quest System Files (USED)
1. **Quest.java** - Core quest data structure
2. **QuestRegistry.java** - Registration of all quest components
3. **QuestResourceReloadListener.java** - Resource loading
4. **QuestTicketItem.java** - Quest ticket item implementation
5. **QuestTicketAcceptanceHandler.java** - Quest acceptance logic
6. **QuestManager.java** - Quest state management
7. **ActiveQuest.java** - Active quest tracking
8. **QuestInventoryManager.java** - Inventory management
9. **QuestEventHandlers.java** - Event handling
10. **QuestTicketTimeUpdater.java** - Time management
11. **BountifulQuestEventHandler.java** - Bountiful integration
12. **QuestItem.java** - Base quest item functionality
13. **BountifulQuestItem.java** - Bountiful quest item
14. **QuestObjective.java** - Quest objectives
15. **QuestReward.java** - Quest rewards
16. **QuestProgressTracker.java** - Progress tracking
17. **QuestIntegration.java** - Integration utilities
18. **QuestTicketState.java** - Ticket state management
19. **QuestErrorRecovery.java** - Error handling
20. **QuestAcceptanceError.java** - Error types

## Bounty Board Files (USED)
1. **BountyBoard.java** - Base bounty board block
2. **BountyBoardBlockEntity.java** - Base block entity
3. **BountyBoardScreen.java** - UI screen
4. **BountyBoardScreenHandler.java** - Screen handler
5. **CookBountyBoard.java** - Cook-specific board
6. **CookBountyBoardBlockEntity.java** - Cook board entity
7. **WarriorBountyBoard.java** - Warrior-specific board
8. **WarriorBountyBoardBlockEntity.java** - Warrior board entity
9. **BlacksmithBountyBoard.java** - Blacksmith-specific board
10. **BlacksmithBountyBoardBlockEntity.java** - Blacksmith board entity
11. **BrewerBountyBoard.java** - Brewer-specific board
12. **BrewerBountyBoardBlockEntity.java** - Brewer board entity
13. **CourierBountyBoard.java** - Courier-specific board
14. **CourierBountyBoardBlockEntity.java** - Courier board entity
15. **MinerBountyBoard.java** - Miner-specific board
16. **MinerBountyBoardBlockEntity.java** - Miner board entity

## Bountiful Integration Files (USED)
1. **BountifulQuestCreator.java** - Quest creation
2. **BountifulQuestData.java** - Quest data handling
3. **BountifulQuestEntry.java** - Quest entries
4. **BountifulQuestInfo.java** - Quest information
5. **BountifulQuestObjectiveType.java** - Objective types
6. **BountifulQuestRewardType.java** - Reward types

## Utility Files (USED)
1. **SkillPointToken.java** - Skill point tokens
2. **QuestGenerator.java** - Quest generation
3. **BountyQuest.java** - Bounty quest implementation

## UI/GUI Files (USED)
1. **gui/QuestButton.java** - UI button component
2. **gui/QuestTooltipRenderer.java** - Tooltip rendering
3. **gui/SpriteHelper.java** - Sprite utilities

## Potentially Unused Files (NEED VERIFICATION)
These files may not have direct references but could be used indirectly:
1. **QuestSlot.java** - Slot implementation
2. **BountySlot.java** - Bounty slot
3. **DecreeSlot.java** - Decree slot
4. **SelectedQuestSlot.java** - Selected quest slot
5. **BountifulQuestSlot.java** - Bountiful quest slot
6. **QuestInventory.java** - Quest inventory
7. **QuestProgress.java** - Progress tracking
8. **QuestStateManager.java** - State management
9. **QuestSelectionState.java** - Selection state
10. **QuestMaskManager.java** - Mask management
11. **QuestDragHandler.java** - Drag handling
12. **QuestDragVisualizer.java** - Drag visualization
13. **QuestTooltipContent.java** - Tooltip content
14. **BountyBoardFeature.java** - Board features

## Networking Files (USED)
1. **MaskSyncPacket.java** - Mask synchronization
2. **QuestSyncPacket.java** - Quest synchronization
3. **SelectionSyncPacket.java** - Selection synchronization
4. **QuestTicketClientUpdater.java** - Client updates

## Analysis Summary
- **Total files in quest folder**: ~60 files
- **Definitely used files**: ~45 files
- **Potentially unused files**: ~15 files
- **Files to investigate further**: Slot implementations, some state managers, and drag handlers

## Next Steps
1. Verify the "potentially unused" files by checking for indirect references
2. Test removal of suspected unused files one by one
3. Focus on optimizing the large, complex files like QuestTicketItem and QuestTicketAcceptanceHandler