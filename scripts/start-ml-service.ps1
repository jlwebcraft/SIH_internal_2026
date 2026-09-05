# Script to start the Python ML Service development server on Windows
param(
    [int]$Port = 8000,
    [string]$Host = "0.0.0.0"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$mlDir = Join-Path $scriptDir "..\apps\ml-service"

Write-Host "Starting ML Service from $mlDir on port $Port..."
Set-Location $mlDir

if (Test-Path ".\.venv\Scripts\Activate.ps1") {
    Write-Host "Activating virtual environment..."
    & ".\.venv\Scripts\Activate.ps1"
}

python -m uvicorn app.main:app --host $Host --port $Port --reload
