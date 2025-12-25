@echo off
setlocal

rem ==== Đường dẫn cơ bản ====
set "BASE_DIR=%~dp0"

rem JavaFX SDK
set "JFX_HOME=%BASE_DIR%javafx-sdk"
set "PATH_TO_FX=%JFX_HOME%\lib"
rem thêm đường dẫn .dll của JavaFX
set "PATH=%JFX_HOME%\bin;%PATH%"

rem Folder .class và resources
set "CLASS_DIR=%BASE_DIR%out"
set "RUN_CP=%CLASS_DIR%;%BASE_DIR%EcosystemSimulation\src\ecosystem"

rem Main class (updated to current JavaFX entry)
set "MAIN_CLASS=ecosystem.ui.JavaFXApp"

rem ==== Chọn java hoặc javaw (ẩn console) ====
set "JAVA_EXE=javaw"
if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\javaw.exe"
)

"%JAVA_EXE%" ^
  --module-path "%PATH_TO_FX%" ^
  --add-modules javafx.controls,javafx.fxml ^
  -cp "%RUN_CP%" ^
  %MAIN_CLASS%

endlocal