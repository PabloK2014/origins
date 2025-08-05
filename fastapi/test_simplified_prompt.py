#!/usr/bin/env python3
"""
Тестовый скрипт для проверки упрощенного промпта генерации квестов
"""

import requests
import json
import time

def test_quest_generation():
    """Тестирует генерацию квестов с новым упрощенным промптом"""
    
    print("🧪 [TEST] Начинаем тестирование упрощенного промпта...")
    
    # Тестируем генерацию квестов для всех классов
    try:
        print("📡 [TEST] Отправляем запрос на /quests/all...")
        start_time = time.time()
        
        response = requests.get("http://localhost:8000/quests/all?quest_count_per_class=1")
        
        end_time = time.time()
        execution_time = end_time - start_time
        
        print(f"⏱ [TEST] Время выполнения: {execution_time:.2f} секунд")
        print(f"📊 [TEST] Статус ответа: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print("✅ [TEST] Успешно получены квесты!")
            
            # Проверяем каждый класс
            classes = ["cook", "courier", "brewer", "blacksmith", "miner", "warrior"]
            total_quests = 0
            
            for cls in classes:
                quests = data.get(cls, [])
                quest_count = len(quests)
                total_quests += quest_count
                
                print(f"📋 [TEST] {cls}: {quest_count} квестов")
                
                if quest_count > 0:
                    quest = quests[0]
                    print(f"   📝 Название: {quest.get('title', 'Нет названия')}")
                    print(f"   🎯 Цель: {quest.get('objective', {}).get('type', 'Нет типа')} - {quest.get('objective', {}).get('target', 'Нет цели')}")
                    print(f"   🏆 Награда: {quest.get('reward', {}).get('experience', 0)} опыта")
                else:
                    print(f"   ❌ Нет квестов для класса {cls}")
            
            print(f"🎯 [TEST] ИТОГО: {total_quests} квестов сгенерировано")
            
            if total_quests >= 6:  # По одному квесту на класс
                print("🎉 [TEST] ТЕСТ ПРОЙДЕН! Все классы имеют квесты")
                return True
            else:
                print("⚠️ [TEST] ТЕСТ ЧАСТИЧНО ПРОЙДЕН: Не все классы имеют квесты")
                return False
                
        else:
            print(f"❌ [TEST] ОШИБКА: {response.status_code}")
            print(f"📄 [TEST] Ответ: {response.text}")
            return False
            
    except Exception as e:
        print(f"🔥 [TEST] КРИТИЧЕСКАЯ ОШИБКА: {str(e)}")
        return False

def test_single_class():
    """Тестирует генерацию квестов для одного класса"""
    
    print("\n🧪 [TEST] Тестируем генерацию для одного класса (cook)...")
    
    try:
        response = requests.get("http://localhost:8000/quests/cook?quest_count=1")
        
        if response.status_code == 200:
            data = response.json()
            quests = data.get("quests", [])
            
            if len(quests) > 0:
                quest = quests[0]
                print(f"✅ [TEST] Квест для cook сгенерирован:")
                print(f"   📝 Название: {quest.get('title', 'Нет названия')}")
                print(f"   🎯 Цель: {quest.get('objective', {}).get('type', 'Нет типа')} - {quest.get('objective', {}).get('target', 'Нет цели')}")
                return True
            else:
                print("❌ [TEST] Квест не сгенерирован")
                return False
        else:
            print(f"❌ [TEST] ОШИБКА: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"🔥 [TEST] ОШИБКА: {str(e)}")
        return False

if __name__ == "__main__":
    print("🚀 [TEST] Запуск тестирования упрощенного промпта")
    print("=" * 60)
    
    # Проверяем, что сервер запущен
    try:
        health_response = requests.get("http://localhost:8000/")
        if health_response.status_code != 200:
            print("❌ [TEST] Сервер не запущен! Запустите FastAPI сервер сначала.")
            exit(1)
    except:
        print("❌ [TEST] Не удается подключиться к серверу! Запустите FastAPI сервер.")
        exit(1)
    
    print("✅ [TEST] Сервер доступен, начинаем тестирование...")
    
    # Тестируем генерацию для всех классов
    all_classes_success = test_quest_generation()
    
    # Тестируем генерацию для одного класса
    single_class_success = test_single_class()
    
    print("\n" + "=" * 60)
    print("📊 [TEST] РЕЗУЛЬТАТЫ ТЕСТИРОВАНИЯ:")
    print(f"   Все классы: {'✅ ПРОЙДЕН' if all_classes_success else '❌ НЕ ПРОЙДЕН'}")
    print(f"   Один класс: {'✅ ПРОЙДЕН' if single_class_success else '❌ НЕ ПРОЙДЕН'}")
    
    if all_classes_success and single_class_success:
        print("🎉 [TEST] ВСЕ ТЕСТЫ ПРОЙДЕНЫ! Упрощенный промпт работает корректно.")
    else:
        print("⚠️ [TEST] НЕКОТОРЫЕ ТЕСТЫ НЕ ПРОЙДЕНЫ. Требуется дополнительная настройка.")