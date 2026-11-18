@echo off
rem ===================================================================
rem  PerioMeasure - cift tiklayin.
rem  Fiji'nin kendi Java'si, ImageJ'si ve FlatLaf arayuz kutuphanesi ile
rem  tek PerioMeasure penceresini acar (Fiji'nin kendisi acilmaz).
rem ===================================================================
setlocal
set "FIJI=%USERPROFILE%\Fiji.app"
if not exist "%FIJI%\plugins\PerioMeasure_.jar" (
  echo PerioMeasure bulunamadi: "%FIJI%\plugins\PerioMeasure_.jar"
  echo Once eklentiyi Fiji'ye kurun ^(README, bolum 1^).
  pause
  exit /b 1
)
set "JAVA="
for /d %%D in ("%FIJI%\java\win64\*") do if exist "%%D\jre\bin\javaw.exe" set "JAVA=%%D\jre\bin\javaw.exe"
if not defined JAVA for /d %%D in ("%FIJI%\java\win64\*") do if exist "%%D\bin\javaw.exe" set "JAVA=%%D\bin\javaw.exe"
set "IJJAR="
for %%J in ("%FIJI%\jars\ij-*.jar") do set "IJJAR=%%J"
set "FLATLAF="
for %%J in ("%FIJI%\jars\flatlaf-*.jar") do set "FLATLAF=%%J"
if not defined JAVA (
  echo Fiji'nin Java'si bulunamadi: "%FIJI%\java\win64"
  pause
  exit /b 1
)
if not defined IJJAR (
  echo ImageJ bulunamadi: "%FIJI%\jars\ij-*.jar"
  pause
  exit /b 1
)
if not defined FLATLAF (
  echo FlatLaf bulunamadi: "%FIJI%\jars\flatlaf-*.jar"
  pause
  exit /b 1
)
start "PerioMeasure" "%JAVA%" -Xmx3g -cp "%IJJAR%;%FLATLAF%;%FIJI%\plugins\PerioMeasure_.jar" com.periomeasure.ui.PerioMeasureApp