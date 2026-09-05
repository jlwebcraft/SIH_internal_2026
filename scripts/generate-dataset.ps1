# Script to generate synthetic procurement ML dataset
param(
    [int]$Samples = 3000,
    [int]$Seed = 42,
    [string]$OutputDir = "data"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$mlDir = Join-Path $scriptDir "..\apps\ml-service"

Set-Location $mlDir

if (Test-Path ".\.venv\Scripts\python.exe") {
    & ".\.venv\Scripts\python.exe" -m app.ml.data.cli generate --samples $Samples --seed $Seed --output-dir $OutputDir
} else {
    python -m app.ml.data.cli generate --samples $Samples --seed $Seed --output-dir $OutputDir
}
