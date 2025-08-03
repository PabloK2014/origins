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

# Добавление простого health check эндпоинта
@app.get("/")
async def health_check():
    return {"status": "ok"}


# Конфигурация OpenRouter API
API_KEY = "io-v2-eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJvd25lciI6IjE4N2Y1MzRlLWRlY2ItNDNjZi1iMzA4LTRiNmQzMWZmMmQ2YyIsImV4cCI6NDkwNzg1MTg0OH0.JrmfmF_vviGoN5JCKE1rjCcdrLWgLFXS7zZ7CXJAfJDZebqX9bx1eDQ0A2EbTFQbU7Y2UOkG2Awshf4HVHxoyw"
client = OpenAI(
    base_url="https://api.intelligence.io.solutions/api/v1/",
    api_key=API_KEY,
)

# Классы игроков и возможные цели
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

# Модель для ответа API
class Quest(BaseModel):
    id: str
    playerClass: str
    level: int
    title: str
    description: str
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

# Промпт для генерации всех квестов
@lru_cache(maxsize=1)
def get_all_quests_prompt(quest_count_per_class: int = 5) -> str:
    return f"""
Generate {quest_count_per_class} quests for EACH of the following Minecraft classes: {', '.join(CLASSES)}.
Return a JSON with separate arrays for each class. Each quest must have a unique ID, be in Russian, and strictly follow the structure below. Use ONLY the provided objective types and targets for each class, and NEVER use 'minecraft:air' or any invalid targets.

Return format:
{{
  {', '.join([f'"{cls}": [{quest_count_per_class} quests for {cls} class]' for cls in CLASSES])}
}}

Quest structure for each:
{{
  "id": "class_target_level",
  "playerClass": "origins:class_name",
  "level": <1-3>,
  "title": "<Russian title>",
  "description": "<Russian description>",
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

# Промпт для DeepSeek
@lru_cache(maxsize=1)
def get_quests_prompt(player_class: str, quest_count: int) -> str:
    return f"""
Generate {quest_count} quests for the Minecraft class "{player_class}" in JSON format, compatible with the following structure. Ensure the quests are unique, have a level from 1 to 3, and use ONLY the provided objective types and targets. The title and description must be in Russian, creative, and match the class theme. Do not include a separate localization file; title and description are in the JSON. Use the provided template and valid values. NEVER use 'minecraft:air' or any invalid targets.

Template:
{{
  "quests": [
    {{
      "id": "class_target_level",
      "playerClass": "origins:{player_class}",
      "level": <1-3>,
      "title": "<Russian title>",
      "description": "<Russian description>",
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

def validate_quest_structure(quest):
    """Валидирует структуру квеста"""
    required_fields = ["id", "playerClass", "level", "title", "description", "objective", "timeLimit", "reward"]
    if not all(key in quest for key in required_fields):
        return False
    if not all(key in quest["objective"] for key in ["type", "target", "amount"]):
        return False
    if not all(key in quest["reward"] for key in ["type", "tier", "experience"]):
        return False
    # Проверка, что target не "minecraft:air" и входит в список TARGETS для класса
    player_class = quest["playerClass"].split(":")[1]
    target = quest["objective"]["target"].replace("minecraft:", "")
    if target == "air" or target not in TARGETS.get(player_class, []):
        return False
    return True

# Функция для обработки частично повреждённого JSON
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

# Эндпоинт для получения всех квестов
@app.get("/quests/all", response_model=AllQuestsResponse)
async def get_all_quests(quest_count_per_class: int = 5):
    """Генерирует квесты для всех классов за один запрос с кэшированием."""
    if quest_count_per_class < 1 or quest_count_per_class > 10:
        raise HTTPException(status_code=400, detail="Количество квестов на класс должно быть от 1 до 10")

    start_time = time.time()
    print(f"🌐 [FastAPI] Начинаем запрос к OpenRouter для генерации {quest_count_per_class} квестов на класс...")

    prompt = get_all_quests_prompt(quest_count_per_class)

    try:
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
                    model="deepseek-ai/DeepSeek-R1-0528",
                    messages=[{"role": "user", "content": prompt}]
                )
            )

        generated_text = completion.choices[0].message.content
        result = {}
        for cls in CLASSES:
            json_match = re.search(rf'"{cls}":\s*\[(.*?)\]', generated_text, re.DOTALL)
            if json_match:
                quests_str = f'{{"{cls}": [{json_match.group(1)}]}}'
                try:
                    generated_json = json.loads(quests_str)
                    class_quests = generated_json.get(cls, [])
                    valid_quests = []
                    for quest in class_quests[:quest_count_per_class]:
                        if validate_quest_structure(quest):
                            quest["id"] = f"{cls}_{uuid.uuid4().hex[:8]}"
                            valid_quests.append(quest)
                    result[cls] = valid_quests
                except json.JSONDecodeError:
                    result[cls] = []
            else:
                result[cls] = []

        if not any(result.values()):
            raise HTTPException(status_code=500, detail="Не удалось сгенерировать валидные квесты")

        end_time = time.time()
        execution_time = end_time - start_time
        print(f"⏱ Все квесты (deepseek/deepseek-r1-0528:free): {execution_time:.2f} секунд")

        return AllQuestsResponse(**result)
    except Exception as e:
        print(f"Ошибка при генерации квестов: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Ошибка генерации квестов: {str(e)}")

# Эндпоинт для асинхронной генерации квестов
@app.get("/quests/{player_class}", response_model=QuestsResponse)
async def get_quests(player_class: str, quest_count: int = 5):
    """Генерирует квесты для одного класса асинхронно с обработкой ошибок."""
    if player_class not in CLASSES:
        raise HTTPException(status_code=404, detail="Класс не найден")
    if quest_count < 1 or quest_count > 10:
        raise HTTPException(status_code=400, detail="Количество квестов должно быть от 1 до 10")

    start_time = time.time()
    print(f"🌐 [FastAPI] Начинаем запрос к OpenRouter для класса {player_class} ({quest_count} квестов)...")

    prompt = get_quests_prompt(player_class, quest_count)

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