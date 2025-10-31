package cn.evolvefield.onebot.client.milky

import com.google.gson.JsonObject
import org.jetbrains.annotations.ApiStatus

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
        TODO()
    }

    /**
     * 将 Onebot 主动操作转换为 Milky 主动操作
     * @return
     * * [String] api 端点访问路径
     * * [JsonObject] HTTP请求体
     */
    fun onebotActionToMilkyAction(json: JsonObject): Pair<String, JsonObject> {
        TODO()
    }

    /**
     * 将 Milky 主动操作的结果转换为 Onebot 主动操作的结果
     */
    fun milkyResultToOnebotResult(path: String, request: JsonObject, response: JsonObject): JsonObject {
        TODO()
    }

}
