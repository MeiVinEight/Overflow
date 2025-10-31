package cn.evolvefield.onebot.client.milky.translator

import com.google.gson.JsonArray
import com.google.gson.JsonObject

internal fun buildObject(block: JsonObject.() -> Unit): JsonObject = JsonObject().apply(block)
internal fun buildArray(block: JsonArray.() -> Unit): JsonArray = JsonArray().apply(block)
internal fun requestParams(block: JsonObject.() -> Unit): JsonObject = buildObject(block)
internal fun responseObject(block: JsonObject.() -> Unit): JsonObject = buildObject(block)
internal fun responseArray(block: JsonArray.() -> Unit): JsonArray = buildArray(block)

internal fun JsonObject.addIfNotExists(key: String, value: Number) {
    if (!has(key)) addProperty(key, value)
}
internal fun JsonObject.addIfNotExists(key: String, value: String) {
    if (!has(key)) addProperty(key, value)
}
internal fun JsonObject.addIfNotExists(key: String, value: Boolean) {
    if (!has(key)) addProperty(key, value)
}
