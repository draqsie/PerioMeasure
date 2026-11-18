@echo off
rem ===================================================================
rem  PerioMeasure - masaustu kisayolu olusturur / creates a desktop shortcut.
rem  Cift tiklayin. Masaustune PerioMeasure simgeli bir kisayol koyar;
rem  kisayol bu klasordeki PerioMeasure.bat'i calistirir.
rem  Proje klasorunu tasirsaniz bu dosyayi yeniden calistirin.
rem ===================================================================
setlocal
set "FIJI=%USERPROFILE%\Fiji.app"
set "PMJAR=%FIJI%\plugins\PerioMeasure_.jar"
if not exist "%PMJAR%" (
  echo PerioMeasure bulunamadi / not found: "%PMJAR%"
  echo Once eklentiyi Fiji'ye kurun / install the plugin first ^(README^).
  pause
  exit /b 1
)
set "JAVA="
for /d %%D in ("%FIJI%\java\win64\*") do if exist "%%D\jre\bin\java.exe" set "JAVA=%%D\jre\bin\java.exe"
if not defined JAVA for /d %%D in ("%FIJI%\java\win64\*") do if exist "%%D\bin\java.exe" set "JAVA=%%D\bin\java.exe"
if not defined JAVA (
  echo Fiji'nin Java'si bulunamadi / Fiji's Java not found: "%FIJI%\java\win64"
  pause
  exit /b 1
)
set "PM_ICON=%USERPROFILE%\PerioMeasure\PerioMeasure.ico"
set "PM_BAT=%~dp0PerioMeasure.bat"
set "PM_DIR=%~dp0"
if not exist "%PM_BAT%" (
  echo PerioMeasure.bat bulunamadi / not found: "%PM_BAT%"
  pause
  exit /b 1
)
"%JAVA%" -cp "%PMJAR%" com.periomeasure.ui.AppIcon "%PM_ICON%"
if errorlevel 1 (
  echo Simge olusturulamadi / could not create the icon.
  pause
  exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "$d=[Environment]::GetFolderPath('Desktop'); $s=(New-Object -ComObject WScript.Shell).CreateShortcut((Join-Path $d 'PerioMeasure.lnk')); $s.TargetPath=$env:PM_BAT; $s.WorkingDirectory=$env:PM_DIR; $s.IconLocation=$env:PM_ICON+',0'; $s.WindowStyle=7; $s.Description='PerioMeasure - periocular measurements'; $s.Save(); Write-Host ('Kisayol olusturuldu / shortcut created: ' + (Join-Path $d 'PerioMeasure.lnk'))"
if errorlevel 1 (
  echo Kisayol olusturulamadi / could not create the shortcut.
  pause
  exit /b 1
)
pause
