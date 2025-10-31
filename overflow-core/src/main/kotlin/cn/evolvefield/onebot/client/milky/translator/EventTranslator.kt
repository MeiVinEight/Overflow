package cn.evolvefield.onebot.client.milky.translator

import com.google.gson.JsonObject

internal interface EventTranslator {
    /**
     * Milky 事件转换为 Onebot 事件，可以忽略 self_id 和 time
     */
    fun toOnebot(json: JsonObject): JsonObject

    companion object {
        val milkyRegistry: MutableMap<String, EventTranslator> = mutableMapOf()
        fun init() {
            milkyRegistry.clear()
            register("message_receive") {
                when (val scene = it["message_scene"].asString) {
                    "friend" -> {
                        TODO()
                    }
                    "group" -> {
                        TODO()
                    }
                    "temp" -> {
                        TODO()
                    }
                    else -> throw IllegalStateException("不支持的消息场景 $scene")
                }
            }
        }
        fun register(milkyEvent: String, onebotEvent: (JsonObject) -> JsonObject) {
            milkyRegistry[milkyEvent] = object : EventTranslator {
                override fun toOnebot(json: JsonObject): JsonObject = onebotEvent(json)
            }
        }
        fun execute(milkyEvent: String, json: JsonObject): JsonObject? {
            val impl = milkyRegistry[milkyEvent] ?: return null
            val result = impl.toOnebot(json)
            // 添加共同字段
            result.add("self_id", json["self_id"])
            result.add("time", json["time"])
            return result
        }
    }
}
