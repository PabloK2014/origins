package io.github.apace100.origins.quest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.apace100.origins.Origins;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP клиент для взаимодействия с чат-помощником AI API
 */
public class ChatAssistantApiClient {
    private static final String API_BASE_URL = "http://localhost:8000";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .version(HttpClient.Version.HTTP_1_1) // Принудительно используем HTTP/1.1
            .build();
    private static final Gson gson = new Gson();
    
    /**
     * Отправляет вопрос к AI и получает ответ
     */
    public static CompletableFuture<ChatResponse> askQuestion(String question, String minecraftVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = API_BASE_URL + "/chat/ask";
                                
                // Создаем JSON запрос с правильными именами полей для FastAPI
                JsonObject requestJson = new JsonObject();
                requestJson.addProperty("question", question);
                requestJson.addProperty("minecraft_version", minecraftVersion);
                // context поле опциональное, не добавляем если null
                String jsonBody = gson.toJson(requestJson);
                
                                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(60)) // Таймаут 1 минута
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                
                                
                if (response.statusCode() == 200) {
                    ChatResponse chatResponse = parseChatResponse(response.body());
                                        return chatResponse;
                } else {
                    Origins.LOGGER.error("❌ [ChatAssistant] API ERROR: Status " + response.statusCode());
                    Origins.LOGGER.error("Response body: " + response.body());
                    
                    if (response.statusCode() == 422) {
                        Origins.LOGGER.error("🔥 [ChatAssistant] Validation error - check JSON structure");
                        Origins.LOGGER.error("Sent JSON: " + jsonBody);
                    }
                    
                    return new ChatResponse("Извините, произошла ошибка при обращении к AI. Попробуйте позже.", false, "API Error: " + response.statusCode(), "error");
                }
                
            } catch (IOException | InterruptedException e) {
                Origins.LOGGER.error("🔥 [ChatAssistant] API EXCEPTION: " + e.getMessage());
                return new ChatResponse("Извините, не удалось связаться с AI сервисом. Проверьте подключение.", false, e.getMessage(), "error");
            }
        });
    }
    
    /**
     * Парсит JSON ответ от API в объект ChatResponse
     */
    private static ChatResponse parseChatResponse(String jsonResponse) {
        try {
            JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            String answer = responseObj.get("answer").getAsString();
            boolean success = responseObj.get("success").getAsBoolean();
            String errorMessage = responseObj.has("error_message") && !responseObj.get("error_message").isJsonNull() 
                ? responseObj.get("error_message").getAsString() : null;
            String responseType = responseObj.has("response_type") 
                ? responseObj.get("response_type").getAsString() : "general";
            
            return new ChatResponse(answer, success, errorMessage, responseType);
            
        } catch (Exception e) {
            Origins.LOGGER.error("🔥 [ChatAssistant] Ошибка при парсинге ответа от API", e);
            return new ChatResponse("Извините, произошла ошибка при обработке ответа.", false, e.getMessage(), "error");
        }
    }
    
    /**
     * Проверяет доступность Chat API
     */
    public static CompletableFuture<Boolean> isChatApiAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/"))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                boolean isAvailable = response.statusCode() == 200;
                                
                return isAvailable;
                
            } catch (Exception e) {
                Origins.LOGGER.warn("🔍 [ChatAssistant] API Health Check: ❌ EXCEPTION - " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Класс для запроса к чат API
     */
    public static class ChatRequest {
        public final String question;
        public final String minecraft_version;
        public final String context;
        
        public ChatRequest(String question, String minecraftVersion, String context) {
            this.question = question;
            this.minecraft_version = minecraftVersion;
            this.context = context;
        }
    }
    
    /**
     * Класс для ответа от чат API
     */
    public static class ChatResponse {
        public final String answer;
        public final boolean success;
        public final String errorMessage;
        public final String responseType;
        
        public ChatResponse(String answer, boolean success, String errorMessage, String responseType) {
            this.answer = answer;
            this.success = success;
            this.errorMessage = errorMessage;
            this.responseType = responseType;
        }
    }
}