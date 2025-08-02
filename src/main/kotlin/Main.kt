import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import java.io.File
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.Role
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.error
import io.modelcontextprotocol.kotlin.sdk.ok
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

fun main(args: Array<String>): Unit = runBlocking {
    val port = args.getOrNull(1)?.toIntOrNull() ?: 3001
    runSseMcpServerUsingKtorPlugin(port)
}

suspend fun runSseMcpServerUsingKtorPlugin(port: Int) {
    println("Starting sse server on port $port")
    println("Use inspector to connect to the http://localhost:$port/sse")

    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        mcp {
            return@mcp configureServer()
        }
    }.startSuspend(wait = true)
}

fun configureServer(): Server {
    val server = Server(
        Implementation(
            name = "mcp-kotlin test server",
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

    server.addPrompt(
        name = "Kotlin Developer",
        description = "Develop small kotlin applications",
        arguments = listOf(
            PromptArgument(
                name = "Project Name",
                description = "Project name for the new project",
                required = true
            )
        )
    ) { request ->
        GetPromptResult(
            "Description for ${request.name}",
            messages = listOf(
                PromptMessage(
                    role = Role.user,
                    content = TextContent("Develop a kotlin project named <name>${request.arguments?.get("Project Name")}</name>")
                )
            )
        )
    }

    // Add a tool
    server.addTool(
        name = "kotlin-sdk-tool",
        description = "A test tool",
        inputSchema = Tool.Input()
    ) { request ->
        CallToolResult(
            content = listOf(TextContent("Hello, world!"))
        )
    }

    server.addTool(
        name = "save-file",
        description = "Save content to a file",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("path", JsonPrimitive("string"))
                put("content", JsonPrimitive("string"))
                },
            required = listOf("path", "content")
        )
    ){ request -> saveFile(request)

            }

    // Add a resource
    server.addResource(
        uri = "https://search.com/",
        name = "Web Search",
        description = "Web search engine",
        mimeType = "text/html"
    ) { request ->
        ReadResourceResult(
            contents = listOf(
                TextResourceContents("Placeholder content for ${request.uri}", request.uri, "text/html")
            )
        )
    }

    return server
}

fun saveFile(request: CallToolRequest): CallToolResult {
    val path = request.arguments["path"]?.toString()?.trim('"')  ?: return CallToolResult.error("Path argument is required")
    val content = request.arguments["content"]?.toString()?.trim('"')  ?: return CallToolResult.error("Content argument is required")


    val (success, message) = saveFile(path, content)
    if (!success) {
        return CallToolResult.error(message)
    }

    return CallToolResult.ok("Successfully saved file to $path")
}

fun saveFile(path: String, content: String): Pair<Boolean, String> {
    return try {
        File(path).writeText(content)
        true to "File saved successfully"
    } catch (e: Exception) {
        false to "Failed to save file: ${e.message}"
    }
}