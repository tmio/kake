package io.tmio.buildk

import io.tmio.kake.File
import io.tmio.kake.Kake
import java.net.URI
import io.tmio.kake.Kake.Companion.file
import java.net.URL

class BuildK {
    companion object {

        fun write(fileName: String, contents: String = "", callable: (()->String)? = null): String {
            var fileContents = if (callable != null) callable() else contents
            val file = java.io.File(fileName).absoluteFile
            file.parentFile.mkdirs()
            file.writeText(fileContents)
            return contents
        }

        fun read(fileName: String, block: (contents: String)-> Any? = { it }) : Any? {
            return block(java.io.File(fileName).readText())
        }

        fun download(fileName: String? = null, url: String): DownloadFileTask {
            return Kake.task(DownloadFileTask(if (fileName == null) (url.drop(url.lastIndexOf('/'))) else fileName, url))
        }

        fun define(artifact: String, body: ()->Unit) {
            body()
        }
    }
}

class DownloadFileTask(name : String, val url: String) : File(name) {

    val source = URL(url)
    init {
    }
}