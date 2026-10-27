param(
    [ValidateSet("up", "status", "logs", "restart", "down", "help")]
    [string]$Action = "up",
    [ValidateRange(30, 3600)]
    [int]$TimeoutSeconds = 300
)

$ErrorActionPreference = "Stop"
$Services = @("ivis-banco", "ivis-api-ia", "ivis-backend")
Set-Location -LiteralPath $PSScriptRoot

function Show-Usage {
    @"
Uso: .\release.ps1 [up|status|logs|restart|down|help]

  up       Cria os .env de exemplo se faltarem, compila e inicia banco, API de IA e backend.
  status   Mostra o estado dos containers.
  logs     Acompanha os logs dos três serviços.
  restart  Reinicia os três serviços e aguarda a prontidão.
  down     Para e remove os containers, preservando o volume do banco.
  help     Mostra esta ajuda.

Timeout de prontidão: use -TimeoutSeconds (padrão: 300).
"@
}

function Ensure-EnvironmentFiles {
    if (-not (Test-Path -LiteralPath ".env" -PathType Leaf)) {
        Copy-Item -LiteralPath ".env.example" -Destination ".env"
        Write-Host "Criei .env a partir de .env.example. Revise as configurações locais."
    }

    if (-not (Test-Path -LiteralPath "api-ia\.env" -PathType Leaf)) {
        Copy-Item -LiteralPath "api-ia\.env.example" -Destination "api-ia\.env"
        Write-Host "Criei api-ia/.env a partir do exemplo. O stub fica ativo por padrão."
    }
}

function Invoke-Compose {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    & docker compose @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose falhou (código $LASTEXITCODE)."
    }
}

function Wait-ForUrl {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][string]$Url
    )

    Write-Host "Aguardando $Label em $Url"
    $attempts = [Math]::Ceiling($TimeoutSeconds / 2)
    for ($attempt = 1; $attempt -le $attempts; $attempt++) {
        try {
            $null = Invoke-RestMethod -Uri $Url -TimeoutSec 3 -ErrorAction Stop
            Write-Host "$Label está pronto." -ForegroundColor Green
            return
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }

    Write-Host "$Label não ficou pronto em $TimeoutSeconds segundos." -ForegroundColor Red
    & docker compose ps
    & docker compose logs --tail=80 ivis-banco ivis-api-ia ivis-backend
    throw "Inicialização incompleta. Consulte os logs acima."
}

function Wait-ForStack {
    Wait-ForUrl -Label "API de IA" -Url "http://127.0.0.1:8000/pronto"
    Wait-ForUrl -Label "backend e banco" -Url "http://127.0.0.1:8081/api/v1/pronto"
    Write-Host ""
    Write-Host "Ambiente iniciado. API de IA: http://localhost:8000" -ForegroundColor Green
    Write-Host "Backend: http://localhost:8081/api" -ForegroundColor Green
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker não foi encontrado. Instale/inicie o Docker Desktop e tente novamente."
}

switch ($Action) {
    "help" {
        Show-Usage
    }
    "up" {
        Ensure-EnvironmentFiles
        Invoke-Compose -Arguments @("config", "--quiet")
        Invoke-Compose -Arguments (@("up", "--build", "-d") + $Services)
        Wait-ForStack
    }
    "status" {
        Ensure-EnvironmentFiles
        Invoke-Compose -Arguments @("ps")
    }
    "logs" {
        Ensure-EnvironmentFiles
        Invoke-Compose -Arguments (@("logs", "-f", "--tail=100") + $Services)
    }
    "restart" {
        Ensure-EnvironmentFiles
        Invoke-Compose -Arguments (@("restart") + $Services)
        Wait-ForStack
    }
    "down" {
        Ensure-EnvironmentFiles
        Invoke-Compose -Arguments @("down")
        Write-Host "Containers parados. O volume ivis-banco-dados foi preservado."
    }
}
