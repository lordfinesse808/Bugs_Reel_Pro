@echo off
echo === Environment Verification ===

:: Git check
echo Git Version:
git --version || echo Git not found
echo Git Path:
where git || echo Git path not found

:: JDK check
echo Java Version:
java -version 2>&1 | findstr "version" || echo Java not found
echo Java Path:
where java || echo Java path not found

:: Android Studio check (Quail 1)
echo Android Studio Check:
if exist "C:\Program Files\Android\Android Studio\bin\studio64.exe" (
  echo Android Studio Found at C:\Program Files\Android\Android Studio\bin\studio64.exe
  "C:\Program Files\Android\Android Studio\bin\studio64.exe" --version
) else (
  echo Android Studio not found (check installation directory)
)

:: Claude Code / VS Code check
echo Claude Code / VS Code Path:
where code || echo Claude Code not found

:: Repo check
echo Git Repo Status:
git status || echo No repo detected in current directory

echo === Verification Complete ===
pause
