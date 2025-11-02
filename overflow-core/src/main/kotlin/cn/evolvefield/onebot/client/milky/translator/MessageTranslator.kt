package cn.evolvefield.onebot.client.milky.translator

import com.google.gson.JsonArray
import com.google.gson.JsonObject

internal object MessageTranslator {
    fun milkyIncomingToOnebot(milkMessages: JsonArray): JsonArray {
        val array = JsonArray()
        for (element in milkMessages) {
            array.add(milkyIncomingToOnebot(element.asJsonObject))
        }
        return array
    }

    fun onebotToMilkyOutgoing(onebotMessages: JsonArray): JsonArray {
        val array = JsonArray()
        for (element in onebotMessages) {
            array.add(onebotToMilkyOutgoing(element.asJsonObject))
        }
        return array
    }

    fun milkyIncomingToOnebot(milkMessage: JsonObject): JsonObject {
        val obj = JsonObject()
        // 参考文档:
        // https://docs.go-cqhttp.org/cqcode
        // https://github.com/botuniverse/onebot-11/blob/master/message/array.md
        // https://milky.ntqqrev.org/struct/IncomingSegment
        val type = milkMessage["type"].asString
        val data = milkMessage["data"].asJsonObject
        when (type) {
            "text" -> {
                obj.addProperty("type", "text")
                obj.add("data", data)
            }
            "mention" -> {
                obj.addProperty("type", "at")
                obj.add("data", buildObject {
                    val userId = data["user_id"].asLong
                    addProperty("qq", "$userId")
                    addProperty("name", "$userId")
                })
            }
            "mention_all" -> {
                obj.addProperty("type", "at")
                obj.add("data", buildObject {
                    addProperty("qq", "all")
                })
            }
            "face" -> {
                obj.addProperty("type", "face")
                obj.add("data", buildObject {
                    add("id", data["face_id"])
                })
            }
            "reply" -> {
                obj.addProperty("type", "reply")
                obj.add("data", buildObject {
                    add("id", data["message_seq"])
                    add("seq", data["message_seq"])
                    // TODO: 缺少字段 text, qq, time
                })
            }
            "image" -> {
                obj.addProperty("type", "image")
                obj.add("data", buildObject {
                    add("file", data["temp_url"])
                    when (data["sub_type"].asString) {
                        "normal" -> addProperty("sub_type", 0)
                        "sticker" -> addProperty("sub_type", 7)
                    }
                    add("summary", data["summary"])
                    add("width", data["width"])
                    add("height", data["height"])
                    add("resource_id", data["resource_id"])
                })
            }
            "record" -> {
                obj.addProperty("type", "record")
                obj.add("data", buildObject {
                    add("file", data["temp_url"])
                    add("resource_id", data["resource_id"])
                    add("duration", data["duration"])
                })
            }
            "video" -> {
                obj.addProperty("type", "video")
                obj.add("data", buildObject {
                    add("file", data["temp_url"])
                    add("resource_id", data["resource_id"])
                    add("width", data["width"])
                    add("height", data["height"])
                    add("duration", data["duration"])
                })
            }
            "file" -> {
                obj.addProperty("type", "milky_file")
                obj.add("data", data) // Onebot 没有文件消息定义
            }
            "forward" -> {
                obj.addProperty("type", "forward")
                obj.add("data", buildObject {
                    add("id", data["forward_id"])
                })
            }
            "market_face" -> {
                // 使用 LLOnebot 的商城表情格式
                obj.addProperty("type", "milky_market_face")
                obj.add("data", data) // Onebot 没有商城表情消息定义，也没有仅含 url 的第三方定义
            }
            "light_app" -> {
                obj.addProperty("type", "milky_light_app")
                obj.add("data", data)
            }
            "xml" -> {
                obj.addProperty("type", "milky_xml")
                obj.add("data", data)
            }
            else -> throw IllegalStateException("无法转换消息 $type 为 Onebot 消息")
        }
        return obj
    }

    fun onebotToMilkyOutgoing(onebotMessage: JsonObject): JsonObject {
        val obj = JsonObject()
        // 参考文档:
        // https://docs.go-cqhttp.org/cqcode
        // https://github.com/botuniverse/onebot-11/blob/master/message/array.md
        // https://milky.ntqqrev.org/struct/OutgoingSegment
        val type = onebotMessage["type"].asString
        val data = onebotMessage["data"].asJsonObject
        when (type) {
            "text" -> {
                obj.addProperty("type", "text")
                obj.add("data", data)
            }
            "at" -> {
                if (data["qq"].asString == "all") {
                    obj.addProperty("type", "mention_all")
                    obj.add("data", JsonObject())
                } else {
                    obj.addProperty("type", "mention")
                    obj.add("data", buildObject {
                        addProperty("user_id", data["qq"].asString.toLong())
                    })
                }
            }
            "face" -> {
                obj.addProperty("type", "face")
                obj.add("data", buildObject {
                    add("face_id", data["id"])
                })
            }
            "reply" -> {
                obj.addProperty("type", "reply")
                obj.add("data", buildObject {
                    add("message_seq", data["id"])
                })
            }
            "image" -> {
                obj.addProperty("type", "image")
                obj.add("data", buildObject {
                    add("uri", data["file"])
                    if (data.has("summary")) add("summary", data["summary"])
                    if (data.has("sub_type")) when (data["sub_type"].asInt) {
                        1 -> addProperty("sub_type", "normal")
                        7 -> addProperty("sub_type", "sticker")
                    }
                })
            }
            "record" -> {
                obj.addProperty("type", "image")
                obj.add("data", buildObject {
                    add("uri", data["file"])
                })
            }
            "video" -> {
                obj.addProperty("type", "video")
                obj.add("data", buildObject {
                    add("uri", data["file"])
                    if (data.has("thumb_uri")) add("thumb_uri", data["thumb_uri"])
                })
            }
            "forward" -> {
                obj.addProperty("type", "forward")
                obj.add("data", buildObject {
                    val messages = JsonArray()
                    for (element in data["nodes"].asJsonArray) {
                        val message = JsonObject()
                        val node = element.asJsonObject
                        val onebotMessages = node["messages"].asJsonArray
                        message.add("user_id", node["user_id"])
                        message.add("sender_name", node["sender_name"])
                        message.add("segments", onebotToMilkyOutgoing(onebotMessages))
                    }
                    add("messages", messages)
                })
            }
            else -> throw IllegalStateException("无法转换消息 $type 为 Milky 消息")
        }
        return obj
    }
}