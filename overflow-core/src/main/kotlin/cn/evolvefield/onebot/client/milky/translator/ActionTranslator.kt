package cn.evolvefield.onebot.client.milky.translator

import com.google.gson.JsonElement
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
    fun response(request: JsonObject, data: JsonObject): JsonElement

    companion object : TranslatorJsonUtil {
        val onebotRegistry: MutableMap<String, ActionTranslator> = mutableMapOf()
        val milkyRegistry: MutableMap<String, ActionTranslator> = mutableMapOf()
        fun init() {
            onebotRegistry.clear()
            milkyRegistry.clear()

            //////////////////////////////////////////
            // 系统 API
            // https://milky.ntqqrev.org/api/system
            //////////////////////////////////////////

            register("get_login_info", responseObject { request, data ->
                add("user_id", data["uin"])
                add("nickname", data["nickname"])
            })
            register("get_version_info", "get_impl_info", responseObject { request, data ->
                add("app_name", data["impl_name"])
                add("app_version", data["impl_version"])
                addProperty("protocol_version", "v11")
                for (key in data.keySet()) {
                    add(key, data[key])
                }
            })
            register("get_stranger_info", "get_user_profile", responseObject { request, data ->
                add("user_id", request["user_id"])
                add("nickname", data["nickname"])
                add("qid", data["qid"])
                add("sex", data["age"])
                add("sex", data["sex"])
                add("level", data["level"])
                addIfNotExists("login_days", 0)

                add("remark", data["remark"])
                add("bio", data["bio"])
                add("country", data["country"])
                add("city", data["city"])
                add("school", data["school"])
            })
            register("get_friend_list", responseArray { request, data ->
                for (element in data["friends"].asJsonArray) {
                    val obj = element.asJsonObject
                    // 相同类型与名称的字段
                    // user_id, nickname, sex, qid
                    // 以下是缺少的字段
                    obj.addIfNotExists("age", 0)
                    obj.addIfNotExists("level", 0)
                    obj.addIfNotExists("login_days", 0)
                    add(obj)
                }
            })
            register("get_friend_info", responseObject { request, data ->
                val obj = data["friend"].asJsonObject
                for (key in obj.keySet()) {
                    add(key, obj[key])
                }
                // 同上 get_friend_list
                addIfNotExists("age", 0)
                addIfNotExists("level", 0)
                addIfNotExists("login_days", 0)
            })
            register("get_group_list", responseArray { request, data ->
                for (element in data["groups"].asJsonArray) {
                    val obj = element.asJsonObject
                    // 相同类型与名称的字段
                    // group_id, group_name, member_count, max_member_count
                    // 以下是缺少的字段
                    obj.addIfNotExists("group_memo", "")
                    obj.addIfNotExists("group_create_time", 0L)
                    obj.addIfNotExists("group_level", 0)
                    add(obj)
                }
            })
            register("get_group_info", responseObject { request, data ->
                val obj = data["group"].asJsonObject
                for (key in obj.keySet()) {
                    add(key, obj[key])
                }
                // 同上 get_group_list
                addIfNotExists("group_memo", "")
                addIfNotExists("group_create_time", 0L)
                addIfNotExists("group_level", 0)
            })
            register("get_group_member_list", responseArray { request, data ->
                for (element in data["members"].asJsonArray) {
                    val obj = element.asJsonObject
                    // 相同类型与名称的字段
                    // user_id, nickname, sex, group_id,
                    // card, title, level, role, join_time, last_sent_time
                    // 以下是缺少的字段
                    if (data.has("shut_up_end_time")) {
                        obj.add("shut_up_timestamp", data["shut_up_end_time"])
                    } else {
                        obj.addProperty("shut_up_timestamp", -1L)
                    }
                    add(obj)
                }
            })
            register("get_group_member_info", responseObject { request, data ->
                val obj = data["member"].asJsonObject
                for (key in obj.keySet()) {
                    add(key, obj[key])
                }
                // 同上 get_group_member_list
                if (data.has("shut_up_end_time")) {
                    add("shut_up_timestamp", data["shut_up_end_time"])
                } else {
                    addProperty("shut_up_timestamp", -1L)
                }
            })
            register("get_cookies", requestParams { params ->
                add("domain", params["domain"])
            }, responseObject { request, data ->
                add("cookies", data["cookies"])
            })
            register("get_csrf_token", responseObject { request, data ->
                add("token", data["csrf_token"])
            })
            // TODO: 实现 get_credentials

            //////////////////////////////////////////
            // 消息 API
            // https://milky.ntqqrev.org/api/message
            //////////////////////////////////////////

            register("send_private_msg", "send_private_message", requestParams { params ->
                add("message", MessageTranslator.onebotToMilkyOutgoing(params["message"].asJsonArray))
            }, responseObject { request, data ->
                // TODO: 需要解决一个问题，Onebot 和 mirai 都是用 Int 储存消息 ID 的，需要找一个方法将其改为 Long
                add("message_id", data["message_seq"])
            })
        }

        fun Companion.register(sameAction: String, resp: (JsonObject, JsonObject) -> JsonElement) = register(sameAction, sameAction, resp)
        fun Companion.register(sameAction: String, req: (JsonObject) -> JsonObject, resp: (JsonObject, JsonObject) -> JsonElement) = register(sameAction, sameAction, req, resp)
        fun Companion.register(onebotAction: String, milkyAction: String, resp: (JsonObject, JsonObject) -> JsonElement) = register(onebotAction, milkyAction, { JsonObject() }, resp)
        fun Companion.register(onebotAction: String, milkyAction: String, req: (JsonObject) -> JsonObject, resp: (JsonObject, JsonObject) -> JsonElement) {
            val impl = object : ActionTranslator {
                override val action: String = milkyAction
                override fun request(params: JsonObject): JsonObject = req(params)
                override fun response(request: JsonObject, data: JsonObject): JsonElement = resp(request, data)
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
            // Milky 的返回格式与 Onebot 相同，如果失败，直接返回 Milky 的结果即可
            if (json["status"].asString != "ok") return json
            val data = impl.response(request, json["data"].asJsonObject)
            return JsonObject().apply {
                add("status", json["status"])
                add("retcode", json["retcode"])
                add("data", data)
                request["echo"]?.also { add("echo", it) }
            }
        }
    }
}
