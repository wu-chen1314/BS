[CmdletBinding()]
param(
  [string]$FrontendHost = "localhost",
  [int]$FrontendPort = 5173,
  [int]$BackendPort = 8080,
  [switch]$Restart
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$tmpDir = Join-Path $repoRoot ".tmp"
$homeDir = Join-Path $repoRoot ".home"
$npmCacheDir = Join-Path $repoRoot ".npm-cache"
$stateFile = Join-Path $tmpDir "local-dev-processes.json"
$backendWorkDir = Join-Path $repoRoot "demo"
$backendWrapper = Join-Path $backendWorkDir "mvnw.cmd"
$settingsFile = Join-Path $backendWorkDir "settings.xml"
$mavenRepo = Join-Path $repoRoot ".m2\repository"
$frontendOutLog = Join-Path $tmpDir "frontend-dev.out.log"
$frontendErrLog = Join-Path $tmpDir "frontend-dev.err.log"
$backendOutLog = Join-Path $tmpDir "backend-dev.out.log"
$backendErrLog = Join-Path $tmpDir "backend-dev.err.log"

function Ensure-Directory {
  param([string]$Path)

  if (-not (Test-Path $Path)) {
    New-Item -ItemType Directory -Path $Path | Out-Null
  }
}

function Get-ListeningProcessId {
  param([int]$Port)

  $getNetTcpCommand = Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue
  if ($getNetTcpCommand) {
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
      Select-Object -First 1
    if ($connection) {
      return [int]$connection.OwningProcess
    }
  }

  $pattern = "^\s*TCP\s+\S+:$Port\s+\S+\s+LISTENING\s+(\d+)\s*$"
  $line = netstat -ano | Select-String -Pattern $pattern | Select-Object -First 1
  if ($line -and $line.Matches.Count -gt 0) {
    return [int]$line.Matches[0].Groups[1].Value
  }

  return $null
}

function Get-State {
  if (-not (Test-Path $stateFile)) {
    return $null
  }

  try {
    return Get-Content $stateFile -Raw | ConvertFrom-Json
  } catch {
    Remove-Item $stateFile -Force -ErrorAction SilentlyContinue
    return $null
  }
}

function Stop-ManagedProcess {
  param(
    [object]$ProcessState,
    [string]$Label
  )

  if (-not $ProcessState) {
    return $false
  }

  $candidatePids = @()
  foreach ($propertyName in @("wrapperPid", "servicePid", "pid")) {
    if ($ProcessState.PSObject.Properties.Name -contains $propertyName) {
      $candidatePid = [int]$ProcessState.$propertyName
      if ($candidatePid -gt 0) {
        $candidatePids += $candidatePid
      }
    }
  }

  $stopped = $false
  foreach ($candidatePid in ($candidatePids | Select-Object -Unique)) {
    if (-not (Get-Process -Id $candidatePid -ErrorAction SilentlyContinue)) {
      continue
    }

    cmd.exe /d /c "taskkill /PID $candidatePid /T /F >nul 2>nul" | Out-Null
    $stopped = $true
  }

  if ($stopped) {
    Start-Sleep -Milliseconds 600
    Write-Host "Stopped $Label."
  }

  return $stopped
}

function Stop-ProcessTree {
  param(
    [int[]]$Pids,
    [string]$Label
  )

  $stopped = $false
  foreach ($candidatePid in ($Pids | Where-Object { $_ -gt 0 } | Select-Object -Unique)) {
    if (-not (Get-Process -Id $candidatePid -ErrorAction SilentlyContinue)) {
      continue
    }

    cmd.exe /d /c "taskkill /PID $candidatePid /T /F >nul 2>nul" | Out-Null
    $stopped = $true
  }

  if ($stopped) {
    Start-Sleep -Milliseconds 800
    Write-Host "Stopped $Label."
  }

  return $stopped
}

function Wait-ForPort {
  param(
    [int]$Port,
    [int]$TimeoutSeconds,
    [string]$Label,
    [int]$ProcessId
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    $listenerPid = Get-ListeningProcessId -Port $Port
    if ($listenerPid) {
      return $listenerPid
    }

    if (-not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
      throw "$Label process exited before port $Port became ready. Check the log files in .tmp."
    }

    Start-Sleep -Milliseconds 500
  }

  throw "$Label did not become ready on port $Port within $TimeoutSeconds seconds. Check the log files in .tmp."
}

function Get-NpmCmdPath {
  $npmCmd = Get-Command npm.cmd -ErrorAction SilentlyContinue
  if ($npmCmd) {
    return $npmCmd.Source
  }

  $npm = Get-Command npm -ErrorAction SilentlyContinue
  if ($npm) {
    return $npm.Source
  }

  throw "Could not find npm.cmd or npm in PATH."
}

function Clear-LogFile {
  param([string]$Path)

  if (Test-Path $Path) {
    Remove-Item $Path -Force
  }
}

Ensure-Directory $tmpDir
Ensure-Directory $homeDir
Ensure-Directory $npmCacheDir
Ensure-Directory $mavenRepo

if (-not (Test-Path $backendWrapper)) {
  throw "Missing backend wrapper: $backendWrapper"
}

if (-not (Test-Path $settingsFile)) {
  throw "Missing Maven settings file: $settingsFile"
}

$npmCmdPath = Get-NpmCmdPath
$existingState = Get-State

if ($existingState) {
  $hasActiveManagedProcess = $false
  foreach ($label in @("backend", "frontend")) {
    if ($existingState.$label) {
      $managedPid = [int]$existingState.$label.pid
      if (Get-Process -Id $managedPid -ErrorAction SilentlyContinue) {
        $hasActiveManagedProcess = $true
      }
    }
  }

  if ($hasActiveManagedProcess) {
    if (-not $Restart) {
      throw "Detected already-running local services managed by the script. Run stop-local.ps1 first, or use -Restart."
    }

    Stop-ManagedProcess -ProcessState $existingState.frontend -Label "frontend" | Out-Null
    Stop-ManagedProcess -ProcessState $existingState.backend -Label "backend" | Out-Null
  }

  Remove-Item $stateFile -Force -ErrorAction SilentlyContinue
}

foreach ($port in @($FrontendPort, $BackendPort)) {
  $occupyingPid = Get-ListeningProcessId -Port $port
  if ($occupyingPid) {
    if ($Restart) {
      Write-Host "Port $port is occupied by PID $occupyingPid. Clearing it because -Restart was requested..."
      Stop-ProcessTree -Pids @($occupyingPid) -Label "port $port listener" | Out-Null
      $occupyingPid = Get-ListeningProcessId -Port $port
    }

    if ($occupyingPid) {
      throw "Port $port is already occupied by PID $occupyingPid. Stop the existing process before starting a new local stack."
    }
  }
}

Clear-LogFile $frontendOutLog
Clear-LogFile $frontendErrLog
Clear-LogFile $backendOutLog
Clear-LogFile $backendErrLog

$backendProcess = $null
$frontendProcess = $null
$backendServicePid = $null
$frontendServicePid = $null

try {
  $backendCommand = @(
    "set `"HOME=$homeDir`"",
    "set `"USERPROFILE=$homeDir`"",
    "set `"TEMP=$tmpDir`"",
    "set `"TMP=$tmpDir`"",
    "set `"MAVEN_OPTS=-Duser.home=$homeDir`"",
    "set `"SPRING_REDIS_ENABLED=false`"",
    "set `"SPRING_DEVTOOLS_RESTART_ENABLED=false`"",
    "call `"$backendWrapper`" -q -s `"$settingsFile`" `"-Dmaven.repo.local=$mavenRepo`" spring-boot:run"
  ) -join " && "

  $backendProcess = Start-Process `
    -FilePath "cmd.exe" `
    -ArgumentList @("/d", "/c", $backendCommand) `
    -WorkingDirectory $backendWorkDir `
    -RedirectStandardOutput $backendOutLog `
    -RedirectStandardError $backendErrLog `
    -WindowStyle Hidden `
    -PassThru

  $backendServicePid = Wait-ForPort -Port $BackendPort -TimeoutSeconds 90 -Label "Backend" -ProcessId $backendProcess.Id

  $frontendCommand = @(
    "set `"HOME=$homeDir`"",
    "set `"USERPROFILE=$homeDir`"",
    "set `"TEMP=$tmpDir`"",
    "set `"TMP=$tmpDir`"",
    "set `"npm_config_cache=$npmCacheDir`"",
    "set `"BROWSER=none`"",
    "call `"$npmCmdPath`" run dev -- --host $FrontendHost --port $FrontendPort --strictPort"
  ) -join " && "

  $frontendProcess = Start-Process `
    -FilePath "cmd.exe" `
    -ArgumentList @("/d", "/c", $frontendCommand) `
    -WorkingDirectory $repoRoot `
    -RedirectStandardOutput $frontendOutLog `
    -RedirectStandardError $frontendErrLog `
    -WindowStyle Hidden `
    -PassThru

  $frontendServicePid = Wait-ForPort -Port $FrontendPort -TimeoutSeconds 45 -Label "Frontend" -ProcessId $frontendProcess.Id

  $state = [ordered]@{
    startedAt = (Get-Date).ToString("o")
    repoRoot = $repoRoot
    frontend = [ordered]@{
      pid = $frontendProcess.Id
      wrapperPid = $frontendProcess.Id
      servicePid = $frontendServicePid
      host = $FrontendHost
      port = $FrontendPort
      url = "http://${FrontendHost}:$FrontendPort"
      stdout = $frontendOutLog
      stderr = $frontendErrLog
    }
    backend = [ordered]@{
      pid = $backendProcess.Id
      wrapperPid = $backendProcess.Id
      servicePid = $backendServicePid
      port = $BackendPort
      url = "http://localhost:$BackendPort"
      stdout = $backendOutLog
      stderr = $backendErrLog
    }
  }

  $state | ConvertTo-Json -Depth 4 | Set-Content -Path $stateFile -Encoding UTF8

  Write-Host ""
  Write-Host "Local stack started successfully."
  Write-Host "Frontend: http://${FrontendHost}:$FrontendPort"
  Write-Host "Backend : http://localhost:$BackendPort"
  Write-Host "State   : $stateFile"
  Write-Host "Logs    :"
  Write-Host "  - $frontendOutLog"
  Write-Host "  - $frontendErrLog"
  Write-Host "  - $backendOutLog"
  Write-Host "  - $backendErrLog"
  Write-Host ""
  Write-Host "Stop command:"
  Write-Host "powershell -NoProfile -ExecutionPolicy Bypass -File `"$($PSScriptRoot)\stop-local.ps1`""
} catch {
  if ($frontendProcess -and (Get-Process -Id $frontendProcess.Id -ErrorAction SilentlyContinue)) {
    cmd.exe /d /c "taskkill /PID $($frontendProcess.Id) /T /F >nul 2>nul" | Out-Null
  }

  if ($backendProcess -and (Get-Process -Id $backendProcess.Id -ErrorAction SilentlyContinue)) {
    cmd.exe /d /c "taskkill /PID $($backendProcess.Id) /T /F >nul 2>nul" | Out-Null
  }

  Remove-Item $stateFile -Force -ErrorAction SilentlyContinue
  throw
}
