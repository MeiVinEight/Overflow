package cn.evolvefield.onebot.client.milky.translator

import com.google.gson.JsonObject

internal interface ActionTranslator {
    val action: String

    /**
     * Onebot 主动请求的 params 转换为 Milky 请求体
     */
    fun request(params: JsonObject): JsonObject

    /**
     * Milky 的返回结果转换为 Onebot 的返回结果 data
     */
    fun response(request: JsonObject, json: JsonObject): JsonObject

    companion object {
        val onebotRegistry: MutableMap<String, ActionTranslator> = mutableMapOf()
        val milkyRegistry: MutableMap<String, ActionTranslator> = mutableMapOf()
        fun init() {
            onebotRegistry.clear()
            milkyRegistry.clear()
            register("get_login_info", "get_login_info") { request, json ->
                TODO()
            }
            register("get_version_info", "get_impl_info") { request, json ->
                TODO()
            }
        }
        fun register(onebotAction: String, milkyAction: String, resp: (JsonObject, JsonObject) -> JsonObject) = register(onebotAction, milkyAction, { JsonObject() }, resp)
        fun register(onebotAction: String, milkyAction: String, req: (JsonObject) -> JsonObject, resp: (JsonObject, JsonObject) -> JsonObject) {
            val impl = object : ActionTranslator {
                override val action: String = milkyAction
                override fun request(params: JsonObject): JsonObject = req(params)
                override fun response(request: JsonObject, json: JsonObject): JsonObject = resp(request, json)
            }
            onebotRegistry[onebotAction] = impl
            milkyRegistry[milkyAction] = impl
        }
        fun execute(onebotAction: String, params: JsonObject): Pair<String, JsonObject>? {
            val impl = onebotRegistry[onebotAction] ?: return null
            return impl.action to impl.request(params)
        }
        fun execute(milkyAction: String, request: JsonObject, json: JsonObject): JsonObject? {
            val impl = milkyRegistry[milkyAction] ?: return null
            val data = impl.response(request, json)
            return JsonObject().apply {
                addProperty("status", "ok")
                addProperty("retcode", 0)
                add("data", data)
                request["echo"]?.also { add("echo", it) }
            }
        }
    }
}
