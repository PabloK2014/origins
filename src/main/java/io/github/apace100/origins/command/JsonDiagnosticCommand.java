package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.util.JsonDiagnostic;
import io.github.apace100.origins.util.KeybindingDiagnostic;
import io.github.apace100.origins.util.TextureValidator;
import io.github.apace100.origins.util.DiagnosticReporter;
import io.github.apace100.origins.util.JsonDiagnosticTest;
import io.github.apace100.origins.util.KeybindingTest;
import io.github.apace100.origins.util.TextureTest;
import io.github.apace100.origins.util.OngoingValidationService;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Command for running comprehensive diagnostic checks on the Origins mod
 */
public class JsonDiagnosticCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("origins")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("diagnostic")
                .then(CommandManager.literal("json")
                    .executes(JsonDiagnosticCommand::runJsonDiagnostic)
                    .then(CommandManager.literal("origins")
                        .executes(JsonDiagnosticCommand::runOriginsJsonDiagnostic)))
                .then(CommandManager.literal("textures")
                    .executes(JsonDiagnosticCommand::runTextureDiagnostic))
                .then(CommandManager.literal("keybindings")
                    .executes(JsonDiagnosticCommand::runKeybindingDiagnostic))
                .then(CommandManager.literal("all")
                    .executes(JsonDiagnosticCommand::runFullDiagnostic))
                .then(CommandManager.literal("report")
                    .executes(JsonDiagnosticCommand::generateDiagnosticReport))
                .then(CommandManager.literal("quick")
                    .executes(JsonDiagnosticCommand::runQuickDiagnostic))
                .then(CommandManager.literal("validation")
                    .then(CommandManager.literal("status")
                        .executes(JsonDiagnosticCommand::getValidationStatus))
                    .then(CommandManager.literal("force")
                        .executes(JsonDiagnosticCommand::forceValidation)))
                .then(CommandManager.literal("test")
                    .then(CommandManager.literal("json")
                        .executes(JsonDiagnosticCommand::runJsonTests))
                    .then(CommandManager.literal("keybindings")
                        .executes(JsonDiagnosticCommand::runKeybindingTests))
                    .then(CommandManager.literal("textures")
                        .executes(JsonDiagnosticCommand::runTextureTests))
                    .then(CommandManager.literal("create")
                        .executes(JsonDiagnosticCommand::createTestFiles)))
            )
        );
    }
    
    private static int runJsonDiagnostic(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Запуск диагностики JSON файлов..."), false);
        
        try {
            // Запускаем диагностику JSON в отдельном потоке
            new Thread(() -> {
                JsonDiagnostic.runFullDiagnostic();
                source.sendFeedback(() -> Text.literal("Диагностика JSON завершена. Проверьте логи для деталей."), false);
            }).start();
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при запуске диагностики JSON: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при запуске диагностики JSON: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int runOriginsJsonDiagnostic(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Запуск диагностики Origins JSON файлов..."), false);
        
        try {
            // Запускаем Origins-специфичную диагностику JSON в отдельном потоке
            new Thread(() -> {
                JsonDiagnostic.runOriginsJsonDiagnostic();
                source.sendFeedback(() -> Text.literal("Диагностика Origins JSON завершена. Проверьте логи для деталей."), false);
            }).start();
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при запуске диагностики Origins JSON: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при запуске диагностики Origins JSON: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int runTextureDiagnostic(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Запуск диагностики текстур..."), false);
        
        try {
            TextureValidator.ValidationReport report = TextureValidator.validateAllTextures();
            
            source.sendFeedback(() -> Text.literal("Диагностика текстур завершена:"), false);
            source.sendFeedback(() -> Text.literal("- Валидных текстур: " + report.validTextures.size()), false);
            source.sendFeedback(() -> Text.literal("- Невалидных текстур: " + report.invalidTextures.size()), false);
            source.sendFeedback(() -> Text.literal("- Использовано fallback: " + report.fallbacksUsed.size()), false);
            
            if (report.hasIssues()) {
                source.sendFeedback(() -> Text.literal("Найдены проблемы с текстурами. Проверьте логи для деталей."), false);
            }
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при запуске диагностики текстур: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при запуске диагностики текстур: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int runKeybindingDiagnostic(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Запуск диагностики клавиш..."), false);
        
        try {
            KeybindingDiagnostic.DiagnosticReport report = KeybindingDiagnostic.runDiagnostic();
            
            source.sendFeedback(() -> Text.literal("Диагностика клавиш завершена:"), false);
            source.sendFeedback(() -> Text.literal("- Всего клавиш найдено: " + report.registeredKeybindings.size()), false);
            source.sendFeedback(() -> Text.literal("- Origins клавиш найдено: " + report.originsKeybindings.size()), false);
            source.sendFeedback(() -> Text.literal("- Конфликтов обнаружено: " + report.conflicts.size()), false);
            source.sendFeedback(() -> Text.literal("- Проблем найдено: " + report.issues.size()), false);
            
            if (report.hasIssues()) {
                source.sendFeedback(() -> Text.literal("Найдены проблемы с клавишами. Проверьте логи для деталей."), false);
            }
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при запуске диагностики клавиш: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при запуске диагностики клавиш: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int runFullDiagnostic(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Запуск полной диагностики Origins..."), false);
        
        try {
            // Запускаем все диагностики в отдельном потоке
            new Thread(() -> {
                try {
                    // JSON диагностика
                    source.sendFeedback(() -> Text.literal("1/3 Проверка JSON файлов..."), false);
                    JsonDiagnostic.runFullDiagnostic();
                    
                    // Диагностика текстур
                    source.sendFeedback(() -> Text.literal("2/3 Проверка текстур..."), false);
                    TextureValidator.ValidationReport textureReport = TextureValidator.validateAllTextures();
                    
                    // Диагностика клавиш
                    source.sendFeedback(() -> Text.literal("3/3 Проверка клавиш..."), false);
                    KeybindingDiagnostic.DiagnosticReport keybindingReport = KeybindingDiagnostic.runDiagnostic();
                    
                    // Итоговый отчет
                    source.sendFeedback(() -> Text.literal("=== ИТОГОВЫЙ ОТЧЕТ ДИАГНОСТИКИ ==="), false);
                    source.sendFeedback(() -> Text.literal("Текстуры: " + textureReport.validTextures.size() + " OK, " + 
                                                          textureReport.invalidTextures.size() + " проблем"), false);
                    source.sendFeedback(() -> Text.literal("Клавиши: " + keybindingReport.originsKeybindings.size() + " найдено, " + 
                                                          keybindingReport.conflicts.size() + " конфликтов"), false);
                    
                    boolean hasIssues = textureReport.hasIssues() || keybindingReport.hasIssues();
                    if (hasIssues) {
                        source.sendFeedback(() -> Text.literal("ВНИМАНИЕ: Обнаружены проблемы! Проверьте логи для деталей."), false);
                    } else {
                        source.sendFeedback(() -> Text.literal("Все проверки пройдены успешно!"), false);
                    }
                    
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при полной диагностике: " + e.getMessage(), e);
                    source.sendError(Text.literal("Ошибка при полной диагностике: " + e.getMessage()));
                }
            }).start();
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при запуске полной диагностики: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при запуске полной диагностики: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int generateDiagnosticReport(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Генерация полного диагностического отчета..."), false);
        
        try {
            // Генерируем отчет в отдельном потоке
            new Thread(() -> {
                try {
                    DiagnosticReporter.DiagnosticReport report = DiagnosticReporter.generateReport();
                    
                    // Сохраняем отчет в файлы
                    var jsonFile = DiagnosticReporter.saveReportToFile(report);
                    var textFile = DiagnosticReporter.saveReportAsText(report);
                    
                    source.sendFeedback(() -> Text.literal("Диагностический отчет сгенерирован:"), false);
                    if (jsonFile != null) {
                        source.sendFeedback(() -> Text.literal("- JSON отчет: " + jsonFile.getFileName()), false);
                    }
                    if (textFile != null) {
                        source.sendFeedback(() -> Text.literal("- Текстовый отчет: " + textFile.getFileName()), false);
                    }
                    
                    // Краткая сводка
                    if (report.hasCriticalIssues()) {
                        source.sendFeedback(() -> Text.literal("КРИТИЧЕСКИЕ ПРОБЛЕМЫ ОБНАРУЖЕНЫ!"), false);
                    } else if (report.hasIssues()) {
                        source.sendFeedback(() -> Text.literal("Обнаружены незначительные проблемы"), false);
                    } else {
                        source.sendFeedback(() -> Text.literal("Все системы работают нормально"), false);
                    }
                    
                    // Показываем рекомендации
                    if (!report.recommendations.isEmpty()) {
                        source.sendFeedback(() -> Text.literal("Рекомендации:"), false);
                        for (String recommendation : report.recommendations) {
                            source.sendFeedback(() -> Text.literal("- " + recommendation), false);
                        }
                    }
                    
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при генерации отчета: " + e.getMessage(), e);
                    source.sendError(Text.literal("Ошибка при генерации отчета: " + e.getMessage()));
                }
            }).start();
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при запуске генерации отчета: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при запуске генерации отчета: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int runQuickDiagnostic(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Запуск быстрой диагностики..."), false);
        
        try {
            String summary = DiagnosticReporter.getQuickDiagnosticSummary();
            
            // Разбиваем сводку на строки и отправляем каждую отдельно
            String[] lines = summary.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    source.sendFeedback(() -> Text.literal(line), false);
                }
            }
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при быстрой диагностике: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при быстрой диагностике: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int runJsonTests(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Запуск тестов JSON диагностики..."), false);
        
        try {
            // Запускаем тесты в отдельном потоке
            new Thread(() -> {
                try {
                    JsonDiagnosticTest.runTests();
                    source.sendFeedback(() -> Text.literal("Тесты JSON диагностики завершены. Проверьте логи для деталей."), false);
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при выполнении тестов JSON: " + e.getMessage(), e);
                    source.sendError(Text.literal("Ошибка при выполнении тестов JSON: " + e.getMessage()));
                }
            }).start();
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при запуске тестов JSON: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при запуске тестов JSON: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int createTestFiles(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Создание тестовых JSON файлов..."), false);
        
        try {
            JsonDiagnosticTest.createTestFiles();
            source.sendFeedback(() -> Text.literal("Тестовые файлы созданы в папке json_test_cases/"), false);
            source.sendFeedback(() -> Text.literal("Используйте '/origins diagnostic json' для тестирования валидации"), false);
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при создании тестовых файлов: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при создании тестовых файлов: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int runKeybindingTests(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Запуск тестов клавиш..."), false);
        
        try {
            // Запускаем тесты в отдельном потоке
            new Thread(() -> {
                try {
                    KeybindingTest.runKeybindingTests().thenAccept(testSuite -> {
                        source.sendFeedback(() -> Text.literal("Тесты клавиш завершены:"), false);
                        source.sendFeedback(() -> Text.literal("- Пройдено: " + testSuite.passedTests + "/" + testSuite.results.size()), false);
                        source.sendFeedback(() -> Text.literal("- Время выполнения: " + testSuite.totalDuration + "мс"), false);
                        
                        if (testSuite.allTestsPassed()) {
                            source.sendFeedback(() -> Text.literal("✓ Все тесты клавиш пройдены успешно!"), false);
                        } else {
                            source.sendFeedback(() -> Text.literal("✗ Некоторые тесты клавиш не пройдены. Проверьте логи."), false);
                        }
                        
                        // Показываем детали неудачных тестов
                        for (KeybindingTest.TestResult result : testSuite.results) {
                            if (!result.passed) {
                                source.sendFeedback(() -> Text.literal("  ✗ " + result.testName + ": " + result.message), false);
                            }
                        }
                    }).exceptionally(throwable -> {
                        Origins.LOGGER.error("Ошибка при выполнении тестов клавиш: " + throwable.getMessage(), throwable);
                        source.sendError(Text.literal("Ошибка при выполнении тестов клавиш: " + throwable.getMessage()));
                        return null;
                    });
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при запуске тестов клавиш: " + e.getMessage(), e);
                    source.sendError(Text.literal("Ошибка при запуске тестов клавиш: " + e.getMessage()));
                }
            }).start();
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при запуске тестов клавиш: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при запуске тестов клавиш: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int runTextureTests(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Запуск тестов текстур..."), false);
        
        try {
            // Запускаем тесты в отдельном потоке
            new Thread(() -> {
                try {
                    TextureTest.runTextureTests().thenAccept(testSuite -> {
                        source.sendFeedback(() -> Text.literal("Тесты текстур завершены:"), false);
                        source.sendFeedback(() -> Text.literal("- Пройдено: " + testSuite.passedTests + "/" + testSuite.results.size()), false);
                        source.sendFeedback(() -> Text.literal("- Время выполнения: " + testSuite.totalDuration + "мс"), false);
                        
                        if (testSuite.allTestsPassed()) {
                            source.sendFeedback(() -> Text.literal("✓ Все тесты текстур пройдены успешно!"), false);
                        } else {
                            source.sendFeedback(() -> Text.literal("✗ Некоторые тесты текстур не пройдены. Проверьте логи."), false);
                        }
                        
                        // Показываем детали неудачных тестов
                        for (TextureTest.TextureTestResult result : testSuite.results) {
                            if (!result.passed) {
                                source.sendFeedback(() -> Text.literal("  ✗ " + result.testName + ": " + result.message), false);
                                // Показываем детали для критических ошибок
                                if (!result.details.isEmpty()) {
                                    for (String detail : result.details) {
                                        source.sendFeedback(() -> Text.literal("    " + detail), false);
                                    }
                                }
                            }
                        }
                    }).exceptionally(throwable -> {
                        Origins.LOGGER.error("Ошибка при выполнении тестов текстур: " + throwable.getMessage(), throwable);
                        source.sendError(Text.literal("Ошибка при выполнении тестов текстур: " + throwable.getMessage()));
                        return null;
                    });
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при запуске тестов текстур: " + e.getMessage(), e);
                    source.sendError(Text.literal("Ошибка при запуске тестов текстур: " + e.getMessage()));
                }
            }).start();
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при запуске тестов текстур: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при запуске тестов текстур: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int getValidationStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            String status = OngoingValidationService.getValidationStatus();
            
            // Разбиваем статус на строки и отправляем каждую отдельно
            String[] lines = status.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    source.sendFeedback(() -> Text.literal(line), false);
                }
            }
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при получении статуса валидации: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при получении статуса валидации: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int forceValidation(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Принудительный запуск валидации..."), false);
        
        try {
            OngoingValidationService.forceValidation();
            source.sendFeedback(() -> Text.literal("Принудительная валидация запущена. Проверьте логи для результатов."), false);
            
            return 1;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при принудительной валидации: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при принудительной валидации: " + e.getMessage()));
            return 0;
        }
    }
}