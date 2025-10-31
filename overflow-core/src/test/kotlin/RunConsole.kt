import net.mamoe.mirai.console.ConsoleFrontEndImplementation
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.terminal.MiraiConsoleImplementationTerminal
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import java.net.URI

@OptIn(ConsoleExperimentalApi::class, ConsoleFrontEndImplementation::class)
suspend fun main() {
    val uri = URI("http://127.0.0.1:8080/api?access_token=awa")
    println(uri.resolve(buildString {
        append(uri.path.removeSuffix("/"))
        append("/")
        append("send_message")
        if (uri.query.isNotEmpty()) {
            append("?")
            append(uri.query)
        }
    }))
}