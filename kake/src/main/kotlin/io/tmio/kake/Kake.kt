package io.tmio.kake

import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import java.nio.charset.Charset

class Kake {
    companion object {

        var kakefileNames = mutableListOf("Kakefile")

        var allTasks = mutableSetOf<Task>()

        @JvmStatic fun main(args: Array<String>) {
            val kakefile = find_kakefile()
            eval(kakefile)
            run(*args)
        }

        fun find_kakefile(): java.io.File {
            for (kakefileName in kakefileNames) {
                val folder = java.io.File(".").walkBottomUp().asIterable().firstOrNull { java.io.File(it, kakefileName).exists() }
                if (folder != null) {
                    return java.io.File(folder, kakefileName)
                }
            }
            throw RuntimeException("Could not find Kakefile")
        }

        fun eval(file: java.io.File) {
            eval(file.readText(Charset.forName("UTF-8")))
        }

        fun eval(text: String) {
            val engine = KotlinJsr223JvmLocalScriptEngineFactory().scriptEngine
            engine.eval("import io.tmio.kake.Kake.Companion.task\n" +
                    "import io.tmio.kake.Kake.Companion.file\n" + text)
        }

        fun tasks(): List<Task> = allTasks.filter { it.enhancers.isEmpty() }

        fun task(name: String, vararg lambdas: ()->Unit): Task {
            return task(Task(name), *lambdas)
        }

        fun file(name: String, vararg lambdas: ()->Unit): File {
            return task(File(name), *lambdas)
        }

        fun <T: Task> task(task : T, vararg lambdas: ()->Unit): T {
            var newTask : T = task
            if (!allTasks.add(newTask)) {
                newTask = allTasks.first { it.equals(newTask) } as T
            }
            newTask.enhance(*lambdas)
            return newTask
        }

        fun run(vararg taskNames: String) {
            for (taskName in taskNames) {
                val task = tasks().firstOrNull() { it.name.equals(taskName) } ?:  throw RuntimeException("Don't know how to build task '" + taskName + "'")
                task.execute()
            }

        }
    }
}

open class Task(val name: String) {

    val lambdas = mutableListOf<()->Unit>()

    val enhancers = mutableListOf<Task>()

    val dependencies = mutableListOf<Task>()

    var executed = false


    fun enhance(vararg others: Task): Task {
        for (other in others) {
            this.dependencies.add(other)
            other.enhancers.add(this)
        }
        return this
    }

    fun enhance(vararg lambdas: ()->Unit): Task {
        for (lambda in lambdas) {
            this.lambdas.add(lambda)
        }
        return this
    }

    fun execute() {
        if (executed) {
            return
        }
        for (dep in dependencies) {
            dep.execute()
        }
        for (lambda in lambdas) {
            lambda.invoke()
        }
        executed = true
    }

    open fun needed(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Task) other.name.equals(name) else false
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

open class File(name : String) : Task(name) {

    override fun needed() : Boolean {
        return !java.io.File(name).exists()
    }
}