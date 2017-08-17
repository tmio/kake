package io.tmio.kake

import io.tmio.kake.Kake.Companion.task
import io.tmio.kake.Kake.Companion.file
import io.tmio.kake.Kake.Companion.main


fun main(args: Array<String>) {
    task("compile", {println("Compiling")})
    task("test", {println("Testing")})
    task("test").enhance(task("compile"))
    main(args)
}


