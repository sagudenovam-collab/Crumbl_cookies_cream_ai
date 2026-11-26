# Чеклист соответствия техническому заданию

Статусы: ✅ выполнено · 🟡 частично выполнено · ❌ не выполнено.
Пункт отмечается выполненным только если он реально реализован в коде (не
только описан в README).

## 1. Главная задача проекта

| Пункт | Статус | Где реализовано | Чем подтверждается |
|---|---|---|---|
| Чтение документа с требованиями | ✅ | `parser/*DocumentParser.java` | `parser/*Test.java` |
| Извлечение отдельных требований | ✅ | `extraction/RequirementExtractor.java` | `RequirementExtractorTest`, `AnalysisOrchestratorE2ETest` |
| Присвоение идентификаторов | ✅ | `extraction/RequirementNormalizer.java` | `RequirementNormalizerTest` |
| Индивидуальный анализ требования | ✅ | `analysis/IndividualRequirementAnalyzer.java` | `IndividualRequirementAnalyzerTest` |
| Анализ набора требований в совокупности | ✅ | `analysis/RequirementSetAnalyzer.java` | `RequirementSetAnalyzerTest` |
| Поиск противоречий/неоднозначностей/неполноты/дублирования | ✅ | `RequirementSetAnalyzer`, `IndividualRequirementAnalyzer` | `*Test`, `AnalysisOrchestratorE2ETest` |
| Проверка доказательности замечаний | ✅ | `analysis/EvidenceVerifier.java`, `ContradictionDetector.java` | `EvidenceVerifierTest`, `ContradictionDetectorTest` |
| Рекомендации по исправлению | ✅ | поле `recommendation` в `Finding`, `ContradictionRecord` | `MarkdownReportGeneratorTest` |
| Requirement Health Score | ✅ | `analysis/RequirementScorer.java` | `RequirementScorerTest` |
| Подробный отчёт на русском языке | ✅ | `report/MarkdownReportGenerator.java` | `MarkdownReportGeneratorTest` |

## 2. Обязательное техническое задание

### 2.1 Входные документы

| Пункт | Статус | Где |
|---|---|---|
| Путь к документу через командную строку | ✅ | `cli/CliRunner.java` (`--input`) |
| PDF (обязательно) | ✅ | `parser/PdfDocumentParser.java`, сохраняет номер страницы |
| Markdown (обязательно) | ✅ | `parser/MarkdownDocumentParser.java`, сохраняет номер строки |
| DOCX (по возможности) | ✅ | `parser/DocxDocumentParser.java` (номер страницы не определяется POI — используется порядковый номер абзаца, честно отражено в коде/README) |
| TXT (по возможности) | ✅ | `parser/PlainTextDocumentParser.java` |
| Обработка пустых документов | ✅ | `ParsedDocument.isEmpty()`, обработано в `AnalysisOrchestrator` |
| Обработка повреждённых файлов | ✅ | `DocumentParsingException` во всех парсерах; тесты `corruptedPdfThrowsDocumentParsingException`, `corruptedDocxThrowsException` |
| Обработка неподдерживаемых форматов | ✅ | `DocumentParserRegistry`, тест `unsupportedFormatThrowsClearError` |
| Документы без требований | ✅ | извлечение вернёт пустой список, отчёт явно это укажет |
| Документы с большим количеством требований | 🟡 | реализовано чанкирование с перекрытием и digest-проход (`RequirementSetAnalyzer`), но не покрыто отдельным тестом на реально большом документе (сотни требований) из-за ограничений среды разработки на обращения к реальному LLM |
| Документы с таблицами и списками | 🟡 | DOCX-таблицы разбираются (`DocxDocumentParser`); списки Markdown/TXT разбираются как обычные абзацы (без специального распознавания маркеров списка) |
| Документы с требованиями без идентификаторов | ✅ | `RequirementNormalizer` присваивает `REQ-XXX`, если `originalId` отсутствует |
| Не придумывать координаты | ✅ | `SourceLocation` заполняется только из реально распознанных сегментов; индексы `[S{n}]` в промпте извлечения не позволяют модели изобретать номера |

### 2.2 Извлечение требований — ✅

Все перечисленные поля реализованы в `model/Requirement.java`. Неоднозначные
фрагменты помечаются `needsHumanReview=true` (`RequirementExtractor`,
`ExtractionPrompts`).

### 2.3 Анализ каждого требования — ✅

Критерии ISO/IEC/IEEE 29148 и IEEE 830 — `AnalysisPrompts.INDIVIDUAL_SYSTEM_PROMPT`.
Все обязательные поля замечания — `model/Finding.java`.

### 2.4 Анализ всего набора требований — ✅ (с оговоркой 🟡 по очень большим документам, см. 2.1)

Реализовано в `RequirementSetAnalyzer` + `ContradictionDetector`.

## 3. Evidence-First Architecture — ✅

`EvidenceVerifier` — отдельный этап после LLM-анализа, проверяющий цитаты,
ID требований и выставляющий статус `CONFIRMED_ISSUE` / `POTENTIAL_ISSUE` /
`NEEDS_HUMAN_REVIEW`. Покрыто `EvidenceVerifierTest`.

## 4. Severity и приоритеты — ✅

`SeverityClassifier` (`model/Severity`, `model/Priority`), включая защиту от
завышения серьёзности непроверенных замечаний. Покрыто `SeverityClassifierTest`.

## 5. Конкурентные функции

| Функция | Статус | Где |
|---|---|---|
| 5.1 Requirement Health Score | ✅ | `RequirementScorer` |
| 5.2 Contradiction Detective | ✅ | `ContradictionDetector` + вкладка «Анализ набора» на фронтенде |
| 5.3 Smart Rewrite Suggestions | ✅ | поле `improvedWording`, промпт запрещает выдумывать значения (плейсхолдеры) |
| 5.4 Traceability Matrix | ✅ | `TraceabilityAnalyzer` |
| 5.5 False Positive Protection | ✅ | `ContradictionDetector`, пороги уверенности в `EvidenceVerifier`/`RequirementScorer` |
| 5.6 Executive Summary | ✅ | `model/ExecutiveSummary`, раздел 1 отчёта |

## 6. Итоговый отчёт — ✅

`MarkdownReportGenerator` реализует все 15 (в коде — 14 пронумерованных,
титульная часть вынесена отдельно) разделов из ТЗ, включая паспорт анализа,
Health Score, таблицы требований/противоречий/дубликатов, матрицу
прослеживаемости, приоритетный план и итоговый вердикт с 4 статусами. JSON
формируется `JsonReportWriter`.

## 7. Технологический стек — ✅

Java 21, Spring Boot 3, Maven, Gemini API, Jackson, Apache PDFBox, Apache POI,
Spring RestClient. `LLMProvider` — отдельный интерфейс, Gemini — одна из
возможных реализаций (`llm/LLMProvider.java`, `llm/GeminiLLMProvider.java`).
Ключ — через `.env`/переменные окружения (`.env.example`, `CreamAiApplication`
загружает dotenv). Обработка лимитов/ошибок/таймаутов — ✅ (`GeminiLLMProvider`).

## 8. Архитектура — ✅

Все перечисленные в ТЗ компоненты реализованы как отдельные классы (см.
структуру пакетов в README). Без БД и микросервисов, как и требовалось для
хакатона.

## 9. Запуск одной командой — ✅

`cli/CliRunner.java`: `--input`, `--output`, `--format`; проверка
существования файла, создание выходной директории, атомарная запись отчёта
(не оставляет повреждённый файл при сбое), понятные коды возврата и сообщения
об ошибках.

## 10. Тестирование

| Сценарий из ТЗ | Статус | Тест |
|---|---|---|
| 1. Хорошо сформулированное требование | ✅ | `IndividualRequirementAnalyzerTest.wellFormedRequirementProducesNoFindings` |
| 2. Неоднозначное требование | ✅ | `IndividualRequirementAnalyzerTest.ambiguousRequirementProducesFindingWithQuote`, `RequirementExtractorTest` |
| 3. Непроверяемое требование | ✅ | покрыто сценарием REQ-2/REQ-8 в `docs/demo-requirements.md` и E2E-тестом |
| 4. Неполное требование | 🟡 | покрывается тем же механизмом (критерий «Полнота»), отдельного юнит-теста на этот конкретный критерий не выделено |
| 5. Составное требование | 🟡 | правило "не объединять" заложено в промпт извлечения (`ExtractionPrompts`), отдельного автоматического теста на разделение составного требования нет (требует реального вызова LLM для проверки его решения) |
| 6. Противоречие между требованиями | ✅ | `RequirementSetAnalyzerTest.detectsContradictionBetweenTwoRequirements`, `AnalysisOrchestratorE2ETest` |
| 7. Дублирующиеся требования | ✅ | `RequirementSetAnalyzerTest.detectsDuplicateRequirements`, `AnalysisOrchestratorE2ETest` |
| 8. Чрезмерное проектное ограничение | 🟡 | критерий "независимость от проектного решения" заложен в промпт, отдельного юнит-теста нет |
| 9. Отсутствие исходного ID | ✅ | `RequirementNormalizerTest` |
| 10. Пустой документ | ✅ | `MarkdownDocumentParserTest.emptyDocumentProducesNoSegments`, `PdfDocumentParserTest.emptyPdfProducesNoSegments`, `VerdictCalculatorTest.emptyDocumentIsInsufficientInformation` |
| 11. Некорректный PDF | ✅ | `PdfDocumentParserTest.corruptedPdfThrowsDocumentParsingException` |
| 12. Ошибка API | ✅ | `IndividualRequirementAnalyzerTest.llmErrorPropagatesInsteadOfFakeSuccess` |
| 13. Ответ LLM с неправильным JSON | ✅ | `IndividualRequirementAnalyzerTest.malformedJsonResponseThrowsInvalidResponse`, `JsonExtractionUtilTest` |
| 14. Отсутствующая цитата в AI-замечании | ✅ | `EvidenceVerifierTest.quoteNotFoundInDocumentIsNeedsHumanReview`, `ContradictionDetectorTest` |
| 15. Большой документ с большим количеством требований | 🟡 | логика чанкирования реализована (`RequirementSetAnalyzer`), но не проверена тестом на документе с сотнями требований — см. ограничение в разделе 2.1 |

Unit- и интеграционные тесты — ✅ (JUnit 5 + AssertJ + Spring Boot Test).
Тесты с реальным Gemini API отделены тегом `requires-gemini-api` и исключены
из сборки по умолчанию (`pom.xml`, `GeminiLLMProviderIT`) — ✅.
End-to-end тест полного процесса — ✅ (`AnalysisOrchestratorE2ETest`).

**Важная оговорка по запуску тестов**: среда, в которой готовился этот
репозиторий, не имеет сетевого доступа к Maven Central, поэтому `mvn test`
не мог быть запущен локально в этой среде. Тесты запускаются и проверяются
через GitHub Actions CI (`.github/workflows/ci.yml`) сразу после push в
репозиторий. Результат конкретного прогона CI — в истории Actions репозитория
(ссылка будет указана в финальном сообщении после push).

## 11. Демонстрация для жюри — ✅

`docs/demo-requirements.md` (документ с заложенными дефектами) +
`docs/DEMO.md` (сценарий, команды, ожидаемый результат, сильные стороны,
архитектура, методика оценки).

## 12. README и документация — ✅

`README.md` содержит все перечисленные в ТЗ разделы. `.gitignore`,
`.env.example` — ✅. Инструкция по сборке и демонстрации — ✅ (`docs/DEMO.md`).
Mermaid-диаграмма — ❌ не добавлена (текстовая ASCII-схема конвейера приведена
вместо неё; помечено как опциональное "при необходимости").
`docker-compose.yml`/Dockerfile — ❌ не добавлены (в ТЗ помечено как
необязательное: "не делай Docker обязательным, если обычный запуск проще").

## 13. Финальная проверка по ТЗ соревнования — этот документ

## 14. Особые правила разработки

Соблюдены: архитектура реализована полностью (не только спроектирована),
решения принимались самостоятельно без лишних уточняющих вопросов,
фиктивные данные вместо реального анализа не используются (ошибки LLM
пробрасываются, а не маскируются), TODO вместо обязательной реализации не
оставлены. Единственное честно обозначенное ограничение — невозможность
запустить `mvn`/`npm` test-прогон **локально в этой среде разработки** из-за
сетевых ограничений (см. пункт 10) — реальная проверка выполняется через CI.

## 15–16. План работы и финальная сдача

Этапы 1–6 выполнены (изучение репозитория, основа, AI-интеграция,
надёжность/верификация, отчёт, тесты). Этап 7 (конкурсная подготовка) —
выполнен, за исключением:

- **Реальный отчёт на документе организаторов** (`reports/organizer-analysis.md`)
  — ❌ **не создан**, так как документ организаторов хакатона не был
  предоставлен в рамках этой задачи. Фиктивный отчёт сознательно не создан —
  согласно прямому требованию ТЗ ("если документ организаторов недоступен, не
  создавай фиктивный отчёт"). **Что нужно для завершения этого пункта**:
  получить официальный документ требований от организаторов BCC-HUB × MNU и
  запустить `./mvnw spring-boot:run -Dspring-boot.run.arguments="--input=<путь к документу организаторов> --output=reports/organizer-analysis.md"`.
