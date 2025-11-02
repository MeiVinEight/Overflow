package cn.evolvefield.onebot.client.milky.translator

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

internal interface TranslatorJsonUtil

internal fun TranslatorJsonUtil.buildObject(block: JsonObject.() -> Unit): JsonObject = JsonObject().apply(block)
internal fun TranslatorJsonUtil.buildArray(block: JsonArray.() -> Unit): JsonArray = JsonArray().apply(block)
internal fun TranslatorJsonUtil.requestParams(block: JsonObject.(JsonObject) -> Unit): (JsonObject) -> JsonObject = {
    buildObject { block(this, it) }
}
internal fun TranslatorJsonUtil.responseObject(block: JsonObject.(JsonObject, JsonObject) -> Unit): (JsonObject, JsonObject) -> JsonElement = { request, data ->
    buildObject { block(this, request, data) }
}
internal fun TranslatorJsonUtil.responseObjectOfData(block: JsonObject.(JsonObject, JsonObject) -> Unit): (JsonObject, JsonObject) -> JsonElement = { request, data ->
    data.also { block(it, request, data) }
}
internal fun TranslatorJsonUtil.responseArray(block: JsonArray.(JsonObject, JsonObject) -> Unit): (JsonObject, JsonObject) -> JsonElement = { request, data ->
    buildArray { block(this, request, data) }
}

internal fun JsonObject.addIfNotExists(key: String, value: Number) {
    if (!has(key)) addProperty(key, value)
}
internal fun JsonObject.addIfNotExists(key: String, value: String) {
    if (!has(key)) addProperty(key, value)
}
internal fun JsonObject.addIfNotExists(key: String, value: Boolean) {
    if (!has(key)) addProperty(key, value)
}
