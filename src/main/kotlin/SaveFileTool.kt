import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.error
import io.modelcontextprotocol.kotlin.sdk.ok
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

private fun getTrimmedArg(request: CallToolRequest, name: String): String? =
    request.arguments[name]?.toString()?.trim('"')

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
        val path = getTrimmedArg(request,"path")
            ?: return CallToolResult.error("Path argument is required")
        val content = getTrimmedArg(request,"content")
            ?: return CallToolResult.error("Content argument is required")
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
        val path = getTrimmedArg(request,"path")
            ?: return CallToolResult.error("Path argument is required")
        val (success, content) = safeFileManager.readFile(path)
        if (!success) {
            return CallToolResult.error(content)
        }

        return CallToolResult.ok(content)
    }
}

class GradleCreateKotlinProjectTool(val safeFileManager: SafeFileManager) {
    val gradleManager = GradleManager(safeFileManager)
    private companion object {
        const val DEFAULT_DIRECTORY = "kotlin-project"
    }

    fun create(): Pair<Tool, suspend (CallToolRequest) -> CallToolResult> = Tool(
        name = "create-kotlin-project",
        description = "Create a Kotlin project",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("directoryName", JsonPrimitive("string"))
                put("projectName", JsonPrimitive("string"))
            },
            required = listOf("projectName")
        ),
        outputSchema = Tool.Output(properties = buildJsonObject {
            put("directoryName", JsonPrimitive("string"))
        },),
        annotations = null
    ) to { request -> createProject(request) }

    private fun createProject(request: CallToolRequest): CallToolResult {

        val directoryName: String = getTrimmedArg(request, "directoryName") ?: DEFAULT_DIRECTORY
        val projectName: String = getTrimmedArg(request, "projectName")
            ?: return CallToolResult.error("projectName argument is required")

        val (success, content) = gradleManager.createProject(directoryName, projectName)
        if (!success) {
            return CallToolResult.error(content)
        }

        return CallToolResult.ok(content)
    }
}