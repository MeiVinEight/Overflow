package cn.evolvefield.onebot.client.milky

import cn.evolvefield.onebot.client.connection.OneBotProducer
import cn.evolvefield.onebot.client.core.Bot
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

internal class MilkyProducer(
    private val client: MilkyWSClient,
) : OneBotProducer {
    override fun invokeOnClose(block: () -> Unit) = TODO("客户端暂不支持断线重连")

    override fun close() = client.close()

    override fun setBotConsumer(consumer: suspend (Bot) -> Unit) {
        client.botConsumer = consumer
    }

    override suspend fun awaitNewBotConnection(duration: Duration): Bot? {
        return kotlin.runCatching {
            withTimeout(duration) {
                if (client.connectSuspend()) client.createBot() else null
            }
        }.getOrNull()
    }
}
