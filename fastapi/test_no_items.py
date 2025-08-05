#!/usr/bin/env python3
"""
Тест системы квестов без предметов - только опыт
"""

import requests
import json

def test_quest_generation_no_items():
    """Тестирует генерацию квестов без предметов в наградах"""
    
    print("🧪 [TEST] Тестируем систему квестов без предметов...")
    
    try:
        # Тестируем генерацию для всех классов
        response = requests.get("http://localhost:8000/quests/all?quest_count_per_class=1")
        
        if response.status_code == 200:
            data = response.json()
            print("✅ [TEST] Успешно получены квесты!")
            
            classes = ["cook", "courier", "brewer", "blacksmith", "miner", "warrior"]
            all_valid = True
            
            for cls in classes:
                quests = data.get(cls, [])
                if len(quests) > 0:
                    quest = quests[0]
                    reward = quest.get("reward", {})
                    
                    print(f"📋 [TEST] {cls}:")
                    print(f"   📝 Название: {quest.get('title', 'Нет названия')}")
                    print(f"   🎯 Цель: {quest.get('objective', {}).get('type', 'Нет типа')} - {quest.get('objective', {}).get('target', 'Нет цели')}")
                    print(f"   🏆 Награда: tier {reward.get('tier', 0)}, {reward.get('experience', 0)} опыта")
                    
                    # Проверяем, что нет поля items
                    if "items" in reward:
                        print(f"   ❌ ОШИБКА: Найдены предметы в награде!")
                        all_valid = False
                    else:
                        print(f"   ✅ Награда содержит только опыт")
                        
                    # Проверяем обязательные поля
                    required_reward_fields = ["type", "tier", "experience"]
                    missing_fields = [field for field in required_reward_fields if field not in reward]
                    if missing_fields:
                        print(f"   ❌ ОШИБКА: Отсутствуют поля в reward: {missing_fields}")
                        all_valid = False
                    else:
                        print(f"   ✅ Все обязательные поля присутствуют")
                else:
                    print(f"❌ [TEST] Нет квестов для класса {cls}")
                    all_valid = False
            
            if all_valid:
                print("🎉 [TEST] ВСЕ ТЕСТЫ ПРОЙДЕНЫ! Система работает без предметов.")
                return True
            else:
                print("⚠️ [TEST] НЕКОТОРЫЕ ТЕСТЫ НЕ ПРОЙДЕНЫ.")
                return False
                
        else:
            print(f"❌ [TEST] ОШИБКА HTTP: {response.status_code}")
            print(f"📄 [TEST] Ответ: {response.text}")
            return False
            
    except Exception as e:
        print(f"🔥 [TEST] КРИТИЧЕСКАЯ ОШИБКА: {str(e)}")
        return False

def test_fallback_quests():
    """Тестирует fallback квесты"""
    
    print("\n🧪 [TEST] Тестируем fallback квесты...")
    
    # Импортируем функцию из main.py
    import sys
    sys.path.append('.')
    from main import get_fallback_quests
    
    fallback_quests = get_fallback_quests()
    
    all_valid = True
    for cls, quests in fallback_quests.items():
        if len(quests) > 0:
            quest = quests[0]
            reward = quest.get("reward", {})
            
            print(f"📋 [TEST] Fallback {cls}:")
            print(f"   📝 Название: {quest.get('title', 'Нет названия')}")
            print(f"   🏆 Награда: tier {reward.get('tier', 0)}, {reward.get('experience', 0)} опыта")
            
            # Проверяем, что нет поля items
            if "items" in reward:
                print(f"   ❌ ОШИБКА: Найдены предметы в fallback награде!")
                all_valid = False
            else:
                print(f"   ✅ Fallback награда содержит только опыт")
    
    if all_valid:
        print("✅ [TEST] Fallback квесты корректны!")
        return True
    else:
        print("❌ [TEST] Fallback квесты содержат ошибки!")
        return False

if __name__ == "__main__":
    print("🚀 [TEST] Запуск тестирования системы без предметов")
    print("=" * 60)
    
    # Проверяем доступность сервера
    try:
        health_response = requests.get("http://localhost:8000/")
        if health_response.status_code != 200:
            print("❌ [TEST] Сервер не запущен! Запустите FastAPI сервер сначала.")
            exit(1)
    except:
        print("❌ [TEST] Не удается подключиться к серверу! Запустите FastAPI сервер.")
        exit(1)
    
    print("✅ [TEST] Сервер доступен, начинаем тестирование...")
    
    # Тестируем генерацию квестов
    generation_success = test_quest_generation_no_items()
    
    # Тестируем fallback квесты
    fallback_success = test_fallback_quests()
    
    print("\n" + "=" * 60)
    print("📊 [TEST] РЕЗУЛЬТАТЫ ТЕСТИРОВАНИЯ:")
    print(f"   Генерация квестов: {'✅ ПРОЙДЕН' if generation_success else '❌ НЕ ПРОЙДЕН'}")
    print(f"   Fallback квесты: {'✅ ПРОЙДЕН' if fallback_success else '❌ НЕ ПРОЙДЕН'}")
    
    if generation_success and fallback_success:
        print("🎉 [TEST] ВСЕ ТЕСТЫ ПРОЙДЕНЫ! Система квестов работает только с опытом.")
    else:
        print("⚠️ [TEST] НЕКОТОРЫЕ ТЕСТЫ НЕ ПРОЙДЕНЫ. Требуется дополнительная настройка.")