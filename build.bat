@echo off
setlocal EnableDelayedExpansion

rem ==== 1. Đường dẫn cơ bản ====
set "BASE_DIR=%~dp0"

rem Thư mục JavaFX SDK (native .dll + .jar)
set "JFX_HOME=%BASE_DIR%javafx-sdk"
rem (không bắt buộc cho build, nhưng để đây cũng không sao)
set "PATH=%JFX_HOME%\bin;%PATH%"

rem module-path trỏ vào lib của JavaFX SDK
set "PATH_TO_FX=%JFX_HOME%\lib"

rem Thư mục output .class
set "CLASS_DIR=%BASE_DIR%out"

rem ==== 2. Chuẩn bị JDK ====
set "JAVAC_EXE=javac"
if defined JAVA_HOME (
  set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
)

rem ==== 3. Xoá output cũ & tạo thư mục out mới ====
if exist "%CLASS_DIR%" (
  echo Cleaning old output folder...
  rmdir /S /Q "%CLASS_DIR%"
)
mkdir "%CLASS_DIR%"

rem ==== 4. Gom tất cả file .java trong sourcecode ====
echo Collecting .java files...
set "SRC_FILES="
for /R "%BASE_DIR%EcosystemSimulation\src\ecosystem" %%f in (*.java) do (
  set "SRC_FILES=!SRC_FILES! "%%f""
)

rem ==== 5. Compile toàn bộ source ====
echo.
echo Compiling sources...
"%JAVAC_EXE%" ^
  --module-path "%PATH_TO_FX%" ^
  --add-modules javafx.controls,javafx.fxml ^
  -d "%CLASS_DIR%" ^
  %SRC_FILES%

if errorlevel 1 (
  echo.
  echo [ERROR] Build failed. Check the error messages above.
  pause
  exit /b 1
)

echo.
echo Build SUCCESSFUL. Classes are in: "%CLASS_DIR%"
pause
endlocal