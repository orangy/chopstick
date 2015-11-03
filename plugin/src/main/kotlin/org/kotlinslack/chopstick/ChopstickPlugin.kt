package org.kotlinslack.chopstick

import com.squareup.okhttp.*
import groovy.lang.*
import org.gradle.api.*
import org.gradle.util.*
import java.io.*
import java.lang.reflect.Array
import java.net.*

class ChopstickPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("chopsticks", ChopsticksExtension::class.java, project)
        project.afterEvaluate {
            //project.tasks.getByName("compileKotlin").dependsOn.add()

        }
    }
}

open class ChopsticksExtension(val project: Project) : Configurable<ChopsticksExtension> {
    override fun configure(cl: Closure<Any>): ChopsticksExtension {
        val chopsticksAction = ChopsticksSection(project)
        ConfigureUtil.configure(cl, chopsticksAction).execute()
        return this
    }

}

class ChopsticksSection(val project: Project, val folder: String = "${project.buildDir.path}/generated/source/chopstick") {
    val sources = arrayListOf<URL>()
    val folders = arrayListOf<ChopsticksSection>()

    fun folder(path: String, configure: Closure<*>) {
        val folder = ChopsticksSection(project, path)
        folders.add(folder)
        project.configure(folder, configure)
    }

    fun github(src: String) {
        val parts = src.split(':')
        if (parts.size < 2)
            throw IllegalArgumentException("GitHub identifier should include repo, path and an optional hash")
        if (parts.size == 3)
            url("https://raw.githubusercontent.com/${parts[0]}/${parts[2]}}/${parts[1]}")
        else
            url("https://raw.githubusercontent.com/${parts[0]}/master/${parts[1]}")
    }

    fun url(src: Any?) {
        val source: Any? = if (src is Closure<*>) src.call() else src
        when {
            source is CharSequence -> sources.add(URL(source.toString()))
            source is URL -> sources.add(source)
            source is Collection<*> -> for (sco in source) url(sco)
            source != null && source.javaClass.isArray -> {
                val len = Array.getLength(source)
                for (i in 0..len - 1) {
                    url(Array.get(source, i))
                }
            }
            else -> throw IllegalArgumentException("URL must either be a URL, a CharSequence, a Collection or an array.")
        }
    }

    fun execute() {
        val client = OkHttpClient()
        getAllSources().forEach { item ->
            val (url, folder) = item
            println("Downloading: '$url' to '$folder'")
            val fullURL = url.toExternalForm()
            val path = File(url.path)
            val downloadTo = File(folder)
            downloadTo.mkdirs()
            val file = downloadTo.resolve(path.name)
            client.execute(fullURL) {
                success { response ->
                    response.body().byteStream().copyTo(file.outputStream())
                }
                fail { request, ioException ->
                    println("Failed: $ioException")
                }
            }
        }
    }

    data class TransferItem(val url: URL, val folder: String)

    private fun getAllSources(): List<TransferItem> {
        return sources.map { TransferItem(it, folder) } + folders.flatMap { it.getAllSources() }
    }
}
