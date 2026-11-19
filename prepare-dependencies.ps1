param([Parameter(Mandatory=$true)][string]$ModsFolder)
$ErrorActionPreference = 'Stop'
$patterns = @('citadel-2.6.1-1.20.1.jar','create-1.20.1-6.0.8.jar','FarmersDelight-1.20.1-1.3.4.jar','Jade-1.20.1-Forge-11.12.2.jar','*jei-1.20.1-forge-15.58.0.209.jar')
for($i=0;$i -lt $patterns.Count;$i++) {
  $matches = @(Get-ChildItem -LiteralPath $ModsFolder -File | Where-Object { $_.Name -like $patterns[$i] })
  if($matches.Count -ne 1){throw "Expected one dependency matching $($patterns[$i])"}
  $dest=Join-Path $PSScriptRoot "local-maven/local/rats/dep$i/1"
  New-Item -ItemType Directory -Path $dest -Force | Out-Null
  Copy-Item -LiteralPath $matches[0].FullName -Destination (Join-Path $dest "dep$i-1.jar")
  $pom="<project><modelVersion>4.0.0</modelVersion><groupId>local.rats</groupId><artifactId>dep$i</artifactId><version>1</version></project>"
  [IO.File]::WriteAllText((Join-Path $dest "dep$i-1.pom"),$pom)
}
$apiDir = Join-Path $PSScriptRoot 'libs/spell-api'
$rsiDir = Join-Path $PSScriptRoot 'libs/rsi'
$createDir = Join-Path $PSScriptRoot 'libs/create-api'
New-Item -ItemType Directory -Force $apiDir,$rsiDir,$createDir | Out-Null
foreach ($pattern in @('goety-2.*.jar','irons_spellbooks-1.20*.jar','curios-forge*.jar','*黄昏-*.jar','rs_integration-1.6.0.jar')) {
    $dep = @(Get-ChildItem -LiteralPath $ModsFolder -File | Where-Object { $_.Name -like $pattern })
    if ($dep.Count -ne 1) { throw "Expected one dependency matching $pattern" }
    $dest = if ($pattern -like 'rs_integration*') { $rsiDir } else { $apiDir }
    if ($pattern -like '*黄昏*') { $dest = Join-Path $apiDir 'goety-iron-twilight-api.jar' }
    Copy-Item -LiteralPath $dep[0].FullName -Destination $dest -Force
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$create = [IO.Compression.ZipFile]::OpenRead((Get-Item -LiteralPath (Join-Path $ModsFolder 'create-1.20.1-6.0.8.jar')).FullName)
try {
    foreach ($entry in $create.Entries) {
        if ($entry.FullName -like 'META-INF/jarjar/*.jar') {
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,(Join-Path $createDir $entry.Name),$true)
        }
    }
} finally { $create.Dispose() }
Write-Output 'Dependencies prepared. Build with JDK 17 and .\gradlew.bat build.'
