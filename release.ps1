param(
    [ValidateSet("up", "status", "logs", "restart", "down", "help")]
    [string]$Action = "up",
    [ValidateRange(30, 3600)]
    [int]$TimeoutSeconds = 300,
    [switch]$NoBuild,
    [switch]$RefreshImages
)

$ErrorActionPreference = "Stop"
$Services = @("ivis-banco", "ivis-mailpit", "ivis-api-ia", "ivis-backend", "ivis-frontend")

function Show-Usage {
    @"
Uso: .\release.ps1 [up|status|logs|restart|down|help]

  up       Prepara os .env, usa o cache de build e inicia o sistema completo.
  status   Mostra o estado dos containers.
  logs     Acompanha os logs dos serviços do sistema.
  restart  Reinicia os serviços do sistema e aguarda a prontidão.
  down     Para e remove os containers, preservando o volume do banco.
  help     Mostra esta ajuda.

Timeout de prontidão: use -TimeoutSeconds (padrão: 300).
Use -NoBuild com up para iniciar com imagens já construídas.
Use -RefreshImages com up para atualizar imagens-base antes de compilar.
"@
}

function New-RandomKey {
    $bytes = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
    return [Convert]::ToBase64String($bytes)
}

function Get-EnvironmentValue {
    param([string]$Content, [string]$Name)
    $match = [Regex]::Match($Content, "(?m)^$([Regex]::Escape($Name))=(.*)$")
    if (-not $match.Success) { return '' }
    return $match.Groups[1].Value.Trim().Trim('"', "'")
}

function Set-EnvironmentValue {
    param([string]$Content, [string]$Name, [string]$Value)
    $pattern = "(?m)^$([Regex]::Escape($Name))=.*$"
    if ([Regex]::IsMatch($Content, $pattern)) {
        return [Regex]::Replace($Content, $pattern, "$Name=$Value")
    }
    return $Content.TrimEnd() + "`r`n$Name=$Value`r`n"
}

function Ensure-EnvironmentFiles {
    if (-not (Test-Path -LiteralPath ".env" -PathType Leaf)) {
        Copy-Item -LiteralPath ".env.example" -Destination ".env"
        Write-Host "Criei .env a partir de .env.example. Revise as configurações locais."
    }

    if (-not (Test-Path -LiteralPath "api-ia\.env" -PathType Leaf)) {
        Copy-Item -LiteralPath "api-ia\.env.example" -Destination "api-ia\.env"
        Write-Host "Criei api-ia/.env a partir do exemplo. Configure NVIDIA_API_KEY antes de iniciar."
    }

    $environmentPath = Join-Path $PSScriptRoot ".env"
    $environment = [System.IO.File]::ReadAllText($environmentPath)
    $changed = $false

    $jwtKey = Get-EnvironmentValue -Content $environment -Name 'IVIS_JWT_CHAVE_ASSINATURA'
    if (-not $jwtKey) {
        $environment = Set-EnvironmentValue -Content $environment -Name 'IVIS_JWT_CHAVE_ASSINATURA' -Value (New-RandomKey)
        $changed = $true
        Write-Host "Gerei uma chave JWT local em .env. O valor não será exibido."
    }
    else {
        try { $jwtBytes = [Convert]::FromBase64String($jwtKey) }
        catch { throw 'IVIS_JWT_CHAVE_ASSINATURA em .env deve ser Base64 válido de pelo menos 32 bytes.' }
        if ($jwtBytes.Length -lt 32) {
            throw 'IVIS_JWT_CHAVE_ASSINATURA em .env deve conter pelo menos 32 bytes.'
        }
    }

    $serviceKey = Get-EnvironmentValue -Content $environment -Name 'IVIS_IA_CHAVE_SERVICO'
    if (-not $serviceKey -or $serviceKey -in @('chave-local-ficticia-substituir-antes-de-usar-123456789', 'substitua-por-uma-chave-local-com-pelo-menos-32-caracteres')) {
        $environment = Set-EnvironmentValue -Content $environment -Name 'IVIS_IA_CHAVE_SERVICO' -Value (New-RandomKey)
        $changed = $true
        Write-Host 'Gerei uma chave local para a comunicação entre backend e API de IA em .env.'
    }
    elseif ($serviceKey.Length -lt 32) {
        throw 'IVIS_IA_CHAVE_SERVICO em .env deve ter pelo menos 32 caracteres.'
    }

    if ($changed) {
        [System.IO.File]::WriteAllText($environmentPath, $environment, [System.Text.UTF8Encoding]::new($false))
    }
}

function Invoke-Compose {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    & docker compose @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose $($Arguments -join ' ') falhou (código $LASTEXITCODE). Consulte o erro do Docker acima; para serviços que não iniciaram, execute: docker compose ps -a e docker compose logs --tail=80."
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
    Wait-ForUrl -Label "API de IA (processo)" -Url "http://127.0.0.1:8000/saude"
    Wait-ForUrl -Label "backend e banco" -Url "http://127.0.0.1:8081/api/v1/pronto"
    Wait-ForUrl -Label "frontend" -Url "http://127.0.0.1:8080/"
    try {
        $null = Invoke-RestMethod -Uri "http://127.0.0.1:8000/pronto" -TimeoutSec 3 -ErrorAction Stop
    }
    catch {
        Write-Warning "A API de IA iniciou, mas os treinos estão indisponíveis. Configure NVIDIA_API_KEY em api-ia/.env e execute .\release.ps1 up novamente. Consulte docker compose logs --tail=80 ivis-api-ia se a chave já estiver configurada."
    }
    Write-Host ""
    Write-Host "Ambiente iniciado." -ForegroundColor Green
    Write-Host "Frontend: http://localhost:8080" -ForegroundColor Green
    Write-Host "API de IA: http://localhost:8000" -ForegroundColor Green
    Write-Host "Backend: http://localhost:8081/api" -ForegroundColor Green
    Write-Host "Mailpit: http://localhost:8025" -ForegroundColor Green
    Write-Host "PostgreSQL ativo no serviço ivis-banco; porta local configurável por IVIS_BD_PORTA." -ForegroundColor Green
}

if ($Action -ne 'help' -and -not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker não foi encontrado. Instale/inicie o Docker Desktop e tente novamente."
}
if ($NoBuild -and $RefreshImages) {
    throw '-NoBuild e -RefreshImages não podem ser usados juntos.'
}
if (($NoBuild -or $RefreshImages) -and $Action -ne 'up') {
    throw '-NoBuild e -RefreshImages só podem ser usados com up.'
}

Push-Location -LiteralPath $PSScriptRoot
try { switch ($Action) {
    "help" {
        Show-Usage
    }
    "up" {
        Ensure-EnvironmentFiles
        Invoke-Compose -Arguments @("config", "--quiet")
        if ($RefreshImages) {
            Invoke-Compose -Arguments @('pull', 'ivis-banco', 'ivis-mailpit')
            Invoke-Compose -Arguments @('build', '--pull', 'ivis-api-ia', 'ivis-backend', 'ivis-frontend')
        }
        $upArguments = @('up', '-d')
        if ($NoBuild -or $RefreshImages) { $upArguments += '--no-build' }
        else { $upArguments += '--build' }
        Invoke-Compose -Arguments ($upArguments + $Services)
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
} }
finally { Pop-Location }
