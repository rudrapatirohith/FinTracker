[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string[]] $ExportFiles,

    [string] $OutputFile = (Join-Path $PSScriptRoot "private\financial-staging.sql")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$allowedTables = @(
    "profiles",
    "income",
    "monthly_expenses",
    "loans",
    "loan_payments",
    "international_transfers",
    "scheduled_payments",
    "transactions"
)

$builder = [System.Text.StringBuilder]::new()
$totalInserts = 0

foreach ($file in $ExportFiles) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        throw "Export file not found: $file"
    }

    $content = [System.IO.File]::ReadAllText((Resolve-Path -LiteralPath $file).Path)
    $matches = [regex]::Matches(
        $content,
        '(?im)^[ \t]*INSERT[ \t]+INTO[ \t]+public\.([a-z_][a-z0-9_]*)'
    )

    foreach ($match in $matches) {
        $table = $match.Groups[1].Value.ToLowerInvariant()
        if ($allowedTables -notcontains $table) {
            throw "Unexpected source table '$table' in $file. No output was written."
        }
    }

    $totalInserts += $matches.Count
    $rewritten = [regex]::Replace(
        $content,
        '(?im)^([ \t]*INSERT[ \t]+INTO[ \t]+)public\.',
        '$1migration_source.'
    )

    [void] $builder.AppendLine($rewritten)
}

if ($totalInserts -eq 0) {
    throw "No INSERT INTO public.<table> statements were found. No output was written."
}

$outputDirectory = Split-Path -Parent $OutputFile
if (-not (Test-Path -LiteralPath $outputDirectory -PathType Container)) {
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
}

$output = $builder.ToString()
if ($output -match '(?im)^[ \t]*INSERT[ \t]+INTO[ \t]+public\.') {
    throw "A public INSERT prefix remains after transformation. No output was written."
}

[System.IO.File]::WriteAllText(
    [System.IO.Path]::GetFullPath($OutputFile),
    $output,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Prepared staging SQL with $totalInserts INSERT statement(s)."
Write-Host "Output: $([System.IO.Path]::GetFullPath($OutputFile))"
Write-Host "Source export files were read only and were not modified."
