param(
    [ValidateSet('All','Galacticraft')][string]$Project = 'All',
    [string[]]$Tasks = @(':neoforge:remapJar'),
    [switch]$Offline,
    [string]$GradlePath = ''
)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$mappedDrive = $null
try {
    # Java argument files can misread non-ASCII paths on Windows. Map this checkout only.
    if ($IsWindows -and $projectRoot -match '[^\x00-\x7F]') {
        $usedDrives = @(Get-PSDrive -PSProvider FileSystem | Select-Object -ExpandProperty Name)
        foreach ($letter in @('R','Q','T','U','V','W','X','Y','Z')) {
            if ($letter -notin $usedDrives -and !(Test-Path -LiteralPath "$letter`:\")) {
                & subst.exe "$letter`:" $projectRoot
                if ($LASTEXITCODE -ne 0) { throw 'Cannot map an ASCII build path.' }
                $mappedDrive = $letter
                $projectRoot = "$letter`:\"
                break
            }
        }
        if (!$mappedDrive) { throw 'No free drive letter for the ASCII build path.' }
    }
    Push-Location $projectRoot
    try {
        & python -X utf8 scripts/prepare_dependencies.py
        if ($LASTEXITCODE -ne 0) { throw 'Dependency preparation failed.' }
    } finally { Pop-Location }
    $builds = @()
    if ($Project -eq 'All') {
        $builds += @{ Path = 'vendor/DynamicDimensions'; Tasks = @(':neoforge:jar') }
        $builds += @{ Path = 'vendor/MachineLib'; Tasks = @(':neoforge:remapJar') }
    }
    $builds += @{ Path = '.'; Tasks = $Tasks }
    foreach ($build in $builds) {
        Push-Location (Join-Path $projectRoot $build.Path)
        try {
            $runner = $GradlePath
            if (!$runner) { $runner = if ($IsWindows) { '.\gradlew.bat' } else { './gradlew' } }
            $arguments = @('--console=plain','--no-daemon','-PneoforgeOnly=true','-Penabled_platforms=neoforge')
            if ($Offline) { $arguments += '--offline' }
            & $runner @arguments @($build.Tasks)
            if ($LASTEXITCODE -ne 0) { throw "Build failed: $($build.Path)" }
        } finally { Pop-Location }
    }
} finally {
    if ($mappedDrive) { & subst.exe "$mappedDrive`:" /D }
}
