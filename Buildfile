require 'lock_jar/buildr'
require 'buildr/kotlin'

repositories.remote << "http://repo.maven.apache.org/maven2/"

lock_jar do
  group 'default' do
    jar 'org.jetbrains.kotlin:kotlin-runtime:jar:1.1.3-2'
    jar 'org.jetbrains.kotlin:kotlin-stdlib:jar:1.1.3-2'
    jar 'com.github.jnr:jnr-posix:jar:3.0.41'
    jar 'org.jetbrains.kotlin:kotlin-script-util:jar:1.1.3-2'
    jar 'org.jetbrains.kotlin:kotlin-reflect:jar:1.1.3-2'
    jar 'org.apache.httpcomponents:httpclient:jar:4.5.3'
  end
   
  group 'test' do
    jar 'org.jetbrains.spek:spek-api:jar:1.1.2'
    jar 'org.jetbrains.spek:spek-junit-platform-engine:jar:1.1.2'
    jar 'org.junit.platform:junit-platform-runner:jar:1.0.0-M4'
    jar 'com.winterbe:expekt:jar:0.5.0'
    jar 'org.mockito:mockito-all:jar:1.10.19'
  end
end

define "buildk" do
  define "kake", :group => "io.tmio", :version => "1.0.0" do

    compile.using(:kotlinc).with(lock_jars(["default"]))
    test.using(:junit).with(lock_jars(["test"]))
    package(:jar)
  end

  define "buildk", :group => "io.tmio", :version => "1.0.0" do
    compile.using(:kotlinc).with(project("buildk:kake"))
    #test.using(:junit).with(lock_jars(["test"]))
    package(:jar)
  end
end