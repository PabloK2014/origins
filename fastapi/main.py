from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import OpenAI
import json
import re
import uuid
from typing import List, Dict
import time
from functools import lru_cache
from concurrent.futures import ThreadPoolExecutor
import asyncio

app = FastAPI()

# Health check
@app.get("/")
async def health_check():
    return {"status": "ok"}

# Конфигурация OpenRouter API
API_KEY = "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"
client = OpenAI(
    base_url="https://openrouter.ai/api/v1",
    api_key=API_KEY,
)

# Классы и данные
CLASSES = ["cook", "courier", "brewer", "blacksmith", "miner", "warrior"]
OBJECTIVE_TYPES = {
    "cook": ["collect", "craft"],
    "courier": ["collect", "craft"],
    "brewer": ["collect", "craft"],
    "blacksmith": ["collect", "craft"],
    "miner": ["collect", "mine"],
    "warrior": ["kill", "collect"]
}
TARGETS = {
    "cook": ["wheat", "carrot", "potato", "beetroot", "egg", "milk_bucket", "sugar", "cocoa_beans", "honey_bottle", "bread", "cake", "cookie", "pumpkin_pie", "mushroom_stew", "beetroot_soup", "rabbit_stew", "golden_carrot", "golden_apple"],
    "courier": ["paper", "leather", "feather", "sugar", "emerald", "map", "compass", "saddle", "boat", "banner", "firework_rocket"],
    "brewer": ["nether_wart", "blaze_powder", "ghast_tear", "spider_eye", "magma_cream", "glowstone_dust", "redstone", "rabbit_foot", "glistering_melon_slice", "pufferfish", "turtle_helmet"],
    "blacksmith": ["iron_ingot", "gold_ingot", "diamond", "coal", "flint", "iron_sword", "diamond_pickaxe", "iron_axe", "iron_armor", "anvil", "shield", "crossbow"],
    "miner": ["coal", "iron_ore", "gold_ore", "diamond", "redstone", "lapis_lazuli", "emerald", "nether_quartz", "ancient_debris", "obsidian"],
    "warrior": ["zombie", "skeleton", "creeper", "spider", "rotten_flesh", "bone", "gunpowder", "string"]
}

# Модели
class Quest(BaseModel):
    id: str
    playerClass: str
    level: int
    title: str
    objective: Dict
    timeLimit: int
    reward: Dict

class QuestsResponse(BaseModel):
    quests: List[Quest]

class AllQuestsResponse(BaseModel):
    cook: List[Quest]
    courier: List[Quest]
    brewer: List[Quest]
    blacksmith: List[Quest]
    miner: List[Quest]
    warrior: List[Quest]

# Единый промпт
@lru_cache(maxsize=1)
def get_unified_quests_prompt(player_class: str = None, quest_count: int = 5) -> str:
    if player_class:
        return f"""
Generate {quest_count} quests for the Minecraft class "{player_class}" in JSON format, compatible with the following structure. Ensure the quests are unique in their combination of target, objective type, and level, have a level from 1 to 3, and use only the provided objective types and targets. The title must be in Russian, creative, and match the class theme. Use the provided template and valid values. Return only the JSON, without any additional text or formatting, and NEVER use 'minecraft:air' or any invalid targets.

Template:
{{
  "quests": [
    {{
      "id": "class_target_level",
      "playerClass": "origins:{player_class}",
      "level": <1-3>,
      "title": "<Russian title>",
      "objective": {{
        "type": "<objective_type>",
        "target": "minecraft:<target>",
        "amount": <1-20>
      }},
      "timeLimit": <20-50>,
      "reward": {{
        "type": "skill_point_token",
        "tier": <1-3>,
        "experience": <500-1500>
      }}
    }}
  ]
}}

Valid objective types: {', '.join(OBJECTIVE_TYPES[player_class])}
Valid targets: {', '.join(TARGETS[player_class])}
Return ONLY the JSON, no additional text.
"""
    else:
        return f"""
Generate {quest_count} quests for EACH of the following Minecraft classes: {', '.join(CLASSES)} in JSON format, compatible with the following structure. Ensure the quests are unique in their combination of target, objective type, and level, have a level from 1 to 3, and use only the provided objective types and targets. The title must be in Russian, creative, and match the class theme. Use the provided template and valid values. Return only the JSON, without any additional text or formatting, and NEVER use 'minecraft:air' or any invalid targets.

Return format:
{{
  {', '.join([f'"{cls}": [{quest_count} quests for {cls} class]' for cls in CLASSES])}
}}

Quest structure for each:
{{
  "id": "class_target_level",
  "playerClass": "origins:class_name",
  "level": <1-3>,
  "title": "<Russian title>",
  "objective": {{
    "type": "<objective_type>",
    "target": "minecraft:<target>",
    "amount": <1-20>
  }},
  "timeLimit": <20-50>,
  "reward": {{
    "type": "skill_point_token",
    "tier": <1-3>,
    "experience": <500-1500>
  }}
}}

Class objectives and targets (use ONLY these, no exceptions):
{chr(10).join([f"- {cls}: objective types {OBJECTIVE_TYPES[cls]}, targets {TARGETS[cls]}" for cls in CLASSES])}

Return ONLY the JSON, no additional text.
"""

def validate_quest_structure(quest):
    required_fields = ["id", "playerClass", "level", "title", "objective", "timeLimit", "reward"]
    if not all(key in quest for key in required_fields):
        return False
    if not all(key in quest["objective"] for key in ["type", "target", "amount"]):
        return False
    if not all(key in quest["reward"] for key in ["type", "tier", "experience"]):
        return False
    player_class = quest["playerClass"].split(":")[1]
    target = quest["objective"]["target"].replace("minecraft:", "")
    if target == "air" or target not in TARGETS.get(player_class, []):
        return False
    return True

def extract_valid_quests(generated_text: str) -> List[dict]:
    valid_quests = []
    try:
        json_match = re.search(r'\{.*"quests":\s*\[(.*?)\].*}', generated_text, re.DOTALL)
        if json_match:
            quests_str = f'{{"quests": [{json_match.group(1)}]}}'
            try:
                generated_json = json.loads(quests_str)
                quests = generated_json.get("quests", [])
                for quest in quests:
                    if validate_quest_structure(quest):
                        quest["id"] = f"{quest.get('playerClass').split(':')[1]}_{uuid.uuid4().hex[:8]}"
                        valid_quests.append(quest)
            except json.JSONDecodeError:
                pass
    except Exception as e:
        print(f"Ошибка при извлечении валидных квестов: {str(e)}")
    return valid_quests

@app.get("/quests/all", response_model=AllQuestsResponse)
async def get_all_quests(quest_count_per_class: int = 5):
    if quest_count_per_class < 1 or quest_count_per_class > 10:
        raise HTTPException(status_code=400, detail="Количество квестов на класс должно быть от 1 до 10")

    start_time = time.time()
    print(f"🌐 [FastAPI] Начинаем запрос к OpenRouter для генерации {quest_count_per_class} квестов на класс...")

    prompt = get_unified_quests_prompt(quest_count=quest_count_per_class)

    try:
        print(f"📝 [FastAPI] Отправляем промпт в AI модель...")
        print(f"🤖 [FastAPI] Модель: deepseek/deepseek-r1-0528:free")
        print(f"📊 [FastAPI] Запрашиваем {quest_count_per_class} квестов для каждого из {len(CLASSES)} классов")
        
        loop = asyncio.get_event_loop()
        with ThreadPoolExecutor(max_workers=1) as executor:
            completion = await loop.run_in_executor(
                executor,
                lambda: client.chat.completions.create(
                    extra_headers={
                        "HTTP-Referer": "http://localhost:8000",
                        "X-Title": "Minecraft All Quests Generator"
                    },
                    extra_body={},
                    model="deepseek/deepseek-r1-0528:free",
                    messages=[{"role": "user", "content": prompt}]
                )
            )

        generated_text = completion.choices[0].message.content
        print(f"✅ [FastAPI] Получен ответ от AI модели ({len(generated_text)} символов)")
        print(f"🔍 [FastAPI] Начинаем парсинг JSON ответа...")
        print(f"📄 [FastAPI] ПОЛНЫЙ JSON ОТВЕТ ОТ НЕЙРОСЕТИ:")
        print("=" * 80)
        print(generated_text)
        print("=" * 80)
        result = {}
        total_parsed = 0
        
        for cls in CLASSES:
            print(f"🔍 [FastAPI] Парсим квесты для класса: {cls}")
            try:
                json_match = re.search(rf'"{cls}":\s*\[(.*?)\]', generated_text, re.DOTALL)
                if json_match and json_match.group(1):
                    quests_str = f'{{"{cls}": [{json_match.group(1)}]}}'
                    try:
                        generated_json = json.loads(quests_str)
                        class_quests = generated_json.get(cls, [])
                        valid_quests = []
                        
                        print(f"📋 [FastAPI] Найдено {len(class_quests)} квестов для {cls}")
                        
                        for i, quest in enumerate(class_quests[:quest_count_per_class]):
                            try:
                                if validate_quest_structure(quest):
                                    quest["id"] = f"{cls}_{uuid.uuid4().hex[:8]}"
                                    valid_quests.append(quest)
                                    print(f"✅ [FastAPI] {cls} квест {i+1}: {quest.get('title', 'Без названия')}")
                                else:
                                    print(f"❌ [FastAPI] {cls} квест {i+1}: Не прошел валидацию")
                            except Exception as quest_error:
                                print(f"❌ [FastAPI] Ошибка при обработке квеста {i+1} для {cls}: {str(quest_error)}")
                        
                        result[cls] = valid_quests
                        total_parsed += len(valid_quests)
                        print(f"📊 [FastAPI] {cls}: {len(valid_quests)} валидных квестов")
                        
                    except json.JSONDecodeError as e:
                        print(f"❌ [FastAPI] Ошибка парсинга JSON для {cls}: {str(e)}")
                        print(f"🔍 [FastAPI] Проблемный JSON: {quests_str[:200]}...")
                        result[cls] = []
                else:
                    print(f"❌ [FastAPI] Не найден JSON блок для класса {cls}")
                    result[cls] = []
            except Exception as class_error:
                print(f"❌ [FastAPI] Критическая ошибка при обработке класса {cls}: {str(class_error)}")
                result[cls] = []

        if not any(result.values()):
            print(f"❌ [FastAPI] КРИТИЧЕСКАЯ ОШИБКА: Не удалось сгенерировать валидные квесты!")
            raise HTTPException(status_code=500, detail="Не удалось сгенерировать валидные квесты")

        end_time = time.time()
        execution_time = end_time - start_time
        print(f"⏱ [FastAPI] ИТОГО: {execution_time:.2f} секунд")
        print(f"🎯 [FastAPI] УСПЕШНО СГЕНЕРИРОВАНО: {total_parsed} квестов для всех классов")
        print(f"📤 [FastAPI] Отправляем ответ клиенту...")

        return AllQuestsResponse(**result)
    except Exception as e:
        print(f"Ошибка при генерации квестов: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Ошибка генерации квестов: {str(e)}")

@app.get("/quests/{player_class}", response_model=QuestsResponse)
async def get_quests(player_class: str, quest_count: int = 5):
    if player_class not in CLASSES:
        raise HTTPException(status_code=404, detail="Класс не найден")
    if quest_count < 1 or quest_count > 10:
        raise HTTPException(status_code=400, detail="Количество квестов должно быть от 1 до 10")

    start_time = time.time()
    print(f"🌐 [FastAPI] Начинаем запрос к OpenRouter для класса {player_class} ({quest_count} квестов)...")

    prompt = get_unified_quests_prompt(player_class=player_class, quest_count=quest_count)

    try:
        loop = asyncio.get_event_loop()
        with ThreadPoolExecutor(max_workers=2) as executor:
            completion = await loop.run_in_executor(
                executor,
                lambda: client.chat.completions.create(
                    extra_headers={
                        "HTTP-Referer": "http://localhost:8000",
                        "X-Title": f"Minecraft Quest Generator - {player_class}"
                    },
                    extra_body={},
                    model="deepseek-ai/DeepSeek-R1-0528",
                    messages=[{"role": "user", "content": prompt}]
                )
            )

        generated_text = completion.choices[0].message.content
        valid_quests = extract_valid_quests(generated_text)

        if not valid_quests:
            raise HTTPException(status_code=500, detail="Не удалось сгенерировать валидные квесты")

        valid_quests = valid_quests[:quest_count]

        end_time = time.time()
        execution_time = end_time - start_time
        print(f"⏱ {player_class} (deepseek/deepseek-r1-0528:free): {execution_time:.2f} секунд")

        return QuestsResponse(quests=valid_quests)
    except HTTPException as http_err:
        raise http_err
    except Exception as e:
        print(f"Ошибка при генерации квестов для {player_class}: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Ошибка генерации квестов: {str(e)}")