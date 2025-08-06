import java.nio.file.Path
import java.nio.file.Paths

class SafeFileManager(baseDir: String) {
    private val root: Path = Paths.get(baseDir).toAbsolutePath().normalize()

    init {
        if (!root.toFile().exists()) {
            root.toFile().mkdirs()
        }
        if (!root.toFile().isDirectory) {
            throw IllegalArgumentException("The specified path is not a directory: $root")
        }
    }

    private fun resolveSafe(path: String): Path {
        val resolved = root.resolve(path).normalize()
        if (!resolved.startsWith(root)) {
            throw SecurityException("Access to files outside the allowed directory is prohibited: $resolved")
        }
        return resolved
    }

    fun writeFile(relativePath: String, content: String): Pair<Boolean, String> {
        return try {
            val file = resolveSafe(relativePath).toFile()
            file.parentFile.mkdirs()
            file.writeText(content)
            true to "File saved successfully"
        } catch (e: Exception) {
            false to "Failed to save file: ${e.message}"
        }
    }

    fun readFile(relativePath: String): Pair<Boolean, String> {
        return try {
            val file = resolveSafe(relativePath).toFile()
            if (!file.exists()) throw IllegalArgumentException("File not found: $file")
            true to file.readText()
        } catch (e: Exception) {
            false to "Failed to read file: ${e.message}"
        }
    }
}