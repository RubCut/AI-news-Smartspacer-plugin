package com.rubcut.ainews

/** Repository of this plugin, shown in the about section and sent to OpenRouter. */
const val PROJECT_URL = "https://github.com/RubCut/AI-news-Smartspacer-plugin"

/**
 * A backend that can write the news.
 *
 * Each entry only differs in its dialect ([flavor]), its base URL and the model
 * names it offers, so adding a provider is a one line change. The `CUSTOM`
 * entries let the user point the plugin at anything else — a self-hosted
 * gateway, Ollama, LM Studio or a provider that appeared after this release.
 */
enum class AiProvider(
    val id: String,
    val label: String,
    val flavor: ApiFlavor,
    val defaultBaseUrl: String,
    val defaultModel: String,
    /** Models offered before the real list is fetched from the API. */
    val fallbackModels: List<String>,
    val apiKeyUrl: String,
    /** Whether the user may edit the base URL in the settings. */
    val editableBaseUrl: Boolean = false,
    val requiresKey: Boolean = true
) {
    GEMINI(
        id = "gemini",
        label = "Google Gemini",
        flavor = ApiFlavor.GEMINI,
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
        defaultModel = "gemini-2.5-flash",
        fallbackModels = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash"),
        apiKeyUrl = "https://aistudio.google.com/app/apikey"
    ),
    ANTHROPIC(
        id = "anthropic",
        label = "Anthropic Claude",
        flavor = ApiFlavor.ANTHROPIC,
        defaultBaseUrl = "https://api.anthropic.com/v1",
        defaultModel = "claude-sonnet-4-5",
        fallbackModels = listOf(
            "claude-sonnet-4-5",
            "claude-opus-4-1",
            "claude-3-5-haiku-latest"
        ),
        apiKeyUrl = "https://console.anthropic.com/settings/keys"
    ),
    OPENAI(
        id = "openai",
        label = "OpenAI",
        flavor = ApiFlavor.OPENAI,
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        fallbackModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini"),
        apiKeyUrl = "https://platform.openai.com/api-keys"
    ),
    DEEPSEEK(
        id = "deepseek",
        label = "DeepSeek",
        flavor = ApiFlavor.OPENAI,
        defaultBaseUrl = "https://api.deepseek.com/v1",
        defaultModel = "deepseek-chat",
        fallbackModels = listOf("deepseek-chat", "deepseek-reasoner"),
        apiKeyUrl = "https://platform.deepseek.com/api_keys"
    ),
    OPENROUTER(
        id = "openrouter",
        label = "OpenRouter",
        flavor = ApiFlavor.OPENAI,
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "openai/gpt-4o-mini",
        fallbackModels = listOf(
            "openai/gpt-4o-mini",
            "anthropic/claude-3.5-haiku",
            "google/gemini-2.0-flash-001",
            "deepseek/deepseek-chat"
        ),
        apiKeyUrl = "https://openrouter.ai/keys"
    ),
    GROQ(
        id = "groq",
        label = "Groq",
        flavor = ApiFlavor.OPENAI,
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        fallbackModels = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"),
        apiKeyUrl = "https://console.groq.com/keys"
    ),
    MISTRAL(
        id = "mistral",
        label = "Mistral AI",
        flavor = ApiFlavor.OPENAI,
        defaultBaseUrl = "https://api.mistral.ai/v1",
        defaultModel = "mistral-small-latest",
        fallbackModels = listOf("mistral-small-latest", "mistral-large-latest"),
        apiKeyUrl = "https://console.mistral.ai/api-keys"
    ),
    XAI(
        id = "xai",
        label = "xAI Grok",
        flavor = ApiFlavor.OPENAI,
        defaultBaseUrl = "https://api.x.ai/v1",
        defaultModel = "grok-3-mini",
        fallbackModels = listOf("grok-3-mini", "grok-3", "grok-4"),
        apiKeyUrl = "https://console.x.ai/"
    ),
    QWEN(
        id = "qwen",
        label = "Alibaba Qwen",
        flavor = ApiFlavor.OPENAI,
        defaultBaseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
        defaultModel = "qwen-plus",
        fallbackModels = listOf("qwen-plus", "qwen-turbo", "qwen-max"),
        apiKeyUrl = "https://bailian.console.alibabacloud.com/"
    ),
    TOGETHER(
        id = "together",
        label = "Together AI",
        flavor = ApiFlavor.OPENAI,
        defaultBaseUrl = "https://api.together.xyz/v1",
        defaultModel = "meta-llama/Llama-3.3-70B-Instruct-Turbo",
        fallbackModels = listOf(
            "meta-llama/Llama-3.3-70B-Instruct-Turbo",
            "Qwen/Qwen2.5-72B-Instruct-Turbo"
        ),
        apiKeyUrl = "https://api.together.xyz/settings/api-keys"
    ),
    PERPLEXITY(
        id = "perplexity",
        label = "Perplexity",
        flavor = ApiFlavor.OPENAI,
        defaultBaseUrl = "https://api.perplexity.ai",
        defaultModel = "sonar",
        fallbackModels = listOf("sonar", "sonar-pro", "sonar-reasoning"),
        apiKeyUrl = "https://www.perplexity.ai/settings/api"
    ),
    CEREBRAS(
        id = "cerebras",
        label = "Cerebras",
        flavor = ApiFlavor.OPENAI,
        defaultBaseUrl = "https://api.cerebras.ai/v1",
        defaultModel = "llama-3.3-70b",
        fallbackModels = listOf("llama-3.3-70b", "qwen-3-32b"),
        apiKeyUrl = "https://cloud.cerebras.ai/"
    ),
    OLLAMA(
        id = "ollama",
        label = "Ollama / local server",
        flavor = ApiFlavor.OPENAI,
        defaultBaseUrl = "http://127.0.0.1:11434/v1",
        defaultModel = "llama3.2",
        fallbackModels = listOf("llama3.2", "qwen2.5", "mistral"),
        apiKeyUrl = "https://ollama.com/download",
        editableBaseUrl = true,
        // A local server usually needs no key at all.
        requiresKey = false
    ),
    CUSTOM_OPENAI(
        id = "custom_openai",
        label = "Custom (OpenAI-compatible)",
        flavor = ApiFlavor.OPENAI,
        defaultBaseUrl = "",
        defaultModel = "",
        fallbackModels = emptyList(),
        apiKeyUrl = PROJECT_URL,
        editableBaseUrl = true,
        requiresKey = false
    ),
    CUSTOM_ANTHROPIC(
        id = "custom_anthropic",
        label = "Custom (Anthropic-compatible)",
        flavor = ApiFlavor.ANTHROPIC,
        defaultBaseUrl = "",
        defaultModel = "",
        fallbackModels = emptyList(),
        apiKeyUrl = PROJECT_URL,
        editableBaseUrl = true,
        requiresKey = false
    ),
    CUSTOM_GEMINI(
        id = "custom_gemini",
        label = "Custom (Gemini-compatible)",
        flavor = ApiFlavor.GEMINI,
        defaultBaseUrl = "",
        defaultModel = "",
        fallbackModels = emptyList(),
        apiKeyUrl = PROJECT_URL,
        editableBaseUrl = true,
        requiresKey = false
    );

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: GEMINI
        fun fromLabel(label: String?) = entries.firstOrNull { it.label == label }
    }
}
