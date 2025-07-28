# Implementation Plan

- [x] 1. Fix QuestManager multiple quest support


  - Modify canTakeQuest() method to check quest limits instead of blocking all additional quests
  - Update quest acceptance logic to allow multiple active quests up to the configured limit
  - Add proper quest slot validation and error reporting
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_



- [ ] 2. Implement informative quest ticket display names
  - Override getName() method in QuestTicketItem to return quest-specific names
  - Format ticket names as "Билет квеста: [Quest Title]" with rarity and class information


  - Update ticket creation logic to set custom display names in NBT data
  - _Requirements: 2.1, 2.2, 2.3_

- [x] 3. Enhance quest ticket tooltips with progress information


  - Modify appendTooltip() to show real-time quest progress for active quests
  - Add remaining time display with color coding (red for urgent, yellow for moderate, green for plenty)
  - Include completion status indicators and turn-in instructions for completed quests
  - _Requirements: 2.2, 2.4, 2.5_



- [ ] 4. Implement Shift+Right Click quest completion on bounty board
  - Modify BountyBoard.onUse() to detect Shift+Right Click interactions with quest tickets
  - Add quest completion validation logic to check if all objectives are fulfilled


  - Implement reward distribution system for completed quests
  - _Requirements: 3.1, 3.2, 3.3, 3.7_

- [x] 5. Add quest progress validation and tracking system


  - Implement real-time objective progress tracking and validation
  - Add completion status checking for quest turn-in attempts
  - Create quest state transition management (AVAILABLE → ACCEPTED → IN_PROGRESS → COMPLETED → FINISHED)
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_





- [ ] 6. Implement comprehensive error handling and user feedback
  - Add clear error messages for failed quest operations with specific reasons
  - Implement quest limit reached notifications with current count display
  - Add validation for incomplete quest turn-in attempts with remaining objective details
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 7. Update QuestTicketAcceptanceHandler for improved quest acceptance logic
  - Fix canAcceptQuest() method to properly validate quest limits instead of blocking all quests
  - Improve quest validation logic to handle multiple active quests correctly
  - Add better error reporting and user feedback for quest acceptance failures
  - _Requirements: 1.1, 1.2, 1.3, 5.1, 5.2_

- [ ] 8. Test and validate multiple quest functionality
  - Create unit tests for multiple quest acceptance scenarios
  - Test quest limit enforcement and error handling
  - Validate quest ticket display and tooltip functionality with multiple active quests
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3_