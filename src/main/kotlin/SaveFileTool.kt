import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.error
import io.modelcontextprotocol.kotlin.sdk.ok
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class SaveFileTool(val safeFileManager: SafeFileManager) {

    fun create(): Pair<Tool, suspend (CallToolRequest) -> CallToolResult> = Tool(
        name = "save-file",
        description = "Save content to a file",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("path", JsonPrimitive("string"))
                put("content", JsonPrimitive("string"))
            },
            required = listOf("path", "content")
        ),
        outputSchema = null,
        annotations = null
    ) to { request -> saveFile(request) }

    private fun saveFile(request: CallToolRequest): CallToolResult {
        val path = request.arguments["path"]?.toString()?.trim('"')  ?: return CallToolResult.error("Path argument is required")
        val content = request.arguments["content"]?.toString()?.trim('"')  ?: return CallToolResult.error("Content argument is required")
        val (success, message) = safeFileManager.writeFile(path, content)
        if (!success) {
            return CallToolResult.error(message)
        }

        return CallToolResult.ok("Successfully saved file to $path")
    }
}

class LoadFileTool(val safeFileManager: SafeFileManager) {

    fun create(): Pair<Tool, suspend (CallToolRequest) -> CallToolResult> = Tool(
        name = "load-file",
        description = "Load a file content",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("path", JsonPrimitive("string"))
            },
            required = listOf("path")
        ),
        outputSchema = Tool.Output(properties = buildJsonObject {
            put("content", JsonPrimitive("string"))
        },),
        annotations = null
    ) to { request -> loadFile(request) }

    private fun loadFile(request: CallToolRequest): CallToolResult {
        val path = request.arguments["path"]?.toString()?.trim('"') ?: return CallToolResult.error("Path argument is required")
        val (success, content) = safeFileManager.readFile(path)
        if (!success) {
            return CallToolResult.error(content)
        }

        return CallToolResult.ok(content)
    }
}