package cn.evolvefield.onebot.client.milky

import cn.evolvefield.onebot.client.milky.translator.ActionTranslator
import cn.evolvefield.onebot.client.milky.translator.EventTranslator
import com.google.gson.JsonObject
import org.jetbrains.annotations.ApiStatus
import java.lang.IllegalStateException

/**
 * Milky 与 Onebot 转换器
 *
 * 暂未编写完成，方法签名随时会改变
 */
@ApiStatus.Experimental
internal class MilkyTranslator {

    /**
     * 将 Milky 事件转换为 Onebot 事件
     */
    fun milkyEventToOnebotEvent(json: JsonObject): JsonObject {
        val eventType = json["event_type"].asString
        return EventTranslator.execute(eventType, json)
            ?: throw IllegalStateException("无法翻译 $eventType 为 Onebot 事件")
    }

    /**
     * 将 Onebot 主动操作转换为 Milky 主动操作
     * @return
     * * [String] api 端点访问路径
     * * [JsonObject] HTTP请求体
     */
    fun onebotActionToMilkyAction(json: JsonObject): Pair<String, JsonObject> {
        val action = json["action"].asString
        val params = json["params"].asJsonObject
        return ActionTranslator.execute(action, params)
            ?: throw IllegalStateException("无法翻译 $action 为 Milky 的 API")
    }

    /**
     * 将 Milky 主动操作的结果转换为 Onebot 主动操作的结果
     */
    fun milkyResultToOnebotResult(path: String, request: JsonObject, response: JsonObject): JsonObject {
        return ActionTranslator.execute(path, request, response)
            ?: throw IllegalStateException("无法翻译 $path 为 Onebot 的主动操作返回结果")
    }

    companion object {
        fun init() {
            ActionTranslator.init()
            EventTranslator.init()
        }
    }
}
