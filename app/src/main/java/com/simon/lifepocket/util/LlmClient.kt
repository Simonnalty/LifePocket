package com.simon.lifepocket.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI 设置：全部存本机 SharedPreferences，Key 永不上传仓库、不出网（只随请求发给模型服务商）。
 * 默认智谱 GLM-4-Flash（长期免费）；换豆包只需改 base_url / model / key 三项。
 */
object AiSettings {
    const val DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4"
    const val DEFAULT_MODEL = "glm-4-flash"
    const val DEFAULT_NAME = "澄澄"

    fun prefs(c: Context): SharedPreferences =
        c.getSharedPreferences("lifepocket", Context.MODE_PRIVATE)

    fun enabled(c: Context) = prefs(c).getBoolean("ai_enabled", false)
    fun setEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean("ai_enabled", v).apply()

    fun name(c: Context) = prefs(c).getString("ai_name", DEFAULT_NAME) ?: DEFAULT_NAME
    fun setName(c: Context, v: String) = prefs(c).edit().putString("ai_name", v).apply()

    fun baseUrl(c: Context) = prefs(c).getString("ai_base_url", DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    fun setBaseUrl(c: Context, v: String) = prefs(c).edit().putString("ai_base_url", v.trim()).apply()

    fun model(c: Context) = prefs(c).getString("ai_model", DEFAULT_MODEL) ?: DEFAULT_MODEL
    fun setModel(c: Context, v: String) = prefs(c).edit().putString("ai_model", v.trim()).apply()

    fun apiKey(c: Context) = prefs(c).getString("ai_key", "") ?: ""
    fun setApiKey(c: Context, v: String) = prefs(c).edit().putString("ai_key", v.trim()).apply()

    /** AI 可用 = 开关已开 + Key 已填 */
    fun ready(c: Context) = enabled(c) && apiKey(c).isNotBlank()
}

class LlmException(message: String) : Exception(message)

/** OpenAI 兼容 /chat/completions 极简客户端（零第三方依赖，阻塞 IO，需在协程 IO 线程调用） */
object LlmClient {

    fun chat(context: Context, system: String, user: String, temperature: Double = 0.2): String {
        val url = AiSettings.baseUrl(context).trimEnd('/') + "/chat/completions"
        val body = JSONObject()
            .put("model", AiSettings.model(context))
            .put("temperature", temperature)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", system))
                .put(JSONObject().put("role", "user").put("content", user)))
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 12_000
            conn.readTimeout = 40_000
            conn.setRequestProperty("Authorization", "Bearer ${AiSettings.apiKey(context)}")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.readBytes()?.toString(Charsets.UTF_8) ?: ""
            if (code !in 200..299) throw LlmException("HTTP $code ${text.take(160)}")
            val content = JSONObject(text)
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
            return stripFence(content)
        } finally {
            conn.disconnect()
        }
    }

    /** 剥掉 ```json 围栏并抠出 JSON 主体（模型偶发说废话时的兜底） */
    fun stripFence(text: String): String {
        val t = text.trim()
        val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(t)?.groupValues?.get(1)?.trim()
        val candidate = fenced ?: t
        val start = candidate.indexOfFirst { it == '{' || it == '[' }
        val end = candidate.lastIndexOfAny(charArrayOf('}', ']'))
        return if (start >= 0 && end > start) candidate.substring(start, end + 1) else candidate
    }
}
