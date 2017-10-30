package io.tmio.buildk

import com.winterbe.expekt.ExpectAny
import com.winterbe.expekt.should
import jnr.posix.POSIXFactory
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import io.tmio.buildk.BuildK.Companion.write
import io.tmio.buildk.BuildK.Companion.read
import io.tmio.buildk.BuildK.Companion.define
import io.tmio.buildk.BuildK.Companion.download
import io.tmio.kake.File
import org.apache.http.client.HttpClient
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import io.tmio.KakeSpec.Com
import java.net.URI

@RunWith(JUnitPlatform::class)
class BuildKSpec : Spek({

    fun ExpectAny<() -> Unit>.receive(message: String, callable : ()->Any?): ExpectAny<() -> Unit> {
        this.satisfy({ subject ->
            try {
                subject.apply {  }
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
        System.setProperty("user.dir", java.io.File("target/specs").absolutePath)
        val posix = POSIXFactory.getPOSIX()
        posix.chdir("target/specs")
    }

    describe("common methods") {

        beforeEachTest {
            java.io.File(".").listFiles().forEach { it.delete() }
        }

        describe(".write") {
            it("should create path") {
                write("foo/test")
                java.io.File("foo").isDirectory.should.be.`true`
                java.io.File("foo/test").exists().should.be.`true`
            }

            it("should write contents to file") {
                write("foo.txt", "bar")

                java.io.File("foo.txt").readText().should.equal("bar")
            }


            it("should retrieve content from block, if block given") {
                write("test", callable = { "block" })
                java.io.File("test").readText().should.equal("block")
            }

            it("should write empty file if no content provided") {
                write("test")
                java.io.File("test").readText().should.equal("")
            }

            it("should return content as a string") {
                write("test", "content").should.equal("content")
            }

            it("should return empty string if no content provided") {
                write("test").should.equal("")
            }
        }

        describe(".read") {

            val fileName = "test"
            val content = "content"
            beforeEachTest {
                 write(fileName, content)
            }

            it("should return contents of named file") {
                read(fileName).should.equal(content)
            }

            it("should yield to block if block given") {
                read(fileName, {
                    it.should.equal(content)
                })
            }

            it("should return block response if block given") {
                read(fileName, { 5 }).should.equal(5)
            }
        }

        describe(".download") {
            val content = "we has download!"
            val http = Mockito.mock(HttpClient::class.java)


            fun tasks(): List<File> {
                return listOf(download(url = "http://localhost/download"), download("downloaded", "http://localhost/download"))
            }

//            it("should accept a String and download from that URL") {
//            define "foo" {
//            download("http://localhost/download").tap { |task|
//            task.source.should_receive(:read).and_yield [@content]
//            task.invoke
//            task.should contain(@content)
//            }
//            }
//            }

//            it("should accept a URI and download from that URL") {
//            define("foo") {
//            download(URI.parse("http://localhost/download")).tap { |task|
//            task.source.should_receive(:read).and_yield [@content]
//            task.invoke
//            task.should contain(@content)
//            }
//            }
//            }

            it("should accept a path and String and download from that URL") {
            define("foo") {
            download("downloaded", "http://localhost/download").apply {
                this.source.should.receive("read", { ByteArrayInputStream(content.toByteArray())})
                this.execute()
                this.should.contain(content)
            }
            }
            }

            it "should accept an artifact and String and download from that URL" {
            define "foo" {
            artifact("com.example:library:jar:2.0").tap { |artifact|
            download(artifact=>"http://localhost/download").source.should_receive(:read).and_yield [@content]
            artifact.invoke
            artifact.should contain(@content)
            }
            }
            }

            it "should accept a path and URI and download from that URL" {
            define "foo" {
            download("downloaded"=>URI.parse("http://localhost/download")).tap { |task|
            task.source.should_receive(:read).and_yield [@content]
            task.invoke
            task.should contain(@content)
            }
            }
            }

            it "should create path for download" {
            define "foo" {
            download("path/downloaded"=>URI.parse("http://localhost/download")).tap { |task|
            task.source.should_receive(:read).and_yield [@content]
            task.invoke
            task.should contain(@content)
            }
            }
            }

            it "should fail if resource not found" {
            tasks.each { |task|
            task.source.should_receive(:read).and_raise URI::NotFoundError
            lambda { task.invoke }.should raise_error(URI::NotFoundError)
            }
            tasks.last.should_not exist
                    }

            it "should fail on any other error" {
            tasks.each { |task|
            task.source.should_receive(:read).and_raise RuntimeError
                    lambda { task.invoke }.should raise_error(RuntimeError)
            }
            tasks.last.should_not exist
                    }

            it "should execute only if file {es not already exist" {
            define "foo" {
            download("downloaded"=>"http://localhost/download").tap { |task|
            task.source.should_not_receive(:read)
            write task.to_s, "not really"
            task.invoke
            }
            }
            }

            it "should execute without a proxy if none specified" {
            Net::HTTP.should_receive(:new).with("localhost", 80).twice.and_return(@http)
            tasks.each(&:invoke)
            }

            it "should pass Buildr proxy options" {
            Buildr.options.proxy.http = "http://proxy:8080"
            Net::HTTP.should_receive(:new).with("localhost", 80, "proxy", 8080, nil, nil).twice.and_return(@http)
            tasks.each(&:invoke)
            }

            it "should set HTTP proxy from HTTP_PROXY environment variable" {
            ENV["HTTP_PROXY"] = "http://proxy:8080"
            Net::HTTP.should_receive(:new).with("localhost", 80, "proxy", 8080, nil, nil).twice.and_return(@http)
            tasks.each(&:invoke)
            }
        }
    }

})