[CmdletBinding()]
param(
  [switch]$ByPort,
  [int]$FrontendPort = 5173,
  [int]$BackendPort = 8080
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$tmpDir = Join-Path $repoRoot ".tmp"
$stateFile = Join-Path $tmpDir "local-dev-processes.json"
$managedPorts = @()

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

function Stop-ProcessTree {
  param(
    [int[]]$Pids,
    [string]$Label
  )

  $stopped = $false
  foreach ($candidatePid in ($Pids | Where-Object { $_ -gt 0 } | Select-Object -Unique)) {
    $process = Get-Process -Id $candidatePid -ErrorAction SilentlyContinue
    if (-not $process) {
      continue
    }

    cmd.exe /d /c "taskkill /PID $candidatePid /T /F >nul 2>nul" | Out-Null
    $stopped = $true
  }

  if ($stopped) {
    Write-Host "Stopped $Label."
  }

  return $stopped
}

$stoppedAny = $false

if (Test-Path $stateFile) {
  try {
    $state = Get-Content $stateFile -Raw | ConvertFrom-Json
  } catch {
    $state = $null
  }

  if ($state) {
    if ($state.frontend) {
      $managedPorts += [int]$state.frontend.port
      $frontendPids = @()
      foreach ($propertyName in @("wrapperPid", "servicePid", "pid")) {
        if ($state.frontend.PSObject.Properties.Name -contains $propertyName) {
          $frontendPids += [int]$state.frontend.$propertyName
        }
      }
      $stoppedAny = (Stop-ProcessTree -Pids $frontendPids -Label "frontend") -or $stoppedAny
    }

    if ($state.backend) {
      $managedPorts += [int]$state.backend.port
      $backendPids = @()
      foreach ($propertyName in @("wrapperPid", "servicePid", "pid")) {
        if ($state.backend.PSObject.Properties.Name -contains $propertyName) {
          $backendPids += [int]$state.backend.$propertyName
        }
      }
      $stoppedAny = (Stop-ProcessTree -Pids $backendPids -Label "backend") -or $stoppedAny
    }
  }

  Remove-Item $stateFile -Force -ErrorAction SilentlyContinue
}

foreach ($managedPort in ($managedPorts | Where-Object { $_ -gt 0 } | Select-Object -Unique)) {
  $residualPid = Get-ListeningProcessId -Port $managedPort
  if ($residualPid) {
    $stoppedAny = (Stop-ProcessTree -Pids @($residualPid) -Label "residual port $managedPort listener") -or $stoppedAny
  }
}

if ($ByPort) {
  foreach ($port in @($FrontendPort, $BackendPort)) {
    $portPid = Get-ListeningProcessId -Port $port
    if ($portPid) {
      $stoppedAny = (Stop-ProcessTree -Pids @($portPid) -Label "port $port listener") -or $stoppedAny
    }
  }
}

if (-not $stoppedAny) {
  Write-Host "No managed local services were running."
} else {
  Write-Host "Local services stopped."
}
