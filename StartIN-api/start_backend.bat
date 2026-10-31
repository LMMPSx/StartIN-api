@echo off
title StartIn Backend API (Java 21 + Spring Boot)
echo ========================================================
echo     Iniciando Backend Spring Boot (Java 21) - StartIn
echo ========================================================
cd /d %~dp0

set MAVEN_CMD=mvn
if exist "C:\Users\vinic\.maven\apache-maven-3.9.9\bin\mvn.cmd" (
    set MAVEN_CMD="C:\Users\vinic\.maven\apache-maven-3.9.9\bin\mvn.cmd"
)

if exist "target\startin-api-1.0.0.jar" (
    echo Iniciando JAR compilado...
    java -jar target\startin-api-1.0.0.jar
) else (
    echo Compilando e iniciando via Maven...
    %MAVEN_CMD% spring-boot:run
)

pause
