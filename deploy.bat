@echo off

if "%1%"=="" (
	GOTO GET_INPUT_VERSION
) else (
	GOTO PARAMETER_DEFINITION
)

:GET_INPUT_VERSION
set version=
set /p version=Version to Release: 
GOTO SET_VERSION

:PARAMETER_DEFINITION
set version=%1%
GOTO SET_VERSION

:SET_VERSION
if "%version%"=="" (
	GOTO SET_VERSION_IGNORE
) else (
	GOTO SET_VERSION_PERFORM
)

:SET_VERSION_PERFORM
echo Setting Version to Relase: %version%
call mvn versions:set -DnewVersion=%version% -DgenerateBackupPoms=false
if "%errorlevel%"=="1" (GOTO END)
GOTO PERFORM_RELASE

:SET_VERSION_IGNORE
echo Ignoring Set Version ...
GOTO PERFORM_RELASE

:PERFORM_RELASE
echo Performing Relase...
call mvn clean deploy -DperformRelease=true
if "%errorlevel%"=="1" (GOTO END_ERROR)
GOTO COMMIT

:COMMIT
if "%version%"=="" (
	GOTO COMMIT_IGNORE
) else (
	GOTO COMMIT_PERFORM
)

:COMMIT_PERFORM
echo Commiting changed pom(s), and other non commited files...
call git commit -m "Incremented version to: %version%. Release to repository."
if "%errorlevel%"=="1" (
	echo Possibly git is not installed as a valid command line application...
	GOTO END_ERROR
)
GOTO END

:COMMIT_IGNORE
echo Ignoring commit since no changes were made to the pom(s)...
GOTO END

:END_ERROR
echo Error Occured! Please check the previous logs...

:END
pause
