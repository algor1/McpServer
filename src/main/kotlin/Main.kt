import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.coroutines.runBlocking
import shared.configureServer

fun main(args: Array<String>): Unit = runBlocking {
    val command = args.firstOrNull() ?: "--sse-server-ktor"
    val port = args.getOrNull(1)?.toIntOrNull() ?: 3001
    runSseMcpServerUsingKtorPlugin(port)
}

suspend fun runSseMcpServerUsingKtorPlugin(port: Int): Unit {
    println("Starting sse server on port $port")
    println("Use inspector to connect to the http://localhost:$port/sse")

    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        mcp {
            return@mcp configureServer()
        }
    }.startSuspend(wait = true)
}