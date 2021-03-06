package io.tmio.kake

import com.winterbe.expekt.ExpectAny
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import jnr.posix.POSIXFactory
import org.jetbrains.spek.api.dsl.on
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

import io.tmio.kake.Kake.Companion.file
import io.tmio.kake.Kake.Companion.task

object Debug {
    var str: StringBuilder = StringBuilder()
}

@RunWith(JUnitPlatform::class)
class KakeSpec : Spek({

    fun ExpectAny<() -> Unit>.raise(message: String): ExpectAny<() -> Unit> {
        this.satisfy({ subject ->
            try {
                subject.invoke()
                false
            } catch(e: Exception) {
                e.message?.contains(message)!!
            }
        })
        return this
    }

    beforeGroup() {
        java.io.File("target/specs").deleteRecursively()
        java.io.File("target/specs").mkdir()
        val posix = POSIXFactory.getPOSIX()
        posix.chdir("target/specs")
    }

    beforeEachTest {
        Kake.allTasks = mutableSetOf<Task>()
    }

    describe("allTasks") {
        it("should define an array of all tasks, empty by default") {
            Kake.allTasks.should.equal(emptySet())
        }
    }

    describe("task") {
        it("should return a task object") {
            var task = task("something")
            task.should.not.be.`null`
        }

        it("should add the task to the list of global tasks") {
            var task = task("something")
            Kake.allTasks.should.equal(setOf(task))
        }

        it("should not add the same task twice") {
            var task = task("something")
            var task2 = task("something")
            Kake.allTasks.should.equal(setOf(task))
            (task == task2).should.be.`true`
        }

        describe(".enhance") {
            it("should return self") {
                task("something").enhance(task("somethingelse")).should.equal(task("something"))
            }

            it("should allow adding lambas to its definition") {
                var task = task("something")
                task.enhance({ print("Hello") }, { print("World") })
                task.lambdas.size.should.equal(2)
            }

            it("should allow adding tasks to its definition") {
                val foo = task("foo")
                var bar = task("bar")
                val foobar = task("foobar")
                foobar.enhance(foo, bar)
                foobar.dependencies.size.should.equal(2)
            }
        }

        describe(".execute") {
            it("should run all the lambdas") {
                var counter: Int = 0
                val task = task("something")
                task.enhance({ counter += 1 }, { counter += 1 })
                task.execute()
                counter.should.equal(2)
            }

            it("should run lambdas in the order they were added to the task") {
                val str = StringBuilder()
                var task = task("something")
                task.enhance({ str.append("Hello ") }, { str.append("World") })
                task.execute()
                str.toString().should.equal("Hello World")
            }

            it("should execute the dependencies first") {
                val str = StringBuilder()
                val task = task("something")
                task.enhance({ str.append("World") })
                val dep = task("before")
                dep.enhance({ str.append("Hello ") })
                task.enhance(dep)
                task.execute()
                str.toString().should.equal("Hello World")
            }

            it("should run dependencies one time only") {
                val str = StringBuilder()
                val root = task("root", { str.append("Hello ") })
                val task = task("something", { str.append("World") })
                val dep = task("before", { str.append("Kake ") })
                val dep2 = task("before", { str.append("Kake ") })
                dep.enhance(root)
                dep2.enhance(root)
                task.enhance(dep, dep2)
                task.execute()
                str.toString().should.equal("Hello Kake Kake World")
            }
        }

        describe(".needed") {
            it("should return true by default") {
                task("something").needed().should.equal(true)
            }
        }
    }

    describe(".file") {
        it("should define a task, associated with a file") {
            val file = file("foobar.txt")
            expect(file is File).to.equal(true)
        }

        it("should be needed if the file doesn't exist") {
            val file = file("foobar.txt")
            file.needed().should.equal(true)
            java.io.File("foobar.txt").writeText("hello")
            file.needed().should.equal(false)
        }
    }

    describe(".tasks") {
        it("should return empty if no tasks have been defined") {
            Kake.tasks().should.equal(emptyList())
        }

        it("should return tasks with no dependencies") {
            task("something")
            Kake.tasks().size.should.equal(1)
        }

        it("should not return tasks with dependencies") {
            val task = task("foo")
            val task2 = task("bar")
            task.enhance(task2)
            Kake.tasks().size.should.equal(1)
            Kake.tasks().first().should.equal(task)
        }
    }

    describe(".main") {
        describe("A Kakefile is absent") {
            it("should raise an exception") {
                { Kake.main(arrayOf("foo")) }.should.raise("Could not find Kakefile")
            }
        }


        describe("A Kakefile is present") {

            fun Kake.append(arg: String) {
                Debug.str.append(arg)
            }

            beforeEachTest {
                Debug.str = StringBuilder()
                java.io.File("Kakefile").writeText(
                        "import io.tmio.kake.Debug\n" +
                                "task(\"foo\", { Debug.str.append(\"foo\") })\n" +
                                "task(\"bar\", { Debug.str.append(\"bar\") })\n")
            }

            it("should invoke one or more tasks") {
                Kake.main(arrayOf("foo", "bar"))
                Debug.str.toString().should.equal("foobar")
            }

            it("should raise an exception if no task by that name exists") {
                { Kake.main(arrayOf("foo", "else")) }.should.raise("Don't know how to build task")
                Debug.str.toString().should.equal("foo")
            }
        }

        describe("We use a different Kakefile file name") {
            it("should allow to use a different file name") {
                java.io.File("InterestingFile").writeText(
                        "task(\"foo\")")
                Kake.kakefileNames.add("InterestingFile")
                Kake.main(arrayOf("foo"))
            }
        }
    }

    describe(".run") {


        var str: StringBuilder = StringBuilder()
        beforeEachTest {
            java.io.File("Kakefile").createNewFile()

            str = StringBuilder()
            task("foo", { str.append("foo") })
            task("bar", { str.append("bar") })
        }

        it("should invoke one or more tasks") {
            Kake.run("foo", "bar")
            str.toString().should.equal("foobar")
        }

        it("should raise an exception if no task by that name exists") {
            { Kake.run("foo", "else") }.should.raise("Don't know how to build task")
            str.toString().should.equal("foo")
        }

    }
})
