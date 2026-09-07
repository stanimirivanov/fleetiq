[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$KeepInfrastructure,
    [ValidateRange(15, 300)]
    [int]$TimeoutSeconds = 90
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repoRoot 'infra/docker-compose/docker-compose.yml'
$runDirectory = Join-Path $repoRoot ("target/mvb-demo/{0}" -f (Get-Date -Format 'yyyyMMdd-HHmmss'))
$processes = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()
$startedAt = [DateTimeOffset]::UtcNow

function Assert-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found on PATH."
    }
}

function Wait-TcpPort([int]$Port, [string]$Name) {
    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
    while ([DateTimeOffset]::UtcNow -lt $deadline) {
        try {
            $client = [System.Net.Sockets.TcpClient]::new()
            $client.Connect('127.0.0.1', $Port)
            $client.Dispose()
            return
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }
    throw "$Name did not listen on port $Port within $TimeoutSeconds seconds."
}

function Wait-DatabaseStable([string]$Container, [string]$Database) {
    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
    $previousStart = $null
    while ([DateTimeOffset]::UtcNow -lt $deadline) {
        $currentStart = & docker exec $Container psql -U fleetiq -d $Database -tAc `
            'SELECT pg_postmaster_start_time()' 2>$null
        if ($LASTEXITCODE -eq 0 -and $currentStart) {
            $currentStart = $currentStart.Trim()
            if ($currentStart -eq $previousStart) { return }
            $previousStart = $currentStart
        } else {
            $previousStart = $null
        }
        # The image entrypoint briefly starts PostgreSQL while running init scripts.
        # Five seconds between identical start times distinguishes the final server.
        Start-Sleep -Seconds 5
    }
    throw "$Database did not reach a stable PostgreSQL server within $TimeoutSeconds seconds."
}

function Start-FleetIqProcess(
    [string]$Name,
    [string]$Jar,
    [hashtable]$Environment
) {
    $stdout = Join-Path $runDirectory "$Name.out.log"
    $stderr = Join-Path $runDirectory "$Name.err.log"
    $process = Start-Process -FilePath 'java' -ArgumentList @('-jar', $Jar) `
        -WorkingDirectory $repoRoot -Environment $Environment -WindowStyle Hidden `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    $processes.Add($process)
    return $process
}

function Read-Count([string]$Container, [string]$Database, [string]$Sql) {
    $result = & docker exec $Container psql -v ON_ERROR_STOP=1 -U fleetiq -d $Database -tAc $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "Database assertion failed in $Database."
    }
    return [int]$result.Trim()
}

Assert-Command 'docker'
Assert-Command 'java'
Assert-Command 'mvn.cmd'
New-Item -ItemType Directory -Force -Path $runDirectory | Out-Null

try {
    Push-Location $repoRoot
    try {
        if (-not $SkipBuild) {
            & mvn.cmd -pl ':telemetry-ingestion,:fleet-topology,:simulator' -am package -DskipTests
            if ($LASTEXITCODE -ne 0) { throw 'MVB demo build failed.' }
        }

        & docker compose -f $composeFile up -d --wait postgres topology-postgres mosquitto
        if ($LASTEXITCODE -ne 0) { throw 'MVB infrastructure failed to become healthy.' }
        Wait-DatabaseStable 'fleetiq-postgres' 'telemetry_db'
        Wait-DatabaseStable 'fleetiq-topology-postgres' 'topology_db'

        $commonEnvironment = @{
            QUARKUS_OIDC_ENABLED = 'false'
            QUARKUS_OTEL_SDK_DISABLED = 'true'
            FLEETIQ_MQTT_HOST = 'localhost'
            FLEETIQ_MQTT_PORT = '1883'
        }
        $telemetryEnvironment = $commonEnvironment.Clone()
        $telemetryEnvironment['FLEETIQ_MQTT_USERNAME'] = 'telemetry-ingestion'
        $telemetryEnvironment['FLEETIQ_MQTT_PASSWORD'] = 'telemetry-ingestion-dev'
        $telemetryEnvironment['QUARKUS_DATASOURCE_JDBC_URL'] = 'jdbc:postgresql://localhost:5432/telemetry_db'
        $telemetryEnvironment['QUARKUS_DATASOURCE_REACTIVE_URL'] = 'postgresql://localhost:5432/telemetry_db'

        $topologyEnvironment = $commonEnvironment.Clone()
        $topologyEnvironment['FLEETIQ_MQTT_USERNAME'] = 'fleet-topology'
        $topologyEnvironment['FLEETIQ_MQTT_PASSWORD'] = 'fleet-topology-dev'
        $topologyEnvironment['QUARKUS_DATASOURCE_JDBC_URL'] = 'jdbc:postgresql://localhost:5434/topology_db'
        $topologyEnvironment['QUARKUS_DATASOURCE_REACTIVE_URL'] = 'postgresql://localhost:5434/topology_db'

        $simulatorEnvironment = $commonEnvironment.Clone()
        $simulatorEnvironment['FLEETIQ_TENANT_ID'] = 'demo'
        $simulatorEnvironment['FLEETIQ_MQTT_USERNAME'] = 'demo'
        $simulatorEnvironment['FLEETIQ_MQTT_PASSWORD'] = 'simulator-demo-dev'

        Start-FleetIqProcess 'telemetry-ingestion' `
            (Join-Path $repoRoot 'services/telemetry-ingestion/target/quarkus-app/quarkus-run.jar') `
            $telemetryEnvironment | Out-Null
        Start-FleetIqProcess 'fleet-topology' `
            (Join-Path $repoRoot 'services/fleet-topology/target/quarkus-app/quarkus-run.jar') `
            $topologyEnvironment | Out-Null

        Wait-TcpPort 8081 'Telemetry Ingestion'
        Wait-TcpPort 8083 'Fleet Topology'

        Start-FleetIqProcess 'simulator' `
            (Join-Path $repoRoot 'simulator/target/quarkus-app/quarkus-run.jar') `
            $simulatorEnvironment | Out-Null

        $threshold = $startedAt.ToString('yyyy-MM-dd HH:mm:ss.fff+00')
        $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
        while ([DateTimeOffset]::UtcNow -lt $deadline) {
            $telemetryCount = Read-Count 'fleetiq-postgres' 'telemetry_db' (
                "SELECT count(*) FROM telemetry_samples WHERE tenant_id='demo' AND vin='SIM-VIN-001' AND time >= '$threshold'::timestamptz")
            $topologyCount = Read-Count 'fleetiq-topology-postgres' 'topology_db' (
                "SELECT count(*) FROM topology_vehicle_projection WHERE tenant_id='demo' AND vin='SIM-VIN-001' AND position_observed_at >= '$threshold'::timestamptz")
            if ($telemetryCount -gt 0 -and $topologyCount -gt 0) {
                Write-Host "PASS: simulator -> secured MQTT -> telemetry_db -> outbox/MQTT -> topology_db"
                Write-Host "Telemetry samples: $telemetryCount; topology projections: $topologyCount"
                Write-Host "Logs: $runDirectory"
                return
            }
            Start-Sleep -Seconds 1
        }

        throw "The telemetry/topology slice did not complete within $TimeoutSeconds seconds. Logs: $runDirectory"
    } finally {
        Pop-Location
    }
} finally {
    foreach ($process in $processes) {
        if (-not $process.HasExited) {
            Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
        }
    }
    if (-not $KeepInfrastructure) {
        & docker compose -f $composeFile stop postgres topology-postgres mosquitto | Out-Null
    }
}
