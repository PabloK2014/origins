from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import OpenAI
import json
import re
import uuid
from typing import List, Dict

app = FastAPI()

# Конфигурация OpenRouter API для DeepSeek
OPENROUTER_API_KEY = "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"
client = OpenAI(
    base_url="https://openrouter.ai/api/v1",
    api_key=OPENROUTER_API_KEY,
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

# Модель для входных данных запроса
class QuestRequest(BaseModel):
    player_class: str
    quest_count: int = 5  # По умолчанию 5 квестов

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

# Промпт для DeepSeek с экранированными фигурными скобками
DEEPSEEK_PROMPT = """
Generate {quest_count} quests for the Minecraft class "{player_class}" in JSON format, compatible with the following structure. Ensure the quests are unique, have a level from 1 to 3, and use only the provided objective types and targets. The title and description must be in Russian, creative, and match the class theme. Do not include a separate localization file; title and description are in the JSON. Use the provided template and valid values. Return only the JSON, without any additional text or formatting.

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

Valid objective types: {objective_types}
Valid targets: {targets}
"""

@app.get("/quests/{player_class}", response_model=QuestsResponse)
async def get_quests(player_class: str, quest_count: int = 5):
    """Генерирует и возвращает квесты для указанного класса."""
    if player_class not in CLASSES:
        raise HTTPException(status_code=404, detail="Класс не найден")
    if quest_count < 1 or quest_count > 10:
        raise HTTPException(status_code=400, detail="Количество квестов должно быть от 1 до 10")

    prompt = DEEPSEEK_PROMPT.format(
        quest_count=quest_count,
        player_class=player_class,
        objective_types=", ".join(OBJECTIVE_TYPES[player_class]),
        targets=", ".join(TARGETS[player_class])
    )

    try:
        completion = client.chat.completions.create(
            extra_headers={
                "HTTP-Referer": "http://localhost:8000",
                "X-Title": "Minecraft Quest Generator"
            },
            extra_body={},
            model="deepseek/deepseek-chat-v3-0324:free",
            messages=[
                {
                    "role": "user",
                    "content": prompt
                }
            ]
        )

        generated_text = completion.choices[0].message.content
        # Логируем ответ для отладки
        print(f"Raw response from DeepSeek: {generated_text}")

        # Попытка извлечь JSON из ответа
        json_match = re.search(r'\{.*\}', generated_text, re.DOTALL)
        if not json_match:
            raise HTTPException(status_code=500, detail="Ответ нейросети не содержит валидный JSON")

        try:
            generated_json = json.loads(json_match.group(0))
        except json.JSONDecodeError as e:
            print(f"JSON parse error: {str(e)}")
            raise HTTPException(status_code=500, detail=f"Ошибка парсинга JSON: {str(e)}")

        quests = generated_json.get("quests", [])
        if not quests:
            raise HTTPException(status_code=500, detail="В ответе нейросети отсутствует поле 'quests'")

        # Валидация и добавление уникальных ID
        valid_quests = []
        for quest in quests[:quest_count]:
            if all(
                key in quest
                for key in ["id", "playerClass", "level", "title", "description", "objective", "timeLimit", "reward"]
            ):
                # Проверка вложенных структур
                if all(
                    key in quest["objective"]
                    for key in ["type", "target", "amount"]
                ) and all(
                    key in quest["reward"]
                    for key in ["type", "tier", "experience"]
                ):
                    quest["id"] = f"{player_class}_{uuid.uuid4().hex[:8]}"  # Уникальный ID
                    valid_quests.append(quest)
                else:
                    print(f"Пропущен квест с неверной структурой: {quest}")
            else:
                print(f"Пропущен квест с недостаточными полями: {quest}")

        if not valid_quests:
            raise HTTPException(status_code=500, detail="Не удалось сгенерировать валидные квесты")

        return QuestsResponse(quests=valid_quests)
    except Exception as e:
        print(f"Ошибка при запросе к нейросети: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Ошибка генерации квестов: {str(e)}")