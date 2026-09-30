#Requires -Version 7.0
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$project = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$tempBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\', '/')
$fixture = Join-Path $tempBase ('todoink-harness-' + [guid]::NewGuid().ToString('N'))
$pwsh = (Get-Process -Id $PID).Path
$passed = 0

function CheckResult([string]$name, [int]$expected, [string]$contains, [string]$mode = 'Static') {
    $output = (& $pwsh -NoProfile -File (Join-Path $fixture 'doc/harness/checks/check.ps1') -ProjectRoot $fixture -Mode $mode 2>&1) -join "`n"
    if ($LASTEXITCODE -ne $expected -or -not $output.Contains($contains)) {
        throw "$name failed: expected exit=$expected and '$contains'. Actual output:`n$output"
    }
    $script:passed++
    Write-Output "[PASS] $name"
}
function WithFile([string]$relative, [string]$content, [scriptblock]$test) {
    $path = Join-Path $fixture $relative
    $existed = Test-Path -LiteralPath $path -PathType Leaf
    $before = if ($existed) { [IO.File]::ReadAllBytes($path) } else { $null }
    New-Item -ItemType Directory -Path (Split-Path -Parent $path) -Force | Out-Null
    try {
        [IO.File]::WriteAllText($path, $content, [Text.UTF8Encoding]::new($false))
        & $test
    } finally {
        if ($existed) { [IO.File]::WriteAllBytes($path, $before) }
        else { Remove-Item -LiteralPath $path -Force }
    }
}

try {
    New-Item -ItemType Directory -Path $fixture | Out-Null
    foreach ($item in @('doc', 'gradle', 'AGENTS.md', 'README.md', 'settings.gradle.kts', 'build.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat')) {
        Copy-Item -LiteralPath (Join-Path $project $item) -Destination $fixture -Recurse
    }
    New-Item -ItemType Directory -Path (Join-Path $fixture 'app') | Out-Null
    foreach ($item in @('src', 'schemas', 'build.gradle.kts', 'proguard-rules.pro')) {
        Copy-Item -LiteralPath (Join-Path $project "app/$item") -Destination (Join-Path $fixture 'app') -Recurse
    }
    CheckResult 'current baseline' 0 '[PASS] Mode=Static'
    $agentPath = Join-Path $fixture 'AGENTS.md'
    $agentBytes = [IO.File]::ReadAllBytes($agentPath)
    try {
        Remove-Item -LiteralPath $agentPath
        CheckResult 'missing root agent entry' 1 'Missing required file: AGENTS.md'
    } finally { [IO.File]::WriteAllBytes($agentPath, $agentBytes) }
    WithFile 'AGENTS.md' " `n" {
        CheckResult 'empty root agent entry' 1 'Empty agent entry: AGENTS.md'
    }
    WithFile 'AGENTS.md' '[missing](doc/harness/does-not-exist.md)' {
        CheckResult 'broken root agent link' 1 'Broken or outside-project link: AGENTS.md'
    }
    WithFile 'app/src/main/java/com/todoink/app/data/db/BoundaryProbe.kt' "package com.todoink.app.data.db`nimport com.todoink.app.ui.TodoInkApp" {
        CheckResult 'reverse UI dependency' 1 'Forbidden dependency'
    }
    WithFile 'app/src/main/java/com/todoink/app/data/db/BoundaryProbe.kt' "package com.todoink.app.data.db`nval x = com.todoink.app.ui.TodoInkApp" {
        CheckResult 'fully qualified dependency' 1 'Forbidden dependency'
    }
    WithFile 'app/src/main/java/com/todoink/app/data/db/BoundaryProbe.kt' "package com.todoink.app.data.db`nimport com.todoink.app.ui.*" {
        CheckResult 'wildcard dependency' 1 'Forbidden dependency'
    }
    WithFile 'app/src/main/java/com/todoink/app/notification/model/BoundaryProbe.kt' "package com.todoink.app.notification.model`nimport android.os.Bundle" {
        CheckResult 'Android type in model' 1 'Forbidden platform reference'
    }
    WithFile 'app/src/main/java/com/todoink/app/unknown/BoundaryProbe.kt' 'package com.todoink.app.unknown' {
        CheckResult 'unknown package' 1 'Unregistered package'
    }
    WithFile 'app/src/main/java/com/todoink/app/data/db/BoundaryProbe.java' 'package com.todoink.app.data.db;' {
        CheckResult 'unknown language' 1 'Unregistered language'
    }
    WithFile 'app/src/debug/java/BoundaryProbe.kt' 'package com.todoink.app' {
        CheckResult 'unknown source set' 1 'Unregistered source set'
    }
    # The empty debug directory also represents an unregistered source set. Remove only this fixture path.
    $debugPath = [IO.Path]::GetFullPath((Join-Path $fixture 'app/src/debug'))
    if (-not $debugPath.StartsWith($fixture + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe fixture path.' }
    Remove-Item -LiteralPath $debugPath -Recurse -Force
    $settings = Get-Content -LiteralPath (Join-Path $fixture 'settings.gradle.kts') -Raw
    WithFile 'settings.gradle.kts' ($settings + "`ninclude(`":other`" )`n") {
        CheckResult 'unknown module' 1 'Gradle modules differ'
    }
    WithFile 'doc/harness/probe.md' '[missing](does-not-exist.md)' {
        CheckResult 'broken documentation link' 1 'Broken or outside-project link'
    }
    WithFile 'app/schemas/probe.json' '{invalid' {
        CheckResult 'invalid Room schema' 1 'Invalid Room schema'
    }
    $uiPath = 'app/src/main/java/com/todoink/app/ui/status/StatusViewModel.kt'
    $uiContent = Get-Content -LiteralPath (Join-Path $fixture $uiPath) -Raw
    WithFile $uiPath ($uiContent.Replace('import com.todoink.app.data.db.NotificationSnapshotDao', '')) {
        CheckResult 'stale exact exception' 1 'Stale exception'
    }
    WithFile 'app/src/main/java/com/todoink/app/data/db/BoundaryProbe.kt' "package com.todoink.app.data.db`n// import com.todoink.app.ui.TodoInkApp`nval sample = `"com.todoink.app.ui.TodoInkApp`"" {
        CheckResult 'comments and strings are not dependencies' 0 '[PASS] Mode=Static'
    }
    if (-not (Test-Path -LiteralPath (Join-Path $fixture 'app/src/test'))) {
        CheckResult 'absent unit tests cannot pass' 1 'No unit test sources; refusing NO-SOURCE success.' 'Test'
    } else { Write-Output '[INFO] Test sources now exist; absent-test baseline probe is not applicable.' }
    CheckResult 'restored baseline' 0 '[PASS] Mode=Static'
    Write-Output "[PASS] $passed checker scenarios. No Android tests or Gradle builds were run."
} finally {
    $resolved = [IO.Path]::GetFullPath($fixture)
    $expectedParent = [IO.Path]::GetDirectoryName($resolved).TrimEnd('\', '/')
    if ($expectedParent -cne $tempBase -or -not ([IO.Path]::GetFileName($resolved).StartsWith('todoink-harness-'))) {
        throw 'Refusing cleanup outside the owned temporary fixture.'
    }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
}
