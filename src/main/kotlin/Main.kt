import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>): Unit = runBlocking {
    val port = args.getOrNull(1)?.toIntOrNull() ?: 3001
    val rootPath = args.getOrNull(0) ?: ""
    println("Set root path to $rootPath")
    val tools = createTools(rootPath)

    runSseMcpServer(port, tools)
}

private fun createTools(rootPath: String): List<Pair<Tool,suspend (CallToolRequest) -> CallToolResult>> {

    val safeFileManager = SafeFileManager(rootPath)
    val saveFileTool = SaveFileTool(safeFileManager)
    val loadFileTool = LoadFileTool(safeFileManager)
    val readDirTool = ReadDirTool(safeFileManager)
    val gradleCreateKotlinProjectTool = GradleCreateKotlinProjectTool(safeFileManager)

    return listOf(saveFileTool.create(),
        loadFileTool.create(),
        readDirTool.create(),
        gradleCreateKotlinProjectTool.create())
}

suspend fun runSseMcpServer(port: Int, tools: List<Pair<Tool, suspend (CallToolRequest) -> CallToolResult>>) {
    println("Starting sse server on port $port")
    println("Use inspector to connect to the http://localhost:$port/sse")

    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        mcp {
            return@mcp configureServer(tools)
        }
    }.startSuspend(wait = true)
}

fun configureServer(tools: List<Pair<Tool, suspend (CallToolRequest) -> CallToolResult>>): Server {
    val server = Server(
        Implementation(
            name = "mcp-kotlin server",
            version = "0.1.0"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(
                prompts = ServerCapabilities.Prompts(listChanged = true),
                resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                tools = ServerCapabilities.Tools(listChanged = true),
            )
        )
    )
    tools.forEach { (tool, handler) -> server.addTool(tool, handler) }

    return server
}