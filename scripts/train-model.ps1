<#
.SYNOPSIS
    Trains and evaluates the Supply Chain Disruption Prediction ML model.
.DESCRIPTION
    Executes chronological train/val/test evaluation, compares baseline models (LogisticRegression & RandomForest),
    selects the optimal F1 threshold on validation data, and persists the serialized model artifact and metadata.
.PARAMETER Samples
    Number of observations (default: 3000).
.PARAMETER Seed
    Random seed for reproducibility (default: 42).
.PARAMETER OutputDir
    Directory to save model artifacts (default: apps/ml-service/models).
#>
param (
    [int]$Samples = 3000,
    [int]$Seed = 42,
    [string]$OutputDir = "models"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$MlServiceDir = Join-Path (Split-Path -Parent $ScriptDir) "apps\ml-service"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Training Supply Chain Disruption ML Model (Phase 7E)    " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$PythonExe = Join-Path $MlServiceDir ".venv\Scripts\python.exe"
if (-not (Test-Path $PythonExe)) {
    Write-Host "Virtual environment python not found at: $PythonExe" -ForegroundColor Red
    Write-Host "Please set up the virtual environment first." -ForegroundColor Yellow
    exit 1
}

Push-Location $MlServiceDir
try {
    & $PythonExe -m app.ml.training.train --samples $Samples --seed $Seed --output-dir $OutputDir
}
finally {
    Pop-Location
}
