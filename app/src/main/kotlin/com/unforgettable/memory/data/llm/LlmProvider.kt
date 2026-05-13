package com.unforgettable.memory.data.llm

data class LlmModel(
    val id: String,
    val displayName: String = id,
)

data class LlmProvider(
    val id: String,
    val displayName: String,
    val apiKeyLabel: String,
    val apiKeyPlaceholder: String,
    val apiKeyRequiredPrefix: String? = null,
    val chatCompletionsUrl: String,
    val supportsJsonMode: Boolean = true,
    val models: List<LlmModel>,
) {
    val defaultModelId: String = models.first().id
}

object LlmProviders {
    const val OPENAI_ID = "openai"

    val all: List<LlmProvider> = listOf(
        LlmProvider(
            id = OPENAI_ID,
            displayName = "OpenAI",
            apiKeyLabel = "OpenAI API Key",
            apiKeyPlaceholder = "sk-...",
            chatCompletionsUrl = "https://api.openai.com/v1/chat/completions",
            models = listOf(
                LlmModel("gpt-4.1-mini", "GPT-4.1 Mini"),
                LlmModel("gpt-4.1", "GPT-4.1"),
                LlmModel("gpt-4o-mini", "GPT-4o Mini"),
            ),
        ),
        LlmProvider(
            id = "deepseek",
            displayName = "DeepSeek",
            apiKeyLabel = "DeepSeek API Key",
            apiKeyPlaceholder = "sk-...",
            apiKeyRequiredPrefix = "sk-",
            chatCompletionsUrl = "https://api.deepseek.com/chat/completions",
            models = listOf(
                LlmModel("deepseek-v4-flash", "DeepSeek V4 Flash"),
                LlmModel("deepseek-v4-pro", "DeepSeek V4 Pro"),
            ),
        ),
        LlmProvider(
            id = "qwen",
            displayName = "通义千问（阿里云百炼）",
            apiKeyLabel = "DashScope API Key",
            apiKeyPlaceholder = "sk-...",
            chatCompletionsUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            models = listOf(
                LlmModel("qwen3.6-flash", "Qwen3.6 Flash"),
                LlmModel("qwen3.6-plus", "Qwen3.6 Plus"),
                LlmModel("qwen3-max", "Qwen3 Max"),
            ),
        ),
        LlmProvider(
            id = "kimi",
            displayName = "Kimi（月之暗面）",
            apiKeyLabel = "Moonshot API Key",
            apiKeyPlaceholder = "sk-...",
            chatCompletionsUrl = "https://api.moonshot.cn/v1/chat/completions",
            models = listOf(
                LlmModel("kimi-k2.6", "Kimi K2.6"),
                LlmModel("kimi-k2.5", "Kimi K2.5"),
                LlmModel("moonshot-v1-8k", "Moonshot V1 8K"),
                LlmModel("moonshot-v1-32k", "Moonshot V1 32K"),
            ),
        ),
        LlmProvider(
            id = "zhipu",
            displayName = "智谱 GLM",
            apiKeyLabel = "智谱 API Key",
            apiKeyPlaceholder = "sk-...",
            chatCompletionsUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            models = listOf(
                LlmModel("glm-5.1", "GLM-5.1"),
                LlmModel("glm-5-turbo", "GLM-5 Turbo"),
                LlmModel("glm-4.7-flash", "GLM-4.7 Flash"),
            ),
        ),
        LlmProvider(
            id = "qianfan",
            displayName = "百度千帆（ERNIE）",
            apiKeyLabel = "千帆 Bearer Token",
            apiKeyPlaceholder = "bce-v3/...",
            chatCompletionsUrl = "https://qianfan.baidubce.com/v2/chat/completions",
            supportsJsonMode = false,
            models = listOf(
                LlmModel("ernie-4.0-turbo-8k", "ERNIE 4.0 Turbo 8K"),
                LlmModel("ernie-4.0-8k", "ERNIE 4.0 8K"),
                LlmModel("ernie-3.5-8k", "ERNIE 3.5 8K"),
            ),
        ),
    )

    val defaultProvider: LlmProvider = all.first()

    fun find(id: String?): LlmProvider {
        return all.firstOrNull { it.id == id } ?: defaultProvider
    }

    fun findModel(provider: LlmProvider, modelId: String?): LlmModel {
        return provider.models.firstOrNull { it.id == modelId } ?: provider.models.first()
    }
}
