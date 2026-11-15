@echo off
REM Script to run the 6 Unit Tests and 3 Integration Tests in Windows Command Prompt (CMD)
cd /d "%~dp0"

echo ========================================================
echo Running University Management System Tests (6 Unit + 3 Integration)
echo ========================================================
echo.

set "MAVEN_CMD=mvn"
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    if exist "C:\Users\User\.m2\wrapper\dists\apache-maven-3.9.9-bin\4nf9hui3q3djbarqar9g711ggc\apache-maven-3.9.9\bin\mvn.cmd" (
        set "MAVEN_CMD=C:\Users\User\.m2\wrapper\dists\apache-maven-3.9.9-bin\4nf9hui3q3djbarqar9g711ggc\apache-maven-3.9.9\bin\mvn.cmd"
    ) else if exist "E:\Downloads\IntelliJ IDEA Community Edition 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" (
        set "MAVEN_CMD=E:\Downloads\IntelliJ IDEA Community Edition 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd"
    )
)

call "%MAVEN_CMD%" test %*
set TEST_EXIT_CODE=%errorlevel%

if %TEST_EXIT_CODE% equ 0 (
    echo.
    echo ========================================================
    echo SUCCESS: All 9 tests passed successfully!
    echo ========================================================
) else (
    echo.
    echo ========================================================
    echo FAILED: One or more tests failed (Exit Code: %TEST_EXIT_CODE%).
    echo ========================================================
)

echo.
exit /b %TEST_EXIT_CODE%
