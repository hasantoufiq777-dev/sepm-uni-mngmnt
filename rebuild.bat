@echo off
REM Rebuilds the app after Java code changes and restarts it.
REM Usage: double-click this file, or run "rebuild.bat" in this folder.
REM Needs Docker Desktop running. Maven runs inside Docker, so it does not need to be installed.

cd /d "%~dp0"

echo [1/2] Building the jar with Maven (inside Docker)...
docker run --rm -v "%cd%:/app" -v ums-m2:/root/.m2 -w /app maven:3.9.9-eclipse-temurin-21 mvn -q clean package -DskipTests
if errorlevel 1 (
    echo.
    echo Build FAILED. Check the errors above.
    pause
    exit /b 1
)

echo [2/2] Rebuilding and restarting the containers...
docker compose up -d --build
if errorlevel 1 (
    echo.
    echo Docker start FAILED. Is Docker Desktop running?
    pause
    exit /b 1
)

echo.
echo Done. Open http://localhost:8081 (press Ctrl+F5 to refresh).
pause
