[English](README.md) · **Русский**

<div align="center">

<img src="docs/icon.svg" width="112" alt="Иконка AI News для Smartspacer">

# AI News для Smartspacer

**Задайте тему — нейросеть напишет новости прямо в Smartspacer**

[![Build APK](https://github.com/RubCut/AI-news-Smartspacer-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/RubCut/AI-news-Smartspacer-plugin/actions/workflows/build.yml)
![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-7F52FF?logo=kotlin&logoColor=white)
![Material 3 Expressive](https://img.shields.io/badge/Material-3_Expressive-6750A4)
![Gemini](https://img.shields.io/badge/Google-Gemini-4285F4?logo=googlegemini&logoColor=white)

</div>

Плагин добавляет в [Smartspacer](https://github.com/KieronQuinn/Smartspacer) один
Target с новостями, написанными нейросетью:

- 📝 вы вводите тему — от `искусственный интеллект` до `Формула-1`;
- 🤖 выбранный провайдер ИИ пишет краткий заголовок, полный заголовок и саму новость;
- 📰 на смартспейсе видно краткое название и строку `Tap to view full`;
- 📖 по нажатию открывается полноэкранная читалка со свайпом между новостями.

Таргет всегда один: если новость одна — показывается её краткое название, если
несколько — сводка вида `5 stories to read`.

## Возможности

- экран настроек в стиле Material 3 Expressive с большим сворачивающимся
  заголовком;
- полноэкранная читалка со свайпом между новостями, оверлеем из точек и
  счётчиком `x of y`;
- статьи в разметке GitHub Markdown: **жирный**, *курсив*, ~~зачёркнутый~~,
  заголовки, маркированные и нумерованные списки, цитаты, код и ссылки;
- выбор длины статьи — короткая, средняя или длинная;
- список моделей подгружается прямо из API, плюс проверка ключа в одно нажатие;
- много провайдеров ИИ: **Gemini**, **Claude**, **OpenAI**, **DeepSeek**,
  **OpenRouter**, **Groq**, **Mistral**, **xAI Grok**, **Qwen**, **Together**,
  **Perplexity**, **Cerebras**, **Ollama** и любой свой endpoint, совместимый с
  OpenAI, Anthropic или Gemini;
- ключи, модели и базовые URL запоминаются отдельно для каждого провайдера —
  переключение туда-обратно ничего не теряет;
- раздел **О плагине** с описанием и ссылкой на репозиторий;
- интерфейс на русском, английском, украинском, немецком, французском,
  испанском и португальском;
- все сборки подписаны одним ключом, поэтому обновления ставятся поверх;
- динамические цвета на Android 12+;
- светлая и тёмная темы;
- настройки у каждого таргета свои: тема, ключ, модель и расписание хранятся
  отдельно;
- можно добавить несколько таргетов с разными темами;
- автоматическое обновление с индивидуальным интервалом — от 15 до 480 минут;
- `Close & dismiss` скрывает только читаемую новость, а таргет пропадает, когда
  скрыта последняя;
- `Restore dismissed stories` возвращает все скрытые новости;
- нет иконки в лаунчере, нет геолокации и какого-либо отслеживания.

## Требования

- Android 10 или новее;
- установленный Smartspacer;
- API-ключ одного из поддерживаемых провайдеров — либо локальный сервер вроде
  [Ollama](https://ollama.com/), которому ключ вообще не нужен.

> У большинства провайдеров есть бесплатный тариф с лимитами и платные сверх
> него. Проверьте актуальные цены и квоты своего провайдера перед использованием.

### Поддерживаемые провайдеры

| Провайдер | Диалект API | Ключ |
|---|---|---|
| Google Gemini | Gemini | [AI Studio](https://aistudio.google.com/app/apikey) |
| Anthropic Claude | Anthropic | [Консоль](https://console.anthropic.com/settings/keys) |
| OpenAI | OpenAI | [Platform](https://platform.openai.com/api-keys) |
| DeepSeek | OpenAI | [Platform](https://platform.deepseek.com/api_keys) |
| OpenRouter | OpenAI | [Ключи](https://openrouter.ai/keys) |
| Groq | OpenAI | [Консоль](https://console.groq.com/keys) |
| Mistral AI | OpenAI | [Консоль](https://console.mistral.ai/api-keys) |
| xAI Grok | OpenAI | [Консоль](https://console.x.ai/) |
| Alibaba Qwen | OpenAI | [Bailian](https://bailian.console.alibabacloud.com/) |
| Together AI | OpenAI | [Настройки](https://api.together.xyz/settings/api-keys) |
| Perplexity | OpenAI | [Настройки](https://www.perplexity.ai/settings/api) |
| Cerebras | OpenAI | [Cloud](https://cloud.cerebras.ai/) |
| Ollama / локальный сервер | OpenAI | не нужен |
| Свой endpoint | OpenAI, Anthropic или Gemini | зависит от сервера |

## Получение API-ключа

1. Выберите провайдера в настройках и нажмите **Получить ключ …** — откроется
   консоль этого провайдера.
2. Создайте там ключ.
3. Вставьте его в настройки плагина.
4. Нажмите **Проверить ключ**, затем **Загрузить модели** — чтобы подтянуть
   доступные модели.

Для **Ollama** или своего endpoint вместо ключа укажите базовый URL
(например `http://192.168.1.10:11434/v1`), ключ можно оставить пустым.

## Установка

### Готовая debug-сборка

1. Откройте последний успешный запуск в
   [Actions](https://github.com/RubCut/AI-news-Smartspacer-plugin/actions/workflows/build.yml).
2. Скачайте артефакт `ai-news-debug-apk`.
3. Распакуйте архив и установите APK на телефон.

### Сборка из исходников

Требуются JDK 17, Android SDK 36 и Gradle 8.13.

```bash
git clone https://github.com/RubCut/AI-news-Smartspacer-plugin.git
cd AI-news-Smartspacer-plugin
gradle assembleDebug
```

APK будет в:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Настройка

1. В Smartspacer добавьте таргет **AI News**.
2. Настройки откроются автоматически; позже они доступны через **More settings**.
3. Укажите **тему**, о которой должна писать модель, и **язык** новостей.
4. Выберите **провайдера ИИ**, вставьте его **API-ключ** (а для локальных и своих
   endpoint — **базовый URL**), затем:
   - **Test key** — проверяет ключ через API и показывает число доступных моделей;
   - **Fetch models** — загружает реальный список моделей этого ключа в список.
5. Выберите поведение таргета:
   - **Article length** — короткая, средняя или длинная статья;
   - **Update interval** — от 15 до 480 минут;
   - **Stories on the smartspace** — от 1 до 5 новостей одновременно.
6. Нажмите **«Save and generate»**.

У плагина намеренно нет иконки в лаунчере: настройки открываются из Smartspacer
через **More settings**.

## Как работает обновление

```text
Smartspacer
    │ периодический запрос (свой интервал, 15–480 мин)
    ▼
NewsUpdateReceiver
    │ тема, язык, длина, модель
    ▼
Gemini API (структурированный JSON-ответ)
    │ краткий заголовок + полный заголовок + текст в Markdown
    ▼
SharedPreferences → Target
```

`getSmartspaceTargets()` не выполняет сетевых запросов. Провайдер таргета только
читает последние сохранённые новости, поэтому Smartspacer получает ответ быстро.
Генерация происходит в фоновом receiver или по кнопке при сохранении настроек.

## Приватность

- плагин не отслеживает геопозицию и не собирает аналитику;
- API-ключ, тема, настройки и сгенерированные новости хранятся локально в
  `SharedPreferences` — отдельно для каждого таргета;
- провайдеру отправляются только тема, язык и требуемая длина;
- при удалении таргета его настройки и новости стираются.

## Структура проекта

```text
app/src/main/java/com/rubcut/ainews/
├── Constants.kt                # authority, значения по умолчанию и лимиты
├── NewsItem.kt                 # модель новости
├── AiProvider.kt               # список поддерживаемых провайдеров
├── ApiFlavor.kt                # диалекты Gemini / Anthropic / OpenAI
├── AiClient.kt                 # единая точка входа для всех бэкендов
├── GeminiClient.kt             # Gemini generateContent
├── AnthropicClient.kt          # Claude /messages
├── OpenAiClient.kt             # OpenAI-совместимый /chat/completions
├── NewsPrompt.kt               # общий промпт
├── NewsJsonParser.kt           # устойчивый разбор JSON в новости
├── StoryLength.kt              # формулировка промпта и лимит токенов на длину
├── MarkdownRenderer.kt         # GitHub Markdown → форматированный текст
├── NewsUpdater.kt              # генерация и сохранение для одного таргета
├── NewsUpdateReceiver.kt       # запросы обновления Smartspacer
├── SettingsRepository.kt       # локальные настройки и кэш новостей
├── targets/
│   └── NewsTarget.kt           # провайдер таргета Smartspacer
└── ui/
    ├── SettingsActivity.kt     # экран настроек
    ├── NewsActivity.kt         # полноэкранная читалка
    └── StoryPagerAdapter.kt    # одна страница на новость
```

## Ограничения

- модель пишет на основе своих знаний, поэтому новости правдоподобные, но не
  проверенные — воспринимайте их как сгенерированный дайджест, а не новостную ленту;
- для генерации нужна сеть; при ошибке таргет сохраняет прошлые новости, а текст
  ошибки показывается в настройках;
- до настройки таргет показывает `Tap to set up your topic`;
- заголовок на смартспейсе обрезается до 42 символов, поэтому используется
  короткий формат;
- каждое обновление — это один API-запрос на таргет; учитывайте это при выборе
  интервала и тарифа.

## Подпись сборок

Все сборки — CI, локальные, debug и release — подписаны общим ключом из
[`signing/`](signing/README.md), поэтому новый APK всегда ставится поверх
предыдущего без ошибки несовпадения подписи. Чтобы использовать свой ключ,
создайте `signing.properties` в корне репозитория — подробности в README папки.

## Отказ от ответственности

Проект не является официальным продуктом Smartspacer или какого-либо провайдера
ИИ. Названия и товарные знаки принадлежат их правообладателям. Используя API
провайдера, соблюдайте действующие условия сервиса и ограничения вашего тарифа.
Сгенерированные новости могут содержать неточности.
