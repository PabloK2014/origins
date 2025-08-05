#!/usr/bin/env python3
"""
Тестовый скрипт для проверки эндпоинта /chat/ask
"""

import requests
import json

def test_chat_endpoint():
    url = "http://localhost:8000/chat/ask"
    
    # Тестовые данные
    test_data = {
        "question": "Как сделать палки?",
        "minecraft_version": "1.20.1"
    }
    
    print(f"🧪 Тестируем эндпоинт: {url}")
    print(f"📤 Отправляем данные: {json.dumps(test_data, ensure_ascii=False, indent=2)}")
    
    try:
        response = requests.post(url, json=test_data, timeout=30)
        
        print(f"📡 Статус ответа: {response.status_code}")
        print(f"📥 Заголовки ответа: {dict(response.headers)}")
        
        if response.status_code == 200:
            result = response.json()
            print(f"✅ Успешный ответ:")
            print(f"   Ответ: {result.get('answer', 'N/A')[:100]}...")
            print(f"   Успех: {result.get('success', 'N/A')}")
            print(f"   Тип: {result.get('response_type', 'N/A')}")
        else:
            print(f"❌ Ошибка {response.status_code}:")
            print(f"   Тело ответа: {response.text}")
            
    except requests.exceptions.RequestException as e:
        print(f"🔥 Ошибка запроса: {e}")
    except Exception as e:
        print(f"🔥 Неожиданная ошибка: {e}")

if __name__ == "__main__":
    test_chat_endpoint()