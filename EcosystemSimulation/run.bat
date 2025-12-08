@echo off
REM run.bat - compile and run the JavaFX application
REM Place JavaFX SDK lib folder path in environment variable JAVAFX (e.g. C:\javafx-sdk-20\lib)

setlocal
set SRC=src
set OUT=bin






































pauseendlocal:endif exist sources.txt del sources.txt:cleanup)  java -cp "%OUT%" ecosystem.ui.JavaFXApp %*  echo Then re-open the terminal and run this script again.  echo    setx JAVAFX "C:\\javafx-sdk-20\\lib"  echo If you have a JavaFX SDK, set the JAVAFX environment variable to the SDK's "lib" folder, for example:  echo Running without JavaFX module options. This may fail if JavaFX is not on the classpath.) else (  java --module-path "%JAVAFX%" --add-modules javafx.controls,javafx.fxml -cp "%OUT%" ecosystem.ui.JavaFXApp %*if defined JAVAFX (
nrem Run the application (default entry: ecosystem.ui.JavaFXApp)echo Compilation succeeded.)  goto :cleanup  pause  echo Compilation failed.if errorlevel 1 ()  javac -d "%OUT%" @sources.txt  echo WARNING: JAVAFX environment variable not set. Attempting compilation without module flags.) else (  javac --module-path "%JAVAFX%" --add-modules javafx.controls,javafx.fxml -d "%OUT%" @sources.txt  echo Using JavaFX libs from %JAVAFX%
  if defined JAVAFX (echo Compiling Java sources to %OUT% ...)  goto :end  echo No Java sources found under %SRC%\if not exist sources.txt (dir /b /s "%SRC%\*.java" > sources.txtif not exist "%OUT%" mkdir "%OUT%"
necho Discovering Java source files...n