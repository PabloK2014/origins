#!/usr/bin/env python3
"""
Тест для проверки новой логики с отдельными запросами к API
"""

import asyncio
import aiohttp
import time
import json

API_BASE_URL = "http://localhost:8000"
CLASSES = ["cook", "courier", "brewer", "blacksmith", "miner", "warrior"]

async def test_single_class_request(session, player_class, quest_count=5):
    """Тестирует запрос квестов для одного класса"""
    url = f"{API_BASE_URL}/quests/{player_class}?quest_count={quest_count}"
    
    print(f"🚀 Отправляем запрос для класса {player_class}...")
    start_time = time.time()
    
    try:
        async with session.get(url) as response:
            end_time = time.time()
            execution_time = end_time - start_time
            
            if response.status == 200:
                data = await response.json()
                quest_count = len(data.get("quests", []))
                print(f"✅ {player_class}: {quest_count} квестов за {execution_time:.2f}s")
                return player_class, quest_count, execution_time, True
            else:
                error_text = await response.text()
                print(f"❌ {player_class}: Ошибка {response.status} за {execution_time:.2f}s")
                print(f"   Ошибка: {error_text[:100]}...")
                return player_class, 0, execution_time, False
                
    except Exception as e:
        end_time = time.time()
        execution_time = end_time - start_time
        print(f"🔥 {player_class}: Исключение за {execution_time:.2f}s - {str(e)}")
        return player_class, 0, execution_time, False

async def test_all_classes_separately():
    """Тестирует отдельные запросы для всех классов одновременно"""
    print("=" * 60)
    print("🌐 ТЕСТ: Отдельные асинхронные запросы для всех классов")
    print("=" * 60)
    
    start_time = time.time()
    
    async with aiohttp.ClientSession() as session:
        # Создаем задачи для всех классов
        tasks = []
        for player_class in CLASSES:
            task = test_single_class_request(session, player_class, 5)
            tasks.append(task)
        
        # Выполняем все запросы одновременно
        results = await asyncio.gather(*tasks)
    
    end_time = time.time()
    total_time = end_time - start_time
    
    # Анализируем результаты
    successful_classes = 0
    total_quests = 0
    failed_classes = []
    
    print("\n📊 РЕЗУЛЬТАТЫ:")
    print("-" * 40)
    
    for class_name, quest_count, exec_time, success in results:
        if success:
            successful_classes += 1
            total_quests += quest_count
        else:
            failed_classes.append(class_name)
    
    print(f"✅ Успешных классов: {successful_classes}/{len(CLASSES)}")
    print(f"🎯 Всего квестов: {total_quests}")
    print(f"⏱ Общее время: {total_time:.2f}s")
    
    if failed_classes:
        print(f"❌ Неудачные классы: {', '.join(failed_classes)}")
        
        # Тестируем повторные запросы для неудачных классов
        if failed_classes:
            print(f"\n🔄 Повторяем запросы для неудачных классов...")
            await test_retry_failed_classes(failed_classes)
    
    print("=" * 60)
    return successful_classes, total_quests, total_time

async def test_retry_failed_classes(failed_classes):
    """Тестирует повторные запросы для неудачных классов"""
    print("🔄 ПОВТОРНЫЕ ЗАПРОСЫ:")
    print("-" * 30)
    
    async with aiohttp.ClientSession() as session:
        retry_tasks = []
        for player_class in failed_classes:
            task = test_single_class_request(session, player_class, 5)
            retry_tasks.append(task)
        
        retry_results = await asyncio.gather(*retry_tasks)
    
    retry_successful = 0
    retry_total_quests = 0
    
    for class_name, quest_count, exec_time, success in retry_results:
        if success:
            retry_successful += 1
            retry_total_quests += quest_count
    
    print(f"✅ Повторно успешных: {retry_successful}/{len(failed_classes)}")
    print(f"🎯 Квестов при повторе: {retry_total_quests}")

async def test_api_health():
    """Проверяет доступность API"""
    try:
        async with aiohttp.ClientSession() as session:
            async with session.get(f"{API_BASE_URL}/") as response:
                if response.status == 200:
                    print("✅ API сервер доступен")
                    return True
                else:
                    print(f"❌ API сервер недоступен (статус: {response.status})")
                    return False
    except Exception as e:
        print(f"🔥 Ошибка подключения к API: {str(e)}")
        return False

async def main():
    """Основная функция тестирования"""
    print("🧪 ТЕСТИРОВАНИЕ НОВОЙ ЛОГИКИ С ОТДЕЛЬНЫМИ ЗАПРОСАМИ")
    print("=" * 60)
    
    # Проверяем доступность API
    if not await test_api_health():
        print("❌ API недоступен, тестирование невозможно")
        return
    
    # Тестируем отдельные запросы
    successful, total_quests, total_time = await test_all_classes_separately()
    
    # Выводим итоговую статистику
    print("\n🎯 ИТОГОВАЯ СТАТИСТИКА:")
    print("=" * 40)
    print(f"Успешность: {successful}/{len(CLASSES)} классов ({successful/len(CLASSES)*100:.1f}%)")
    print(f"Всего квестов: {total_quests}")
    print(f"Среднее время на класс: {total_time/len(CLASSES):.2f}s")
    print(f"Общее время: {total_time:.2f}s")
    
    if successful == len(CLASSES):
        print("🎉 ВСЕ ТЕСТЫ ПРОШЛИ УСПЕШНО!")
    else:
        print("⚠️ Некоторые тесты не прошли")

if __name__ == "__main__":
    asyncio.run(main())