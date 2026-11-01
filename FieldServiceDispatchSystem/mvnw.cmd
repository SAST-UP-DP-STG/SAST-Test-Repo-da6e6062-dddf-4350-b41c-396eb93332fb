@echo off
setlocal
set MVNW_HOME=%~dp0
set WRAPPER_JAR=%MVNW_HOME%.mvn\wrapper\maven-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$p='%WRAPPER_JAR%';$u='https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar';New-Item -ItemType Directory -Force -Path (Split-Path $p) | Out-Null;Invoke-WebRequest -UseBasicParsing -Uri $u -OutFile $p"
)

if "%JAVA_HOME%"=="" (
  set JAVA_EXE=java
) else (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" -Dmaven.multiModuleProjectDirectory="%MVNW_HOME%." org.apache.maven.wrapper.MavenWrapperMain %*
endlocal
