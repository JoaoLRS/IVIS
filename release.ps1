param(
    [ValidateSet("up", "status", "logs", "restart", "down", "help")]
    [string]$Action = "up",
    [ValidateRange(30, 3600)]
    [int]$TimeoutSeconds = 300
)

$ErrorActionPreference = "Stop"
$Services = @("ivis-banco", "ivis-mailpit", "ivis-api-ia", "ivis-backend", "ivis-frontend")
Set-Location -LiteralPath $PSScriptRoot

function Show-Usage {
    @"
Uso: .\release.ps1 [up|status|logs|restart|down|help]

  up       Cria os .env de exemplo se faltarem, compila e inicia o sistema completo com Docker.
  status   Mostra o estado dos containers.
  logs     Acompanha os logs dos serviços do sistema.
  restart  Reinicia os serviços do sistema e aguarda a prontidão.
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

    $environmentPath = Join-Path $PSScriptRoot ".env"
    $environment = [System.IO.File]::ReadAllText($environmentPath)
    if ($environment -notmatch '(?m)^IVIS_JWT_CHAVE_ASSINATURA=\S+') {
        $bytes = New-Object byte[] 32
        $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
        $jwtKey = [Convert]::ToBase64String($bytes)
        if ($environment -match '(?m)^IVIS_JWT_CHAVE_ASSINATURA=.*$') {
            $environment = [Regex]::Replace(
                $environment,
                '(?m)^IVIS_JWT_CHAVE_ASSINATURA=.*$',
                "IVIS_JWT_CHAVE_ASSINATURA=$jwtKey"
            )
        } else {
            $environment = $environment.TrimEnd() + "`r`nIVIS_JWT_CHAVE_ASSINATURA=$jwtKey`r`n"
        }
        [System.IO.File]::WriteAllText(
            $environmentPath,
            $environment,
            [System.Text.UTF8Encoding]::new($false)
        )
        Write-Host "Gerei uma chave JWT local em .env. O valor não será exibido."
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
    & docker compose logs --tail=80 @Services
    throw "Inicialização incompleta. Consulte os logs acima."
}

function Wait-ForStack {
    Wait-ForUrl -Label "API de IA" -Url "http://127.0.0.1:8000/pronto"
    Wait-ForUrl -Label "backend e banco" -Url "http://127.0.0.1:8081/api/v1/pronto"
    Wait-ForUrl -Label "frontend" -Url "http://127.0.0.1:8080/"
    Write-Host ""
    Write-Host "Ambiente iniciado." -ForegroundColor Green
    Write-Host "Frontend: http://localhost:8080" -ForegroundColor Green
    Write-Host "API de IA: http://localhost:8000" -ForegroundColor Green
    Write-Host "Backend: http://localhost:8081/api" -ForegroundColor Green
    Write-Host "Mailpit: http://localhost:8025" -ForegroundColor Green
    Write-Host "PostgreSQL ativo no serviço ivis-banco; porta local configurável por IVIS_BD_PORTA." -ForegroundColor Green
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
