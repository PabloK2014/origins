package io.github.apace100.origins;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.PowerTypes;
import io.github.apace100.apoli.util.NamespaceAlias;
import io.github.apace100.calio.mixin.CriteriaRegistryInvoker;
import io.github.apace100.calio.resource.OrderedResourceListenerInitializer;
import io.github.apace100.calio.resource.OrderedResourceListenerManager;
import io.github.apace100.origins.badge.BadgeManager;
import io.github.apace100.origins.command.OriginCommand;
import io.github.apace100.origins.command.GiveQualityItemCommand;
import io.github.apace100.origins.command.ProgressionCommand;
import io.github.apace100.origins.networking.ModPacketsC2S;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginManager;
import io.github.apace100.origins.quest.QuestResourceReloadListener;
import io.github.apace100.origins.power.OriginsEntityConditions;
import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.apace100.origins.power.CustomOriginsPowerTypes;
import io.github.apace100.origins.registry.*;
import io.github.apace100.origins.util.ChoseOriginCriterion;
import io.github.apace100.origins.util.OriginsConfigSerializer;
import io.github.apace100.origins.util.OriginsJsonConfigSerializer;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemGroups;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Origins implements ModInitializer, OrderedResourceListenerInitializer {

	public static final String MODID = "origins";
	public static String VERSION = "";
	public static int[] SEMVER;
	public static final Logger LOGGER = LogManager.getLogger(Origins.class);

	public static ServerConfig config;
	private static ConfigSerializer<ServerConfig> configSerializer;

	@Override
	public void onInitialize() {
		FabricLoader.getInstance().getModContainer(MODID).ifPresent(modContainer -> {
			VERSION = modContainer.getMetadata().getVersion().getFriendlyString();
			if(VERSION.contains("+")) {
				VERSION = VERSION.split("\\+")[0];
			}
			if(VERSION.contains("-")) {
				VERSION = VERSION.split("-")[0];
			}
			String[] splitVersion = VERSION.split("\\.");
			SEMVER = new int[splitVersion.length];
			for(int i = 0; i < SEMVER.length; i++) {
				SEMVER[i] = Integer.parseInt(splitVersion[i]);
			}
		});
		LOGGER.info("Origins " + VERSION + " is initializing. Have fun!");

		AutoConfig.register(ServerConfig.class,
			(definition, configClass) -> {
				configSerializer = new OriginsJsonConfigSerializer<>(definition, configClass,
					new OriginsConfigSerializer<>(definition, configClass));
				return configSerializer;
			});
		config = AutoConfig.getConfigHolder(ServerConfig.class).getConfig();

		NamespaceAlias.addAlias(MODID, "apoli");

		OriginsPowerTypes.register();
		CustomOriginsPowerTypes.register();
		OriginsEntityConditions.register();

		ModBlocks.register();
		ModItems.register();
		ModTags.register();
		ModPacketsC2S.register();
		ModEnchantments.register();
		ModEntities.register();
		ModLoot.registerLootTables();
		Origin.init();
		
		// Регистрируем квестовую систему
		io.github.apace100.origins.quest.QuestRegistry.register();
		
		// Регистрируем пакеты квестов
		io.github.apace100.origins.networking.QuestPackets.registerServerPackets();
		
		// Регистрируем обработчики событий для квестов Bountiful
		io.github.apace100.origins.quest.BountifulQuestEventHandler.registerEvents();
		io.github.apace100.origins.networking.QuestAcceptancePacket.registerServerHandler();
		
		// Инициализируем реестр профессий
		io.github.apace100.origins.profession.ProfessionRegistry.init();
		
		// Регистрируем обработчик активации навыков
		io.github.apace100.origins.networking.SkillActivationHandler.register();
		
		// Инициализируем обработчики событий квестов
		io.github.apace100.origins.quest.QuestEventHandlers.initialize();
		
		// Регистрируем обновлятель времени билетов квестов
		io.github.apace100.origins.quest.QuestTicketTimeUpdater.register();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			OriginCommand.register(dispatcher);
			GiveQualityItemCommand.register(dispatcher, registryAccess);
			ProgressionCommand.register(dispatcher);
			io.github.apace100.origins.command.ResetOriginCommand.register(dispatcher);
			io.github.apace100.origins.command.SetActiveSkillCommand.register(dispatcher);
			io.github.apace100.origins.command.JsonDiagnosticCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.ClearQuestsCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.TestQuestCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.TestQuestTrackingCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.CheckQuestTicketsCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.TestProgressCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.BountifulQuestCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.RefreshBountyBoardCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.CreateBountyBoardCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.TestQuestLoadingCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.TestBountyBoardCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.InitBountyBoardCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.TestBountyBoardFixCommand.register(dispatcher, registryAccess, environment);
			io.github.apace100.origins.command.ForceClearBoardCommand.register(dispatcher, registryAccess, environment);
			io.github.apace100.origins.command.FixBountyBoardCommand.register(dispatcher, registryAccess, environment);
			io.github.apace100.origins.command.FinalFixCommand.register(dispatcher, registryAccess, environment);
			io.github.apace100.origins.command.TestTimeUpdateCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.TestAutoTimeCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.CheckTimeStatusCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.ResetAllTicketTimesCommand.register(dispatcher, registryAccess);
			io.github.apace100.origins.command.TestClassRestrictionCommand.register(dispatcher, registryAccess);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register((content) -> {
			content.add(ModItems.ORB_OF_ORIGIN);
		});

		CriteriaRegistryInvoker.callRegister(ChoseOriginCriterion.INSTANCE);
		
		// Запускаем диагностику JSON файлов при инициализации
		if (config.debugMode) {
			LOGGER.info("Debug mode enabled - running JSON diagnostic on startup");
			try {
				io.github.apace100.origins.util.JsonDiagnostic.runFullDiagnostic();
			} catch (Exception e) {
				LOGGER.error("Failed to run startup JSON diagnostic: " + e.getMessage(), e);
			}
		}
		
		// Инициализируем службу постоянной валидации
		try {
			io.github.apace100.origins.util.OngoingValidationService.initialize();
		} catch (Exception e) {
			LOGGER.error("Failed to initialize ongoing validation service: " + e.getMessage(), e);
		}
	}

	public static void serializeConfig() {
		try {
			configSerializer.serialize(config);
		} catch (ConfigSerializer.SerializationException e) {
			Origins.LOGGER.error("Failed serialization of config file: " + e.getMessage());
		}
	}

	public static Identifier identifier(String path) {
		return new Identifier(Origins.MODID, path);
	}

	@Override
	public void registerResourceListeners(OrderedResourceListenerManager manager) {
		Identifier powerData = Apoli.identifier("powers");
		Identifier originData = Origins.identifier("origins");

		OriginManager originLoader = new OriginManager();
		manager.register(ResourceType.SERVER_DATA, originLoader).after(powerData).complete();
		manager.register(ResourceType.SERVER_DATA, new OriginLayers()).after(originData).complete();

		// Регистрируем загрузчик квестов
		manager.register(ResourceType.SERVER_DATA, new QuestResourceReloadListener()).after(originData).complete();

		BadgeManager.init();

		IdentifiableResourceReloadListener badgeLoader = BadgeManager.REGISTRY.getLoader();
		manager.register(ResourceType.SERVER_DATA, badgeLoader).before(powerData).complete();
		PowerTypes.DEPENDENCIES.add(badgeLoader.getFabricId());
	}

	@Config(name = Origins.MODID + "_server")
	public static class ServerConfig implements ConfigData {

		public boolean performVersionCheck = true;

		public boolean showHudOverlay = true; // Показывать HUD оверлей (можно скрыть в настройках)
		
		public boolean debugMode = false; // Режим отладки для подробного логирования

		public JsonObject origins = new JsonObject();

		public boolean isOriginDisabled(Identifier originId) {
			String idString = originId.toString();
			if(!origins.has(idString)) {
				return false;
			}
			JsonElement element = origins.get(idString);
			if(element instanceof JsonObject jsonObject) {
				return !JsonHelper.getBoolean(jsonObject, "enabled", true);
			}
			return false;
		}

		public boolean isPowerDisabled(Identifier originId, Identifier powerId) {
			String originIdString = originId.toString();
			if(!origins.has(originIdString)) {
				return false;
			}
			String powerIdString = powerId.toString();
			JsonElement element = origins.get(originIdString);
			if(element instanceof JsonObject jsonObject) {
				return !JsonHelper.getBoolean(jsonObject, powerIdString, true);
			}
			return false;
		}

		public boolean addToConfig(Origin origin) {
			boolean changed = false;
			String originIdString = origin.getIdentifier().toString();
			JsonObject originObj;
			if(!origins.has(originIdString) || !(origins.get(originIdString) instanceof JsonObject)) {
				originObj = new JsonObject();
				origins.add(originIdString, originObj);
				changed = true;
			} else {
				originObj = (JsonObject) origins.get(originIdString);
			}
			if(!originObj.has("enabled") || !(originObj.get("enabled") instanceof JsonPrimitive)) {
				originObj.addProperty("enabled", Boolean.TRUE);
				changed = true;
			}
			for(PowerType<?> power : origin.getPowerTypes()) {
				String powerIdString = power.getIdentifier().toString();
				if(!originObj.has(powerIdString) || !(originObj.get(powerIdString) instanceof JsonPrimitive)) {
					originObj.addProperty(powerIdString, Boolean.TRUE);
					changed = true;
				}
			}
			return changed;
		}
	}
}
