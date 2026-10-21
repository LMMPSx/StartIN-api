@echo off
title Build StartIn Backend (Java 21)
cd /d %~dp0

set MAVEN_CMD=mvn
if exist "C:\Users\vinic\.maven\apache-maven-3.9.9\bin\mvn.cmd" (
    set MAVEN_CMD="C:\Users\vinic\.maven\apache-maven-3.9.9\bin\mvn.cmd"
)

%MAVEN_CMD% clean package -DskipTests=true
pause
