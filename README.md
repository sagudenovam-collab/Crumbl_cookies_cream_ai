# Requirements Intelligence Agent

Интеллектуальный агент автоматического анализа качества технических требований
перед началом разработки программного обеспечения. Проект создан для
**BCC-HUB × MNU FinTech Hackathon**.

## Какую проблему решает проект

Команды разработки часто начинают работу по требованиям, которые содержат
неоднозначности, противоречия, пробелы и дублирование — это приводит к
переделкам, спорам с заказчиком и срывам сроков. Requirements Intelligence
Agent читает документ с требованиями, извлекает из него отдельные требования,
анализирует каждое по стандартам **ISO/IEC/IEEE 29148:2018** и **IEEE Std
830-1998**, анализирует весь набор требований в совокупности (противоречия,
дубликаты, пробелы), проверяет доказательность каждого найденного замечания
(Evidence-First Architecture) и формирует подробный отчёт на русском языке с
прозрачным показателем качества — **Requirement Health Score**.

Это статический анализ требований до разработки, а не тестирование готового ПО.

## Основные функции

- Разбор документов **PDF** (с привязкой к странице) и **Markdown** (с привязкой
  к строке), а также DOCX и TXT.
- Извлечение отдельных требований с присвоением внутреннего ID (`REQ-001`, ...)
  и сохранением исходного ID, если он есть.
- Индивидуальный анализ каждого требования по 10 критериям ISO/IEC/IEEE 29148
  и IEEE 830 (корректность, необходимость, однозначность, полнота,
  проверяемость, осуществимость, согласованность, атомарность,
  прослеживаемость, независимость от проектного решения).
- Анализ всего набора требований: противоречия, дубликаты, несогласованные
  термины, пробелы в сценариях, проблемы с ролями и правами доступа и т.д.
- **Evidence-First Architecture**: каждое замечание содержит точную цитату,
  местоположение, критерий, объяснение, рекомендацию, серьёзность и статус
  доказательности (`CONFIRMED_ISSUE` / `POTENTIAL_ISSUE` /
  `NEEDS_HUMAN_REVIEW`), подтверждённый отдельным этапом верификации.
- **Requirement Health Score** (0–100) с прозрачной расшифровкой — собственная
  методика продукта, а не официальная оценка ISO.
- **Contradiction Detective** — отдельный механизм поиска противоречий с
  цитатами обеих сторон и предложением разрешения.
- **Smart Rewrite Suggestions** — примеры улучшенных формулировок без
  выдумывания бизнес-правил (плейсхолдеры там, где значение должен определить
  заказчик).
- **Traceability Matrix** и **Executive Summary**.
- Итоговый вердикт: `READY` / `READY_WITH_RESERVATIONS` / `NOT_READY` /
  `INSUFFICIENT_INFORMATION` — критичные дефекты влияют на вердикт отдельно от
  среднего балла.
- CLI (запуск одной командой), REST API и современный веб-интерфейс на русском
  языке.

## Архитектура

```
cream_ai/   — backend: Java 21, Spring Boot 3, Maven
frontend/   — frontend: React + TypeScript + Vite + Tailwind CSS
docs/       — демонстрационный документ и сценарий защиты
reports/    — сюда сохраняются сгенерированные отчёты (в .gitignore)
```

Конвейер анализа (`AnalysisOrchestrator`):

```
DocumentParser → RequirementExtractor → RequirementNormalizer
   → IndividualRequirementAnalyzer → RequirementSetAnalyzer
   → ContradictionDetector → EvidenceVerifier → SeverityClassifier
   → RequirementScorer → TraceabilityAnalyzer → VerdictCalculator
   → MarkdownReportGenerator / JsonReportWriter
```

`LLMProvider` — интерфейс поверх LLM-провайдера. Сейчас реализован
`GeminiLLMProvider`, но архитектура не привязана к одному поставщику: для
перехода на другую модель достаточно новой реализации интерфейса.

Подробнее об архитектуре, методике Health Score и сильных сторонах проекта —
в [`docs/DEMO.md`](docs/DEMO.md).

## Технологический стек

- **Backend**: Java 21, Spring Boot 3, Maven, Apache PDFBox, Apache POI,
  Jackson, Spring RestClient.
- **LLM**: Gemini API (бесплатный тариф), через интерфейс `LLMProvider`.
- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS.
- **Тесты**: JUnit 5, AssertJ, Spring Boot Test.

## Требования к окружению

- Java 21 (JDK)
- Maven (в репозитории есть Maven Wrapper `./mvnw`)
- Node.js 20+ и npm (для frontend)
- Ключ Gemini API

## Получение Gemini API Key

1. Перейдите на https://aistudio.google.com/app/apikey
2. Создайте API-ключ (доступен бесплатный тариф, достаточный для демонстрации).
3. Скопируйте ключ — он понадобится в следующем шаге.

## Настройка `.env`

```bash
cd cream_ai
cp .env.example .env
```

Откройте `.env` и укажите `GEMINI_API_KEY`. Файл `.env` добавлен в
`.gitignore` и никогда не коммитится.

## Установка и запуск backend

```bash
cd cream_ai
./mvnw clean install -DskipTests
```

### Запуск одной командой (CLI)

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--input=../docs/demo-requirements.md --output=reports/result.md"
```

Параметры:

- `--input` — путь к документу (PDF, MD, DOCX, TXT), обязателен.
- `--output` — путь к файлу отчёта (по умолчанию `reports/result.md`).
- `--format` — `md` (по умолчанию) или `json`.

CLI завершается кодом `0` при успехе, `1`–`4` — при разных типах ошибок
(файл не найден, ошибка разбора документа, ошибка LLM, непредвиденная ошибка).
Отчёт пишется атомарно (через временный файл) — при аварийном завершении
повреждённый отчёт не остаётся.

### Запуск backend как сервера (для веб-интерфейса)

```bash
./mvnw spring-boot:run
```

Backend поднимается на `http://localhost:8080`. Основные эндпоинты REST API:

| Метод | Путь | Назначение |
|---|---|---|
| POST | `/api/analyses` | Загрузить документ (`multipart/form-data`, поле `file`) и запустить анализ |
| GET | `/api/analyses/{id}/status` | Статус анализа (этапы, ошибки) |
| GET | `/api/analyses/{id}/result` | Полный результат анализа |
| GET | `/api/analyses/{id}/requirements` | Список извлечённых требований |
| GET | `/api/analyses/{id}/findings` | Замечания, противоречия, дубликаты |
| GET | `/api/analyses/{id}/report` | Скачать отчёт (Markdown) |
| GET | `/api/analyses/{id}/report?format=json` | Скачать отчёт (JSON) |

## Установка и запуск frontend

```bash
cd frontend
npm install
npm run dev
```

Откройте `http://localhost:5173`. Dev-сервер Vite проксирует запросы `/api/*`
на `http://localhost:8080` (см. `frontend/vite.config.ts`).

### Production-сборка frontend

```bash
cd frontend
npm run build
```

## Запуск проекта целиком

1. Запустите backend (`./mvnw spring-boot:run` в `cream_ai/`).
2. В другом терминале запустите frontend (`npm run dev` в `frontend/`).
3. Откройте `http://localhost:5173`, загрузите документ требований.

## Запуск автоматических тестов

```bash
cd cream_ai
./mvnw test
```

По умолчанию тесты, требующие реального обращения к Gemini API, **исключены**
из прогона (тег JUnit `requires-gemini-api`), поэтому сборка проходит без
ключа API. Чтобы запустить и их:

```bash
GEMINI_API_KEY=... ./mvnw test -Dgroups=requires-gemini-api
```

Покрытие тестами включает: разбор всех поддерживаемых форматов (в т.ч. пустые
и повреждённые файлы), извлечение и нормализацию требований, индивидуальный и
групповой анализ (включая обработку ошибок API и некорректного JSON от
модели), верификацию доказательств, классификацию серьёзности, расчёт Health
Score, расчёт вердикта, генерацию Markdown-отчёта и полный end-to-end прогон
конвейера на демонстрационном документе с заложенными дефектами.

## Сборка production-версии

```bash
cd cream_ai
./mvnw clean package
java -jar target/requirements-intelligence-agent.jar
```

## Непрерывная интеграция (CI)

В `.github/workflows/ci.yml` настроена сборка и тестирование backend (Maven)
и frontend (npm build) на каждый push — см. вкладку Actions репозитория.

## Формат входных файлов

- **PDF** — обязательно, с сохранением номера страницы для каждого требования.
- **Markdown (.md)** — обязательно, с сохранением номера строки.
- **DOCX** — реализовано (Apache POI); номера страниц в DOCX не определяются
  программно, поэтому вместо них используется порядковый номер абзаца.
- **TXT** — реализовано, с сохранением номера строки.

Система корректно обрабатывает пустые документы, повреждённые файлы,
неподдерживаемые форматы и документы без требований (отчёт формируется, но
указывает на отсутствие извлечённых требований).

## Описание результата

Результат анализа — это `AnalysisResult`: паспорт анализа, executive summary,
Health Score с расшифровкой, список требований, индивидуальные и системные
замечания, таблицы противоречий и дубликатов, матрица прослеживаемости,
итоговый вердикт и список пунктов, требующих проверки человеком. Он доступен
как Markdown-отчёт (`MarkdownReportGenerator`, 14 разделов) и как JSON
(`JsonReportWriter`).

## Известные ограничения

- Экспорт отчёта в PDF из веб-интерфейса не реализован (только Markdown и
  JSON) — это было помечено в ТЗ как опциональное ("при возможности").
- DOCX не содержит программно определяемых номеров страниц — вместо них
  используется порядковый номер абзаца (честно указано в отчёте, координаты
  не придумываются).
- Состояние задач анализа хранится в памяти процесса (без базы данных) — это
  осознанное упрощение для объёма хакатона; при перезапуске backend активные
  задачи теряются.
- Качество экстракции и анализа требований зависит от модели Gemini; агент не
  подменяет ошибку LLM фиктивным успешным результатом, а сообщает о ней.
- Сборка и тесты в среде, где готовился этот репозиторий, выполнялись через
  CI (GitHub Actions), а не локальным прогоном `mvn`, так как эта среда не
  имела доступа к Maven Central — см. `COMPLIANCE_CHECKLIST.md`.

## Структура репозитория

```
.
├── README.md
├── COMPLIANCE_CHECKLIST.md
├── docs/
│   ├── DEMO.md                  — сценарий демонстрации для жюри
│   └── demo-requirements.md     — демонстрационный документ с заложенными дефектами
├── .github/workflows/ci.yml     — сборка и тесты backend + frontend
├── cream_ai/                    — backend (Java 21 / Spring Boot 3)
│   ├── src/main/java/com/crumble_cookies/cream_ai/
│   │   ├── parser/       — DocumentParser'ы (PDF, Markdown, DOCX, TXT)
│   │   ├── extraction/   — RequirementExtractor, RequirementNormalizer
│   │   ├── llm/          — LLMProvider, GeminiLLMProvider
│   │   ├── analysis/     — анализ, верификация, оценка, вердикт
│   │   ├── report/       — генераторы Markdown/JSON отчётов
│   │   ├── cli/          — CLI-режим
│   │   ├── web/          — REST API
│   │   └── config/       — конфигурация Spring
│   ├── src/test/         — автоматические тесты
│   └── .env.example
└── frontend/                    — frontend (React + TypeScript + Vite)
    └── src/
        ├── api/          — клиент REST API
        └── components/   — UI-компоненты
```
