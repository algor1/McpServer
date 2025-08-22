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

    fun mkdir(relativePath: String): Pair<Boolean, String> {
        return try {
            val dir = resolveSafe(relativePath).toFile()
            if (dir.exists()) {
                if (dir.isDirectory) {
                    true to dir.absolutePath
                } else {
                    false to "A file with the same name already exists: $dir"
                }
            } else {
                if (dir.mkdirs()) {
                    true to dir.absolutePath
                } else {
                    false to "Failed to create directory: $dir"
                }
            }
        } catch (e: Exception) {
            false to "Error creating directory: ${e.message}"
        }
    }
}

class GradleManager(val safeFileManager: SafeFileManager) {
    fun createProject(directoryName: String, projectName: String): Pair<Boolean, String> {
        return try {
            val mkdirResult = safeFileManager.mkdir(directoryName)
            if (!mkdirResult.first) return mkdirResult
            val process = ProcessBuilder( "cmd", "/c", "gradle", "init", "--dsl", "kotlin", "--use-defaults", "--type", "kotlin-application", "--project-name", projectName)
                .directory(Paths.get(mkdirResult.second).toFile())
                .start()
            process.waitFor()
            true to "Project created successfully"
        } catch (e: Exception) {
            false to "Failed to create project: ${e.message}"
        }
    }

}