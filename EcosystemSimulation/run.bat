@echo off
REM run.bat - compile and run the JavaFX application
REM Place JavaFX SDK lib folder path in environment variable JAVAFX (e.g. C:\javafx-sdk-20\lib)

setlocal
set SRC=src
set OUT=bin

if not exist "%OUT%" mkdir "%OUT%"
echo Discovering Java source files...
dir /b /s "%SRC%\*.java" > sources.txt
if not exist sources.txt (
  echo No Java sources found under %SRC%
  goto :end
)

echo Compiling...
if defined JAVAFX (
  javac --module-path "%JAVAFX%" --add-modules javafx.controls,javafx.fxml -d "%OUT%" @sources.txt
) else (
  javac -d "%OUT%" @sources.txt
)

if errorlevel 1 (
  echo Compilation failed.
  pause
  goto :end
)

echo Running...
if defined JAVAFX (
  java --module-path "%JAVAFX%" --add-modules javafx.controls,javafx.fxml -cp "%OUT%" ecosystem.ui.JavaFXApp %*
) else (
  java -cp "%OUT%" ecosystem.ui.JavaFXApp %*
)

:end
endlocal
