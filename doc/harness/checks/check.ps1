#Requires -Version 7.0
[CmdletBinding()]
param(
    [ValidateSet('Static', 'Test', 'Build', 'Verify', 'Device')]
    [string]$Mode = 'Static',
    [string]$ProjectRoot = (Join-Path $PSScriptRoot '../../..')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath($ProjectRoot).TrimEnd('\', '/')
$failures = [Collections.Generic.List[string]]::new()
$usedExceptions = [Collections.Generic.HashSet[string]]::new()

function Fail([string]$message) { $failures.Add($message) }
function Relative([string]$path) { [IO.Path]::GetRelativePath($root, $path).Replace('\', '/') }
function MatchesAny([string]$value, $patterns) {
    foreach ($pattern in $patterns) {
        if ($value -clike $pattern) { return $true }
    }
    return $false
}
function SourceFiles([string]$path) {
    if (Test-Path -LiteralPath $path) {
        Get-ChildItem -LiteralPath $path -Recurse -File |
            Where-Object { $_.Extension -in '.kt', '.java' }
    }
}
function AssertFreshTests([string]$reports, [datetime]$startedAt) {
    $xmlFiles = @()
    if (Test-Path -LiteralPath $reports) {
        $xmlFiles = @(Get-ChildItem -LiteralPath $reports -Recurse -Filter 'TEST-*.xml' -File |
            Where-Object { $_.LastWriteTimeUtc -ge $startedAt.AddSeconds(-2) })
    }
    if ($xmlFiles.Count -eq 0) { throw "No fresh JUnit reports: $reports" }
    $tests = 0; $skipped = 0; $errors = 0
    foreach ($file in $xmlFiles) {
        [xml]$report = Get-Content -LiteralPath $file.FullName -Raw -Encoding utf8
        foreach ($suite in $report.SelectNodes('//testsuite')) {
            $tests += [int]$suite.GetAttribute('tests')
            $skipped += [int]$suite.GetAttribute('skipped')
            $errors += [int]$suite.GetAttribute('failures') + [int]$suite.GetAttribute('errors')
        }
    }
    if ($tests -le 0 -or $skipped -gt 0 -or $errors -gt 0) {
        throw "Tests must be nonempty, passing and not skipped: tests=$tests skipped=$skipped errors=$errors"
    }
    Write-Output "[PASS] Fresh JUnit results: $tests tests, 0 skipped, 0 failures/errors."
}
function RunGradle([string[]]$tasks) {
    $wrapper = if ($IsWindows) { Join-Path $root 'gradlew.bat' } else { Join-Path $root 'gradlew' }
    if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) { throw 'Gradle wrapper is missing.' }
    Push-Location $root
    try {
        & $wrapper @tasks --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
    } finally { Pop-Location }
}

try {
    $rulesPath = Join-Path $root 'doc/harness/checks/rules.json'
    $rules = Get-Content -LiteralPath $rulesPath -Raw -Encoding utf8 | ConvertFrom-Json
    if ($rules.version -ne 1 -or @($rules.packages).Count -eq 0) { throw 'Invalid boundary rules.' }
    foreach ($relative in $rules.requiredFiles) {
        if (-not (Test-Path -LiteralPath (Join-Path $root $relative) -PathType Leaf)) {
            Fail "Missing required file: $relative"
        }
    }

    # Only maintained docs are checked. Original reference snapshots keep their own links.
    $docs = @(Get-ChildItem -LiteralPath (Join-Path $root 'doc/harness') -Recurse -Filter '*.md' -File)
    $docs += Get-Item -LiteralPath (Join-Path $root 'doc/README.md')
    $docs += Get-Item -LiteralPath (Join-Path $root 'doc/reference/README.md')
    $agentPath = Join-Path $root 'AGENTS.md'
    if (Test-Path -LiteralPath $agentPath -PathType Leaf) {
        $docs += Get-Item -LiteralPath $agentPath
        if ([string]::IsNullOrWhiteSpace((Get-Content -LiteralPath $agentPath -Raw -Encoding utf8))) {
            Fail 'Empty agent entry: AGENTS.md'
        }
    }
    $linkCount = 0
    foreach ($doc in $docs) {
        $content = Get-Content -LiteralPath $doc.FullName -Raw -Encoding utf8
        foreach ($match in [regex]::Matches($content, '(?<!!)\[[^\]\r\n]*\]\((?<target>[^)\r\n]+)\)')) {
            $target = $match.Groups['target'].Value.Trim().Trim('<', '>')
            if ($target -match '^(?:[a-zA-Z][a-zA-Z0-9+.-]*:|#)') { continue }
            $target = [Uri]::UnescapeDataString(($target -split '#', 2)[0])
            if (-not $target) { continue }
            $resolved = [IO.Path]::GetFullPath((Join-Path $doc.DirectoryName $target))
            $inside = $resolved.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)
            if (-not $inside -or -not (Test-Path -LiteralPath $resolved)) {
                Fail "Broken or outside-project link: $(Relative $doc.FullName) -> $target"
            }
            $linkCount++
        }
    }

    $settings = Get-Content -LiteralPath (Join-Path $root 'settings.gradle.kts') -Raw -Encoding utf8
    $modules = [Collections.Generic.List[string]]::new()
    foreach ($call in [regex]::Matches($settings, '(?m)^\s*include\s*\(([^\r\n]*)\)')) {
        $arguments = $call.Groups[1].Value
        if ($arguments -notmatch '^\s*"[^"\r\n]+"(?:\s*,\s*"[^"\r\n]+")*\s*$') {
            Fail 'Dynamic module includes require an explicit checker update.'
        }
        foreach ($item in [regex]::Matches($arguments, '"([^"]+)"')) { $modules.Add($item.Groups[1].Value) }
    }
    if (($modules | Sort-Object) -join ',' -cne (($rules.modules | Sort-Object) -join ',')) {
        Fail 'Gradle modules differ from registered modules.'
    }
    foreach ($set in Get-ChildItem -LiteralPath (Join-Path $root 'app/src') -Directory) {
        if ($set.Name -cnotin $rules.sourceSets) { Fail "Unregistered source set: $($set.Name)" }
    }

    $sourceFiles = @(SourceFiles (Join-Path $root 'app/src/main'))
    if ($sourceFiles.Count -eq 0) { Fail 'No production sources found.' }
    # Lightweight lexical scan, not a compiler. See ARCHITECTURE.md for limitations.
    $nonCodePattern = '(?s)/\*.*?\*/|//[^\r\n]*|""".*?"""|"(?:\\.|[^"\\])*"|''(?:\\.|[^''\\])*'''
    foreach ($file in $sourceFiles) {
        $relative = Relative $file.FullName
        $registeredRoot = $false
        foreach ($sourceRoot in $rules.sourceRoots) {
            if ($relative.StartsWith($sourceRoot + '/', [StringComparison]::Ordinal)) { $registeredRoot = $true }
        }
        if (-not $registeredRoot) { Fail "Unregistered source root: $relative" }
        if ($file.Extension -cne '.kt') { Fail "Unregistered language: $relative"; continue }
        $code = [regex]::Replace((Get-Content -LiteralPath $file.FullName -Raw -Encoding utf8), $nonCodePattern, ' ')
        $packageMatch = [regex]::Match($code, '(?m)^\s*package\s+([\w.]+)')
        if (-not $packageMatch.Success) { Fail "Missing Kotlin package: $relative"; continue }
        $packageName = $packageMatch.Groups[1].Value
        $matching = @($rules.packages | Where-Object {
            $packageName -ceq $_.name -or ($_.recursive -and $packageName.StartsWith($_.name + '.', [StringComparison]::Ordinal))
        } | Sort-Object { $_.name.Length } -Descending)
        if ($matching.Count -eq 0) { Fail "Unregistered package: $packageName ($relative)"; continue }
        $rule = $matching[0]
        $code = [regex]::Replace($code, '(?m)^\s*package\s+[\w.]+', '')
        $references = @([regex]::Matches($code, '\b(?:com\.todoink\.app|android|androidx)(?:\.[A-Za-z_][A-Za-z0-9_]*)+(?:\.\*)?') |
            ForEach-Object { $_.Value } | Sort-Object -Unique)
        foreach ($reference in $references) {
            if (MatchesAny $reference $rule.forbid) { Fail "Forbidden platform reference: $relative -> $reference"; continue }
            if (-not $reference.StartsWith('com.todoink.app.', [StringComparison]::Ordinal)) { continue }
            if (MatchesAny $reference $rule.allow) { continue }
            $exception = @($rules.exceptions | Where-Object { $_.file -ceq $relative -and $_.reference -ceq $reference })
            if ($exception.Count -eq 1 -and -not [string]::IsNullOrWhiteSpace($exception[0].reason)) {
                [void]$usedExceptions.Add("$relative|$reference")
            } else { Fail "Forbidden dependency: $relative -> $reference" }
        }
    }
    foreach ($exception in $rules.exceptions) {
        if (-not $usedExceptions.Contains("$($exception.file)|$($exception.reference)")) {
            Fail "Stale exception: $($exception.file) -> $($exception.reference)"
        }
    }

    $schemas = @(Get-ChildItem -LiteralPath (Join-Path $root 'app/schemas') -Recurse -Filter '*.json' -File)
    if ($schemas.Count -eq 0) { Fail 'No exported Room schema.' }
    foreach ($schemaFile in $schemas) {
        try {
            $schema = Get-Content -LiteralPath $schemaFile.FullName -Raw -Encoding utf8 | ConvertFrom-Json
            if ($schema.database.version -lt 1 -or @($schema.database.entities).Count -eq 0) { throw 'Invalid schema structure.' }
        } catch { Fail "Invalid Room schema: $(Relative $schemaFile.FullName)" }
    }

    if ($failures.Count -gt 0) {
        foreach ($failure in $failures) { Write-Output "[FAIL] $failure" }
        exit 1
    }
    Write-Output "[PASS] Static: $($docs.Count) docs, $linkCount local links, $($sourceFiles.Count) Kotlin files, $($schemas.Count) Room schemas."
    Write-Output "[INFO] Exact legacy exceptions used: $($usedExceptions.Count). Static checks do not prove behavior."

    $unitSources = @(SourceFiles (Join-Path $root 'app/src/test'))
    $deviceSources = @(SourceFiles (Join-Path $root 'app/src/androidTest'))
    if ($unitSources.Count -eq 0) { Write-Output '[INFO] No unit test sources. Test/Verify cannot pass yet.' }
    if ($Mode -in 'Test', 'Verify') {
        if ($unitSources.Count -eq 0) { throw 'No unit test sources; refusing NO-SOURCE success.' }
        $startedAt = [datetime]::UtcNow
        RunGradle @(':app:testDebugUnitTest', '--rerun-tasks')
        AssertFreshTests (Join-Path $root 'app/build/test-results/testDebugUnitTest') $startedAt
    }
    if ($Mode -eq 'Build') { RunGradle @(':app:assembleDebug') }
    if ($Mode -eq 'Verify') { RunGradle @(':app:lintDebug', ':app:assembleDebug') }
    if ($Mode -eq 'Device') {
        if ($deviceSources.Count -eq 0) { throw 'No instrumentation test sources; refusing NO-SOURCE success.' }
        $startedAt = [datetime]::UtcNow
        RunGradle @(':app:connectedDebugAndroidTest', '--rerun-tasks')
        AssertFreshTests (Join-Path $root 'app/build/outputs/androidTest-results/connected') $startedAt
    }
    Write-Output "[PASS] Mode=$Mode. Only the checks listed above were executed."
    exit 0
} catch {
    Write-Output "[FAIL] $($_.Exception.Message)"
    exit 1
}
