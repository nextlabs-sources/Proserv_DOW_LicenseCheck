@echo off
set /p NXLPath=Enter Nextlabs install directory : 

if exist "%NXLPath%\Policy Controller" (
    echo Found Policy Controller folder.
	
	REM SET PATH=%PATH%;%SystemRoot%\System32;%SystemRoot%;%SystemRoot%\System32\Wbem
	echo Stopping PC. This may take a while...
	"%NXLPath%\Policy Controller\bin\StopEnforcer.exe"
	echo Done.
	
	if exist "%NXLPath%\Policy Controller\jservice\config" (
		echo config directory exists.
	) else (
		echo config directory does not exist. Creating.
		mkdir "%NXLPath%\Policy Controller\jservice\config"
	)
	
	if exist "%NXLPath%\Policy Controller\jservice\jar" (
		echo jar directory exists.
	) else (
		echo jar directory does not exist. Creating.
		mkdir "%NXLPath%\Policy Controller\jservice\jar"
	)
	
	echo Copying DowAdvancedCondition.properties file.
	copy DowAdvancedCondition.properties "%NXLPath%\Policy Controller\jservice\config"
	echo Done.
	
	echo Copying DowLicenseCheck JAR file.
	copy DowLicenseCheck.jar "%NXLPath%\Policy Controller\jservice\jar"
	echo Done.
		
	NET start ComplianceEnforcerService
	
) else (
    echo Could not find Policy Controller folder. Quitting.
)
