package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class QuestRegistry {
    // Блок доски объявлений
    public static final Block BOUNTY_BOARD = new BountyBoard(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.5f));
    
    // Специализированные доски объявлений для классов
    public static final Block COOK_BOUNTY_BOARD = new CookBountyBoard(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.5f));
    public static final Block WARRIOR_BOUNTY_BOARD = new WarriorBountyBoard(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.5f));
    public static final Block BLACKSMITH_BOUNTY_BOARD = new BlacksmithBountyBoard(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.5f));
    public static final Block BREWER_BOUNTY_BOARD = new BrewerBountyBoard(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.5f));
    public static final Block COURIER_BOUNTY_BOARD = new CourierBountyBoard(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.5f));
    public static final Block MINER_BOUNTY_BOARD = new MinerBountyBoard(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(2.5f));
    
    // BlockItem для досок объявлений
    public static final BlockItem BOUNTY_BOARD_ITEM = new BlockItem(BOUNTY_BOARD, new Item.Settings());
    public static final BlockItem COOK_BOUNTY_BOARD_ITEM = new BlockItem(COOK_BOUNTY_BOARD, new Item.Settings());
    public static final BlockItem WARRIOR_BOUNTY_BOARD_ITEM = new BlockItem(WARRIOR_BOUNTY_BOARD, new Item.Settings());
    public static final BlockItem BLACKSMITH_BOUNTY_BOARD_ITEM = new BlockItem(BLACKSMITH_BOUNTY_BOARD, new Item.Settings());
    public static final BlockItem BREWER_BOUNTY_BOARD_ITEM = new BlockItem(BREWER_BOUNTY_BOARD, new Item.Settings());
    public static final BlockItem COURIER_BOUNTY_BOARD_ITEM = new BlockItem(COURIER_BOUNTY_BOARD, new Item.Settings());
    public static final BlockItem MINER_BOUNTY_BOARD_ITEM = new BlockItem(MINER_BOUNTY_BOARD, new Item.Settings());
    
    // BlockEntity для доски объявлений
    public static BlockEntityType<BountyBoardBlockEntity> BOUNTY_BOARD_BLOCK_ENTITY;
    
    // BlockEntity для специализированных досок
    public static BlockEntityType<CookBountyBoardBlockEntity> COOK_BOUNTY_BOARD_BLOCK_ENTITY;
    public static BlockEntityType<WarriorBountyBoardBlockEntity> WARRIOR_BOUNTY_BOARD_BLOCK_ENTITY;
    public static BlockEntityType<BlacksmithBountyBoardBlockEntity> BLACKSMITH_BOUNTY_BOARD_BLOCK_ENTITY;
    public static BlockEntityType<BrewerBountyBoardBlockEntity> BREWER_BOUNTY_BOARD_BLOCK_ENTITY;
    public static BlockEntityType<CourierBountyBoardBlockEntity> COURIER_BOUNTY_BOARD_BLOCK_ENTITY;
    public static BlockEntityType<MinerBountyBoardBlockEntity> MINER_BOUNTY_BOARD_BLOCK_ENTITY;
    
    // Screen Handler Type
    public static ScreenHandlerType<BountyBoardScreenHandler> BOUNTY_BOARD_SCREEN_HANDLER;
    
    // Предметы наград за задания
    public static final Item SKILL_POINT_TOKEN_TIER1 = new SkillPointToken(new Item.Settings(), 500);
    public static final Item SKILL_POINT_TOKEN_TIER2 = new SkillPointToken(new Item.Settings(), 1000);
    public static final Item SKILL_POINT_TOKEN_TIER3 = new SkillPointToken(new Item.Settings(), 1500);
    
    // Билеты квестов разных редкостей
    public static final Item QUEST_TICKET_COMMON = new QuestTicketItem(new Item.Settings(), Quest.QuestRarity.COMMON);
    public static final Item QUEST_TICKET_UNCOMMON = new QuestTicketItem(new Item.Settings(), Quest.QuestRarity.UNCOMMON);
    public static final Item QUEST_TICKET_RARE = new QuestTicketItem(new Item.Settings(), Quest.QuestRarity.RARE);
    public static final Item QUEST_TICKET_EPIC = new QuestTicketItem(new Item.Settings(), Quest.QuestRarity.EPIC);
    
    // Новый предмет квеста в стиле Bountiful
    public static final BountifulQuestItem BOUNTIFUL_QUEST_ITEM = new BountifulQuestItem();
    
    public static void register() {
        Origins.LOGGER.info("Регистрация системы квестов...");
        
        // Регистрируем блок доски объявлений
        Registry.register(Registries.BLOCK, new Identifier(Origins.MODID, "bounty_board"), BOUNTY_BOARD);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "bounty_board"), BOUNTY_BOARD_ITEM);
        
        // Регистрируем специализированные доски объявлений
        Registry.register(Registries.BLOCK, new Identifier(Origins.MODID, "cook_bounty_board"), COOK_BOUNTY_BOARD);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "cook_bounty_board"), COOK_BOUNTY_BOARD_ITEM);
            
        Registry.register(Registries.BLOCK, new Identifier(Origins.MODID, "warrior_bounty_board"), WARRIOR_BOUNTY_BOARD);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "warrior_bounty_board"), WARRIOR_BOUNTY_BOARD_ITEM);
            
        Registry.register(Registries.BLOCK, new Identifier(Origins.MODID, "blacksmith_bounty_board"), BLACKSMITH_BOUNTY_BOARD);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "blacksmith_bounty_board"), BLACKSMITH_BOUNTY_BOARD_ITEM);
            
        Registry.register(Registries.BLOCK, new Identifier(Origins.MODID, "brewer_bounty_board"), BREWER_BOUNTY_BOARD);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "brewer_bounty_board"), BREWER_BOUNTY_BOARD_ITEM);
            
        Registry.register(Registries.BLOCK, new Identifier(Origins.MODID, "courier_bounty_board"), COURIER_BOUNTY_BOARD);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "courier_bounty_board"), COURIER_BOUNTY_BOARD_ITEM);
            
        Registry.register(Registries.BLOCK, new Identifier(Origins.MODID, "miner_bounty_board"), MINER_BOUNTY_BOARD);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "miner_bounty_board"), MINER_BOUNTY_BOARD_ITEM);
        
        // Регистрируем BlockEntity
        BOUNTY_BOARD_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            new Identifier(Origins.MODID, "bounty_board"),
            FabricBlockEntityTypeBuilder.create(BountyBoardBlockEntity::new, BOUNTY_BOARD).build());
            
        // Регистрируем BlockEntity для специализированных досок
        COOK_BOUNTY_BOARD_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            new Identifier(Origins.MODID, "cook_bounty_board"),
            FabricBlockEntityTypeBuilder.create(CookBountyBoardBlockEntity::new, COOK_BOUNTY_BOARD).build());
            
        WARRIOR_BOUNTY_BOARD_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            new Identifier(Origins.MODID, "warrior_bounty_board"),
            FabricBlockEntityTypeBuilder.create(WarriorBountyBoardBlockEntity::new, WARRIOR_BOUNTY_BOARD).build());
            
        BLACKSMITH_BOUNTY_BOARD_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            new Identifier(Origins.MODID, "blacksmith_bounty_board"),
            FabricBlockEntityTypeBuilder.create(BlacksmithBountyBoardBlockEntity::new, BLACKSMITH_BOUNTY_BOARD).build());
            
        BREWER_BOUNTY_BOARD_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            new Identifier(Origins.MODID, "brewer_bounty_board"),
            FabricBlockEntityTypeBuilder.create(BrewerBountyBoardBlockEntity::new, BREWER_BOUNTY_BOARD).build());
            
        COURIER_BOUNTY_BOARD_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            new Identifier(Origins.MODID, "courier_bounty_board"),
            FabricBlockEntityTypeBuilder.create(CourierBountyBoardBlockEntity::new, COURIER_BOUNTY_BOARD).build());
            
        MINER_BOUNTY_BOARD_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            new Identifier(Origins.MODID, "miner_bounty_board"),
            FabricBlockEntityTypeBuilder.create(MinerBountyBoardBlockEntity::new, MINER_BOUNTY_BOARD).build());
        
        // Регистрируем Screen Handler (правильный способ для Fabric)
        BOUNTY_BOARD_SCREEN_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(Origins.MODID, "bounty_board"),
            new ExtendedScreenHandlerType<>(BountyBoardScreenHandler::new)
        );
        
        // Регистрируем предметы наград
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "skill_point_token_tier1"), SKILL_POINT_TOKEN_TIER1);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "skill_point_token_tier2"), SKILL_POINT_TOKEN_TIER2);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "skill_point_token_tier3"), SKILL_POINT_TOKEN_TIER3);
        
        // Регистрируем билеты квестов
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "quest_ticket_common"), QUEST_TICKET_COMMON);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "quest_ticket_uncommon"), QUEST_TICKET_UNCOMMON);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "quest_ticket_rare"), QUEST_TICKET_RARE);
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "quest_ticket_epic"), QUEST_TICKET_EPIC);
        
        // Регистрируем новый предмет квеста
        Registry.register(Registries.ITEM, new Identifier(Origins.MODID, "bountiful_quest"), BOUNTIFUL_QUEST_ITEM);
        
        Origins.LOGGER.info("Система квестов зарегистрирована успешно!");
    }
} 