package io.github.apace100.origins.power;

import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.PowerTypeReference;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.origins.Origins;
import net.minecraft.registry.Registry;

/**
 * Регистрация кастомных типов сил для Origins
 */
@SuppressWarnings("unchecked")
public class CustomOriginsPowerTypes {
    
    public static final PowerType<?> BLACKSMITH_QUALITY_CRAFTING = 
        new PowerTypeReference<>(Origins.identifier("blacksmith_quality_crafting"));
    
    public static final PowerType<?> BREWER_ENHANCED_POTIONS = 
        new PowerTypeReference<>(Origins.identifier("brewer_enhanced_potions"));
    
    public static final PowerType<?> BREWER_REDUCED_DROPS = 
        new PowerTypeReference<>(Origins.identifier("brewer_reduced_drops"));
    
    public static final PowerType<?> COOK_ENHANCED_FOOD = 
        new PowerTypeReference<>(Origins.identifier("cook_enhanced_food"));
    

    
    public static void register() {
        register(new PowerFactory<>(Origins.identifier("blacksmith_quality_crafting"),
            new SerializableData(),
            data -> (type, entity) -> new BlacksmithQualityCraftingPower(type, entity)
        ).allowCondition());
            
        register(new PowerFactory<>(Origins.identifier("brewer_enhanced_potions"),
            new SerializableData()
                .add("duration_multiplier", SerializableDataTypes.FLOAT, 2.0f),
            data -> (type, entity) -> new BrewerEnhancedPotionsPower(type, entity,
                data.getFloat("duration_multiplier"))
        ).allowCondition());
            
        register(new PowerFactory<>(Origins.identifier("brewer_reduced_drops"),
            new SerializableData()
                .add("drop_reduction_multiplier", SerializableDataTypes.FLOAT, 0.7f),
            data -> (type, entity) -> new BrewerReducedDropsPower(type, entity,
                data.getFloat("drop_reduction_multiplier"))
        ).allowCondition());
            
        register(new PowerFactory<>(Origins.identifier("cook_enhanced_food"),
            new SerializableData()
                .add("nutrition_multiplier", SerializableDataTypes.FLOAT, 1.5f)
                .add("saturation_multiplier", SerializableDataTypes.FLOAT, 1.5f),
            data -> (type, entity) -> new CookEnhancedFoodPower(type, entity,
                data.getFloat("nutrition_multiplier"),
                data.getFloat("saturation_multiplier"))
        ).allowCondition());
        

    }
    
    private static void register(PowerFactory<?> serializer) {
        Registry.register(ApoliRegistries.POWER_FACTORY, serializer.getSerializerId(), serializer);
    }
}