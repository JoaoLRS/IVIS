param(
    [ValidateSet('install', 'update', 'help')]
    [string]$Action = 'install',
    [switch]$BuildDocker
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

function Show-Usage {
    @'
Uso: .\config.ps1 [install|update|help] [-BuildDocker]

  install       Instala as versoes registradas nos arquivos de dependencias (padrao).
  update        Atualiza pacotes npm dentro das faixas do package.json e
                sincroniza Python e Gradle com seus arquivos de dependencias.
  -BuildDocker  Reconstroi as imagens do frontend, backend e API de IA com
                as imagens-base mais recentes. Nao inicia os containers.

Requisitos locais: Node.js 22+, Python 3.12 ou 3.13 e JDK 25.
O JDK 25 é localizado por JAVA_HOME, PATH ou pastas padrão de instalação.
Para iniciar o sistema completo depois, execute .\release.ps1 up.
'@
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string]$Command,
        [string[]]$Arguments = @()
    )

    Write-Host "`n[$Label] $Command $($Arguments -join ' ')" -ForegroundColor Cyan
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Label falhou (codigo $LASTEXITCODE)."
    }
}

function Require-Command {
    param([string]$Name, [string]$Hint)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "$Name nao encontrado. $Hint"
    }
}

function Install-NpmPackage {
    param([string]$Directory)
    $manifest = Join-Path $Directory 'package.json'
    $lock = Join-Path $Directory 'package-lock.json'
    if (-not (Test-Path -LiteralPath $manifest -PathType Leaf)) {
        throw "Manifesto ausente: $manifest"
    }

    Push-Location -LiteralPath $Directory
    try {
        if ($Action -eq 'update') {
            Invoke-Checked -Label "npm update ($Directory)" -Command 'npm' -Arguments @('update')
        }
        elseif (Test-Path -LiteralPath $lock -PathType Leaf) {
            Invoke-Checked -Label "npm ci ($Directory)" -Command 'npm' -Arguments @('ci')
        }
        else {
            Invoke-Checked -Label "npm install ($Directory)" -Command 'npm' -Arguments @('install')
        }
    }
    finally {
        Pop-Location
    }
}

function Get-PythonLauncher {
    if (Get-Command py -ErrorAction SilentlyContinue) {
        foreach ($version in @('3.12', '3.13')) {
            try {
                & py "-$version" --version 2>$null
                if ($LASTEXITCODE -eq 0) {
                    return @{ Command = 'py'; Arguments = @("-$version") }
                }
            }
            catch {
                # A versao solicitada nao esta instalada; tente a proxima.
            }
        }
    }
    foreach ($name in @('python', 'python3')) {
        if (Get-Command $name -ErrorAction SilentlyContinue) {
            $version = & $name --version 2>$null
            if ($LASTEXITCODE -eq 0 -and $version -match '^Python 3\.(12|13)\.') {
                return @{ Command = $name; Arguments = @() }
            }
        }
    }
    throw 'Python 3.12 ou 3.13 nao encontrado. Instale uma dessas versoes e tente novamente.'
}

function Install-PythonPackages {
    $apiDirectory = Join-Path $root 'api-ia'
    $requirements = Join-Path $apiDirectory 'requirements.txt'
    $venvPython = Join-Path $apiDirectory '.venv\Scripts\python.exe'
    if (-not (Test-Path -LiteralPath $requirements -PathType Leaf)) {
        throw "Arquivo ausente: $requirements"
    }

    if (-not (Test-Path -LiteralPath $venvPython -PathType Leaf)) {
        $launcher = Get-PythonLauncher
        Invoke-Checked -Label 'Criar ambiente virtual da API de IA' -Command $launcher.Command -Arguments ($launcher.Arguments + @('-m', 'venv', (Join-Path $apiDirectory '.venv')))
    }

    $version = & $venvPython --version
    if ($LASTEXITCODE -ne 0 -or $version -notmatch '^Python 3\.(12|13)\.') {
        throw "O ambiente api-ia/.venv precisa usar Python 3.12 ou 3.13 (encontrado: $version). Recrie-o com a versao correta."
    }

    $arguments = @('-m', 'pip', 'install')
    if ($Action -eq 'update') { $arguments += '--upgrade' }
    $arguments += @('-r', $requirements)
    Invoke-Checked -Label 'Dependencias da API de IA' -Command $venvPython -Arguments $arguments
}

function Get-BackendJdkHome {
    $candidates = [System.Collections.Generic.List[string]]::new()
    if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME) }

    $javac = Get-Command javac -ErrorAction SilentlyContinue
    if ($javac -and $javac.Source) {
        $candidates.Add((Split-Path -Parent (Split-Path -Parent $javac.Source)))
    }

    foreach ($directory in @('C:\Program Files\OpenJDK', 'C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium')) {
        if (Test-Path -LiteralPath $directory -PathType Container) {
            Get-ChildItem -LiteralPath $directory -Directory -Filter '*25*' -ErrorAction SilentlyContinue |
                ForEach-Object { $candidates.Add($_.FullName) }
        }
    }

    foreach ($candidate in $candidates) {
        $jdkCandidatePath = $candidate.Trim().Trim('"')
        $java = Join-Path $jdkCandidatePath 'bin\java.exe'
        $compiler = Join-Path $jdkCandidatePath 'bin\javac.exe'
        if (-not (Test-Path -LiteralPath $java -PathType Leaf) -or -not (Test-Path -LiteralPath $compiler -PathType Leaf)) {
            continue
        }
        $versionOutput = & $java --version
        $version = $versionOutput | Select-Object -First 1
        if ($LASTEXITCODE -eq 0 -and $version -match '^(?:openjdk|java)(?: version)? "?25(?:\.|\s|"|$)') {
            return $jdkCandidatePath
        }
    }

    throw 'O backend requer JDK 25. Configure JAVA_HOME para a pasta do JDK 25; para compilar apenas com Docker, use .\release.ps1 up.'
}

function Install-BackendDependencies {
    param([Parameter(Mandatory)][string]$JdkHome)
    $backendDirectory = Join-Path $root 'backend'
    $wrapper = Join-Path $backendDirectory 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
        throw "Gradle Wrapper ausente: $wrapper"
    }
    Push-Location -LiteralPath $backendDirectory
    $previousJavaHome = $env:JAVA_HOME
    try {
        $env:JAVA_HOME = $JdkHome
        $arguments = @('bootJar', '--no-daemon', '--console=plain')
        if ($Action -eq 'update') { $arguments += '--refresh-dependencies' }
        Invoke-Checked -Label 'Dependencias e build do backend' -Command $wrapper -Arguments $arguments
    }
    finally {
        $env:JAVA_HOME = $previousJavaHome
        Pop-Location
    }
}

function Build-DockerImages {
    Require-Command -Name 'docker' -Hint 'Instale e inicie o Docker Desktop.'
    Invoke-Checked -Label 'Docker Compose' -Command 'docker' -Arguments @('compose', 'version')

    foreach ($relativePath in @('.env', 'api-ia\.env')) {
        $target = Join-Path $root $relativePath
        if (-not (Test-Path -LiteralPath $target -PathType Leaf)) {
            Copy-Item -LiteralPath "$target.example" -Destination $target
            Write-Host "Criado $relativePath a partir do exemplo."
        }
    }

    $environmentPath = Join-Path $root '.env'
    $environment = [System.IO.File]::ReadAllText($environmentPath)
    if ($environment -notmatch '(?m)^IVIS_JWT_CHAVE_ASSINATURA=\S+') {
        $bytes = New-Object byte[] 32
        $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
        $key = [Convert]::ToBase64String($bytes)
        if ($environment -match '(?m)^IVIS_JWT_CHAVE_ASSINATURA=.*$') {
            $environment = [Regex]::Replace($environment, '(?m)^IVIS_JWT_CHAVE_ASSINATURA=.*$', "IVIS_JWT_CHAVE_ASSINATURA=$key")
        }
        else {
            $environment = $environment.TrimEnd() + "`r`nIVIS_JWT_CHAVE_ASSINATURA=$key`r`n"
        }
        [System.IO.File]::WriteAllText($environmentPath, $environment, [System.Text.UTF8Encoding]::new($false))
        Write-Host 'Chave JWT local gerada em .env.'
    }

    $serviceKeyMatch = [Regex]::Match($environment, '(?m)^IVIS_IA_CHAVE_SERVICO=(.*)$')
    $serviceKey = if ($serviceKeyMatch.Success) { $serviceKeyMatch.Groups[1].Value.Trim() } else { '' }
    if (-not $serviceKey -or $serviceKey -eq 'chave-local-ficticia-substituir-antes-de-usar-123456789') {
        $bytes = New-Object byte[] 32
        $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
        $key = [Convert]::ToBase64String($bytes)
        if ($serviceKeyMatch.Success) {
            $environment = [Regex]::Replace($environment, '(?m)^IVIS_IA_CHAVE_SERVICO=.*$', "IVIS_IA_CHAVE_SERVICO=$key")
        }
        else {
            $environment = $environment.TrimEnd() + "`r`nIVIS_IA_CHAVE_SERVICO=$key`r`n"
        }
        [System.IO.File]::WriteAllText($environmentPath, $environment, [System.Text.UTF8Encoding]::new($false))
        Write-Host 'Chave local da API de IA gerada em .env.'
    }

    Push-Location -LiteralPath $root
    try {
        Invoke-Checked -Label 'Validar Compose' -Command 'docker' -Arguments @('compose', 'config', '--quiet')
        Invoke-Checked -Label 'Construir imagens Docker' -Command 'docker' -Arguments @('compose', 'build', '--pull', 'ivis-api-ia', 'ivis-backend', 'ivis-frontend')
    }
    finally {
        Pop-Location
    }
}

try {
    if ($Action -eq 'help') {
        Show-Usage
        return
    }

    Require-Command -Name 'node' -Hint 'Instale Node.js 22 ou superior.'
    Require-Command -Name 'npm' -Hint 'Instale Node.js 22 ou superior.'
    $nodeVersion = & node --version
    if ($LASTEXITCODE -ne 0 -or $nodeVersion -notmatch '^v(\d+)\.' -or [int]$Matches[1] -lt 22) {
        throw "Node.js 22 ou superior e necessario (encontrado: $nodeVersion)."
    }
    $jdkHome = Get-BackendJdkHome
    Write-Host "JDK 25 do backend: $jdkHome"

    Install-NpmPackage -Directory $root
    Install-NpmPackage -Directory (Join-Path $root 'frontend')
    Install-NpmPackage -Directory (Join-Path $root 'cypress\e2e')
    Install-PythonPackages
    Install-BackendDependencies -JdkHome $jdkHome
    Invoke-Checked -Label 'Build do frontend' -Command 'npm' -Arguments @('--prefix', (Join-Path $root 'frontend'), 'run', 'build')
    if ($BuildDocker) { Build-DockerImages }

    Write-Host "`nConfiguracao concluida. Para iniciar: .\release.ps1 up" -ForegroundColor Green
}
catch {
    [Console]::Error.WriteLine("Erro: $($_.Exception.Message)")
    exit 1
}
