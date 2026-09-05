# Script to validate an ML dataset CSV file
param(
    [Parameter(Mandatory=$true)]
    [string]$File
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$mlDir = Join-Path $scriptDir "..\apps\ml-service"

Set-Location $mlDir

if (Test-Path ".\.venv\Scripts\python.exe") {
    & ".\.venv\Scripts\python.exe" -m app.ml.data.cli validate $File
} else {
    python -m app.ml.data.cli validate $File
}
