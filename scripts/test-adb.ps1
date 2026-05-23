param(
    [string]$DeviceId = "",
    [string]$PackageName = "com.callguardian.app",
    [string]$MainActivity = ".MainActivity",
    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk",
    [string]$OutputDir = "build\adb-smoke",
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [switch]$SkipNavigationCapture,
    [switch]$TrySetCallScreeningRole,
    [switch]$ClearLogcat
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "== $Message ==" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Invoke-Checked {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [switch]$AllowFailure,
        [switch]$Quiet
    )

    $display = "$FilePath $($Arguments -join ' ')"
    Write-Host "> $display"
    $oldErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $FilePath @Arguments 2>&1
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $oldErrorActionPreference
    }
    if ($output -and -not $Quiet) {
        $output | ForEach-Object { Write-Host $_ }
    }
    if ($code -ne 0 -and -not $AllowFailure) {
        throw "Command failed ($code): $display"
    }
    return @{
        Code = $code
        Output = ($output -join [Environment]::NewLine)
    }
}

function Invoke-Adb {
    param(
        [string[]]$Arguments,
        [switch]$AllowFailure,
        [switch]$Quiet
    )

    $adbArgs = @()
    if ($DeviceId.Trim().Length -gt 0) {
        $adbArgs += @("-s", $DeviceId)
    }
    $adbArgs += $Arguments
    return Invoke-Checked -FilePath "adb" -Arguments $adbArgs -AllowFailure:$AllowFailure -Quiet:$Quiet
}

function Save-Text {
    param(
        [string]$Path,
        [string]$Text
    )

    $parent = Split-Path -Parent $Path
    if ($parent -and -not (Test-Path $parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    Set-Content -Path $Path -Value $Text -Encoding UTF8
}

function Get-DeviceSize {
    $sizeResult = Invoke-Adb -Arguments @("shell", "wm", "size") -AllowFailure -Quiet
    if ($sizeResult.Output -match "Physical size:\s*(\d+)x(\d+)") {
        return @{
            Width = [int]$Matches[1]
            Height = [int]$Matches[2]
        }
    }
    return @{
        Width = 1080
        Height = 2400
    }
}

function Capture-AppScreen {
    param([string]$Name)

    $remoteXml = "/sdcard/callguardian-$Name.xml"
    $remotePng = "/sdcard/callguardian-$Name.png"
    Invoke-Adb -Arguments @("shell", "uiautomator", "dump", $remoteXml) -AllowFailure -Quiet | Out-Null
    Invoke-Adb -Arguments @("pull", $remoteXml, (Join-Path $OutputDir "$Name.xml")) -AllowFailure -Quiet | Out-Null
    Invoke-Adb -Arguments @("shell", "screencap", "-p", $remotePng) -AllowFailure -Quiet | Out-Null
    Invoke-Adb -Arguments @("pull", $remotePng, (Join-Path $OutputDir "$Name.png")) -AllowFailure -Quiet | Out-Null
}

Write-Step "Prepare output"
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$resolvedOutputDir = (Resolve-Path $OutputDir).Path
Write-Ok "Report folder: $resolvedOutputDir"

Write-Step "Check device"
$devices = Invoke-Checked -FilePath "adb" -Arguments @("devices")
Save-Text -Path (Join-Path $OutputDir "adb-devices.txt") -Text $devices.Output

if ($DeviceId.Trim().Length -eq 0) {
    $onlineDevices = @($devices.Output -split "`r?`n" |
        Where-Object { $_ -match "^\S+\s+device$" } |
        ForEach-Object { ($_ -split "\s+")[0] })
    if ($onlineDevices.Count -eq 1) {
        $DeviceId = $onlineDevices[0]
        Write-Ok "Using device: $DeviceId"
    } elseif ($onlineDevices.Count -gt 1) {
        throw "Multiple devices found. Re-run with -DeviceId <serial>."
    } else {
        throw "No online ADB device found."
    }
} else {
    Write-Ok "Using requested device: $DeviceId"
}

$androidVersion = Invoke-Adb -Arguments @("shell", "getprop", "ro.build.version.release")
$sdkVersion = Invoke-Adb -Arguments @("shell", "getprop", "ro.build.version.sdk")
Save-Text -Path (Join-Path $OutputDir "device.txt") -Text @"
device=$DeviceId
android=$($androidVersion.Output.Trim())
sdk=$($sdkVersion.Output.Trim())
"@
Write-Ok "Android $($androidVersion.Output.Trim()) / SDK $($sdkVersion.Output.Trim())"

if (-not $SkipBuild) {
    Write-Step "Build and unit tests"
    Invoke-Checked -FilePath ".\gradlew.bat" -Arguments @(":app:testDebugUnitTest", ":app:assembleDebug") | Out-Null
    Write-Ok "Build and unit tests passed"
}

if (-not (Test-Path $ApkPath)) {
    throw "APK not found: $ApkPath"
}

if (-not $SkipInstall) {
    Write-Step "Install APK"
    Invoke-Adb -Arguments @("install", "-r", "-t", $ApkPath) | Out-Null
    Write-Ok "APK installed"
}

Write-Step "Grant runtime permissions"
$permissions = @(
    "android.permission.READ_CONTACTS",
    "android.permission.POST_NOTIFICATIONS"
)
foreach ($permission in $permissions) {
    Invoke-Adb -Arguments @("shell", "pm", "grant", $PackageName, $permission) -AllowFailure | Out-Null
}
Write-Ok "Permission grant attempts completed"

Write-Step "Overlay app-op"
Invoke-Adb -Arguments @("shell", "appops", "set", $PackageName, "SYSTEM_ALERT_WINDOW", "allow") -AllowFailure | Out-Null
$overlay = Invoke-Adb -Arguments @("shell", "appops", "get", $PackageName, "SYSTEM_ALERT_WINDOW") -AllowFailure
Save-Text -Path (Join-Path $OutputDir "overlay-appops.txt") -Text $overlay.Output

Write-Step "Call screening role"
$role = Invoke-Adb -Arguments @("shell", "cmd", "role", "get-role-holders", "android.app.role.CALL_SCREENING") -AllowFailure
if ($role.Output -notmatch [regex]::Escape($PackageName) -and $TrySetCallScreeningRole) {
    Write-Warn "Role not held. Trying to assign CALL_SCREENING role."
    Invoke-Adb -Arguments @("shell", "cmd", "role", "add-role-holder", "android.app.role.CALL_SCREENING", $PackageName, "0") -AllowFailure | Out-Null
    $role = Invoke-Adb -Arguments @("shell", "cmd", "role", "get-role-holders", "android.app.role.CALL_SCREENING") -AllowFailure
}
Save-Text -Path (Join-Path $OutputDir "call-screening-role.txt") -Text $role.Output
if ($role.Output -match [regex]::Escape($PackageName)) {
    Write-Ok "CALL_SCREENING role held by $PackageName"
} else {
    Write-Warn "CALL_SCREENING role is not assigned to $PackageName"
}

if ($ClearLogcat) {
    Write-Step "Clear logcat"
    Invoke-Adb -Arguments @("logcat", "-c") | Out-Null
    Write-Ok "Logcat cleared"
}

Write-Step "Launch app"
Invoke-Adb -Arguments @("shell", "am", "start", "-n", "$PackageName/$MainActivity") | Out-Null
Start-Sleep -Seconds 2

Write-Step "Capture UI and screenshots"
Capture-AppScreen -Name "ui"

if (-not $SkipNavigationCapture) {
    Write-Step "Capture main navigation tabs"
    $size = Get-DeviceSize
    $tapY = [int]($size.Height * 0.935)
    $tabs = @(
        @{ Name = "protection"; X = 0.10 },
        @{ Name = "rules"; X = 0.30 },
        @{ Name = "log"; X = 0.50 },
        @{ Name = "stats"; X = 0.70 },
        @{ Name = "settings"; X = 0.90 }
    )
    foreach ($tab in $tabs) {
        $tapX = [int]($size.Width * $tab.X)
        Invoke-Adb -Arguments @("shell", "input", "tap", "$tapX", "$tapY") -AllowFailure -Quiet | Out-Null
        Start-Sleep -Milliseconds 700
        Capture-AppScreen -Name $tab.Name
    }
}

$focus = Invoke-Adb -Arguments @("shell", "dumpsys", "window") -AllowFailure -Quiet
Save-Text -Path (Join-Path $OutputDir "window.txt") -Text $focus.Output

Write-Step "Collect package, telecom, and logcat diagnostics"
$packageDump = Invoke-Adb -Arguments @("shell", "dumpsys", "package", $PackageName) -AllowFailure -Quiet
Save-Text -Path (Join-Path $OutputDir "package.txt") -Text $packageDump.Output

$telecomDump = Invoke-Adb -Arguments @("shell", "dumpsys", "telecom") -AllowFailure -Quiet
Save-Text -Path (Join-Path $OutputDir "telecom.txt") -Text $telecomDump.Output

$logcat = Invoke-Adb -Arguments @("logcat", "-d", "-v", "time") -AllowFailure -Quiet
Save-Text -Path (Join-Path $OutputDir "logcat.txt") -Text $logcat.Output
$filteredLogcat = ($logcat.Output -split "`r?`n" | Where-Object {
    $_ -match "CallGuardian|callguardian|GuardianCallScreeningService|CallScreening|SCREENING"
}) -join [Environment]::NewLine
Save-Text -Path (Join-Path $OutputDir "logcat-callguardian.txt") -Text $filteredLogcat

Write-Step "Summary"
$installed = Invoke-Adb -Arguments @("shell", "pm", "list", "packages", $PackageName) -AllowFailure
$currentFocus = (($focus.Output -split "`r?`n") | Where-Object { $_ -match "mCurrentFocus|mFocusedApp" }) -join [Environment]::NewLine
$summary = @"
CallGuardian ADB smoke test

Device: $DeviceId
Android: $($androidVersion.Output.Trim()) / SDK $($sdkVersion.Output.Trim())
Package: $PackageName
Installed: $($installed.Output.Trim())
Call screening role:
$($role.Output.Trim())
Overlay:
$($overlay.Output.Trim())
Current focus:
$currentFocus

Artifacts:
$resolvedOutputDir
"@
Save-Text -Path (Join-Path $OutputDir "summary.txt") -Text $summary
Write-Host $summary

Write-Ok "ADB smoke test completed"
