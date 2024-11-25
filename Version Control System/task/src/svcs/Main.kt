package svcs

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.io.appendText
import kotlin.io.path.listDirectoryEntries

fun createVcsDir() {
    File("vcs").mkdirs()
    File("vcs/commits").mkdirs()
    File("vcs/config.txt").createNewFile()
    File("vcs/index.txt").createNewFile()
    File("vcs/log.txt").createNewFile()
}

fun configFun(args: Array<String>) {
    try {
        val workDir = System.getProperty("user.dir")
        val vcsDir = File("$workDir/vcs")

        val configFile = File(vcsDir, "config.txt")

        var username: String

        if (args.first() == "config" && args.size == 1) {

            if (!configFile.exists()) {
                print("Please, tell me who you are.")
            }

            if (configFile.exists()) {
                username = configFile.readText()

                if (username.isBlank()) {
                    println("Please, tell me who you are.")
                } else {
                    println("The username is $username.")
                }
            }
        }
        if (args.first() == "config" && args.size > 1) {
            username = args[1]
            configFile.writeText(username)
            print("The username is $username.\n")
        }
    }
    catch (e: Exception) {
        print(e)
    }

}

fun addFun(args: Array<String>) {
    try {
        val workDir = System.getProperty("user.dir")
        val vcsDir = File("$workDir/vcs")

        if (!vcsDir.exists()) {
            vcsDir.mkdirs()
        }

        val indexFile = File(vcsDir, "index.txt")
        val fileName: String

        if (args.first() == "add" && args.size == 1) {
            if (indexFile.exists()) {
                val lines = indexFile.readLines()

                if (lines.isNotEmpty()) {
                    print("Tracked files:\n")
                    for (line in lines) {
                        println("$line")
                    }
                } else println("Add a file to the index.")
            } else {
                print("Add a file to the index.")
            }

        }

        if (args.first() == "add" && args.size > 1) {
            fileName = args[1]

            if (!File(workDir, fileName).exists()) {
                print("Can't find '$fileName'.")
            }

            if (File(workDir, fileName).exists()) {
                indexFile.appendText("$fileName\n")
                print("The file '$fileName' is tracked.")
            }

        }
    }

    catch (e: Exception) {
        print(e)
    }
}

fun commitFun(args: Array<String>) {
    val username = File("vcs/config.txt").readText()
    val trackedFiles = File("vcs/index.txt").readLines()
    val commits = File("vcs/log.txt").readLines().chunked(3).reversed()
    val head = commits.firstOrNull()?.first()

    if (args.size == 1) {
        println("Message was not passed.")
    } else if (args.size == 2) {
        val changedFiles = trackedFiles.filter { name ->
            File(name).hash() != File("vcs/commits/$head/$name").hash()
        }
        if (head == null || changedFiles.isNotEmpty()) {
            val message = args.last()
            val hash = trackedFiles.map { File(it).readText() }.joinToString("").hash()
            File("vcs/commits/$hash").mkdir()
            trackedFiles.forEach { File(it).copyTo(File("vcs/commits/$hash/$it"), overwrite = true) }
            File("vcs/log.txt").appendText(hash + System.lineSeparator())
            File("vcs/log.txt").appendText(username + System.lineSeparator())
            File("vcs/log.txt").appendText(message + System.lineSeparator())
            println("Changes are committed.")
        } else {
            println("Nothing to commit.")
        }
    }
}

fun logFun(args: Array<String>) {
    val commits = File("vcs/log.txt").readLines().chunked(3).reversed()

    if (commits.isEmpty()) {
        println("No commits yet.")
    } else {
        commits.map { (hash, author, message) ->
            buildString {
                appendLine("commit $hash")
                appendLine("Author: $author")
                appendLine(message)
            }
        }.joinToString(System.lineSeparator()).also(::println)
    }
}

fun File.hash() = if (this.exists()) {
    this.readText().hash()
} else ""

fun String.hash() = MessageDigest
    .getInstance("SHA-1")
    .digest(this.toByteArray())
    .joinToString("") { "%02x".format(it) }

fun getCommitDir(commitId: String): File {
    val commitsDir = File("vcs/commits")
    return commitsDir.resolve(commitId)
}

fun getCommitSet(): Set<String> {
    val commitsDit = File("vcs/commits")
    return commitsDit.listFiles()!!.map { it.name }.toSet()
}

fun checkout(commitId: String?) {
    when (commitId) {
        null -> println("Commit id was not passed.")
        in getCommitSet() -> {
            val sourcePath = getCommitDir(commitId)
            val targetPath = File(System.getProperty("user.dir"))

            sourcePath.toPath().listDirectoryEntries().forEach {
                val newPath = targetPath.toPath().resolve(sourcePath.toPath().relativize(it))
                Files.copy(it, newPath, StandardCopyOption.REPLACE_EXISTING)
            }

            println("Switched to commit $commitId.")
        }
        else -> println("Commit does not exist.")
    }
}


fun main(args: Array<String>) {
    createVcsDir()
    val options = args.elementAtOrNull(1)

    val help = """These are SVCS commands:
config     Get and set a username.
add        Add a file to the index.
log        Show commit logs.
commit     Save changes.
checkout   Restore a file."""

    when (args.firstOrNull()) {
        "--help" -> print(help)
        null -> print(help)
        "config" -> configFun(args)
        "add" -> addFun(args)
        "log" -> logFun(args)
        "commit" -> commitFun(args)
        "checkout" -> checkout(options)
        else -> print("'${args.first()}' is not a SVCS command.")
    }
}