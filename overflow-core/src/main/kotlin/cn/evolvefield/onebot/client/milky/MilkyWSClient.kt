package cn.evolvefield.onebot.client.milky

import cn.evolvefield.onebot.client.config.BotConfig
import cn.evolvefield.onebot.client.connection.CloseCode
import cn.evolvefield.onebot.client.connection.IAdapter
import cn.evolvefield.onebot.client.connection.WSClientReconnectController
import cn.evolvefield.onebot.client.connection.eventDispatcher
import cn.evolvefield.onebot.client.core.Bot
import cn.evolvefield.onebot.client.handler.ActionHandler
import cn.evolvefield.onebot.client.handler.EventHolder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import net.mamoe.mirai.event.events.BotOfflineEvent
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.slf4j.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URI
import kotlin.text.ifEmpty

internal class MilkyWSClient(
    override val scope: CoroutineScope,
    private val config: BotConfig,
    private val apiURI: URI,
    eventURI: URI,
    override val logger: Logger,
    override val actionHandler: ActionHandler,
    private val retryTimes: Int = 5,
    private val retryWaitMills: Long = 5000L,
    private val retryRestMills: Long = 60000L,
    private val header: Map<String, String> = mapOf(),
) : WebSocketClient(eventURI, header), IAdapter {
    internal var botConsumer: suspend (Bot) -> Unit = {}
    override val eventsHolder: MutableMap<Long, MutableList<EventHolder>> = mutableMapOf()
    private val reconnect: WSClientReconnectController = object: WSClientReconnectController(
        config.parentJob, scope, logger, retryTimes, retryWaitMills, retryRestMills
    ) {
        override fun reconnectInternal(): Boolean = reconnectBlocking()
        override fun onGivingUpReconnecting() {
            loginBot?.eventDispatcher {
                broadcastAsync(BotOfflineEvent.Dropped(it, null))
            }
        }
    }
    private var scheduleClose = false
    private var loginBot: Bot? = null
    private val translator: MilkyTranslator = MilkyTranslator()

    init {
        connectionLostTimeout = Math.max(0, config.heartbeatCheckSeconds)
        @OptIn(InternalCoroutinesApi::class)
        config.parentJob?.run { Job(this) }?.invokeOnCompletion(
            onCancelling = true,
            invokeImmediately = true,
        ) { if (isOpen) close() }
    }

    suspend fun createBot(): Bot {
        val bot = Bot(this, this, config, actionHandler)
        botConsumer.invoke(bot)
        loginBot = bot
        return bot
    }

    suspend fun connectSuspend(): Boolean {
        if (super.connectBlocking()) return true
        return reconnect.connectDef.await()
    }

    override fun connect() {
        scheduleClose = false
        super.connect()
    }

    override fun close() {
        scheduleClose = true
        super.close()
    }

    override fun onOpen(handshakedata: ServerHandshake) {
        logger.info("▌ 已连接到服务器 ┈━═☆")
    }

    override fun onMessage(message: String) {
        val translated: JsonObject
        try {
            val json = JsonParser.parseString(message).asJsonObject
            // 将 Milky 翻译成 Onebot，再丢给 Overflow 进行处理
            translated = translator.milkyEventToOnebotEvent(json)
        } catch (e: JsonSyntaxException) {
            logger.error("Json语法错误: {}", message)
            return
        }
        handleReceiveEvent(translated)

    }

    override fun send(text: String) {} // 禁止向 /event 端点发送消息
    internal suspend fun send(json: JsonObject): JsonObject {
        // 将 Onebot 翻译为 Milky，改为发送给 HTTP 接口 apiURI，并翻译返回结果为 Onebot
        val translated = translator.onebotActionToMilkyAction(json)
        val path = translated.first
        val request = translated.second
        val resultText: String
        withContext(Dispatchers.IO) {
            // 构建 /api 端点访问地址
            val newURI = apiURI.resolve(buildString {
                append(apiURI.path.removeSuffix("/"))
                append("/")
                append(path)
                if (apiURI.query.isNotEmpty()) {
                    append("?")
                    append(apiURI.query)
                }
            })
            val conn = newURI.toURL().openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            for ((key, value) in header) {
                conn.addRequestProperty(key, value)
            }
            conn.doInput = true
            conn.doOutput = true
            PrintWriter(conn.getOutputStream()).use { writer ->
                writer.print(request.toString())
                writer.flush()
                BufferedReader(InputStreamReader(conn.getInputStream(), Charsets.UTF_8)).use { reader ->
                    resultText = reader.readText()
                }
            }
        }
        try {
            val response = JsonParser.parseString(resultText).asJsonObject
            // 将 Milky 翻译成 Onebot，返回给 Overflow 进行处理
            return translator.milkyResultToOnebotResult(path, request, response)
        } catch (e: JsonSyntaxException) {
            logger.error("Json语法错误: {}", resultText)
            return JsonObject().apply {
                addProperty("status", "failed")
                addProperty("retcode", -1)
                addProperty("message", "Json语法错误")
            }
        }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        logger.info(
            "▌ 服务器连接因 {} 已关闭 (关闭码: {})",
            reason.ifEmpty { "未知原因" },
            CloseCode.valueOf(code) ?: code
        )
        unlockMutex()

        // 自动重连
        if (!scheduleClose) reconnect.retry()
        else loginBot?.eventDispatcher {
            broadcastAsync(BotOfflineEvent.Active(it, null))
        }
    }

    override fun onError(ex: Exception) {
        logger.error("▌ 出现错误 {} 或未连接 ┈━═☆", ex.localizedMessage)
    }

    companion object {
        fun create(
            scope: CoroutineScope,
            config: BotConfig,
            apiURI: URI,
            eventURI: URI,
            logger: Logger,
            actionHandler: ActionHandler,
            retryTimes: Int,
            retryWaitMills: Long,
            retryRestMills: Long,
            header: Map<String, String> = mapOf()
        ): MilkyWSClient {
            return MilkyWSClient(
                scope,
                config,
                apiURI,
                eventURI,
                logger,
                actionHandler,
                retryTimes,
                retryWaitMills,
                retryRestMills,
                header
            )
        }
    }
}
