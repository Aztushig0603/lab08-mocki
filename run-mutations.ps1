$ErrorActionPreference = 'Stop'
$source = 'src/main/java/mn/edu/must/sqat/CourseRegistrationService.java'
$results = 'results'
New-Item -ItemType Directory -Force -Path $results | Out-Null
$original = Get-Content -Raw -Encoding UTF8 $source
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Write-Utf8NoBom($path, $content) {
    [System.IO.File]::WriteAllText($path, $content, $script:utf8NoBom)
}

function Run-MvnTest($logPath) {
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        mvn test 2>&1 | Tee-Object -FilePath $logPath
    }
    finally {
        $ErrorActionPreference = $prev
    }
}

function Run-Mutation($number, $old, $replacement) {
    if (-not $script:original.Contains($old)) {
        throw "Mutant ${number}: target code was not found; source may have changed."
    }
    $mutated = $script:original.Replace($old, $replacement)
    Write-Utf8NoBom $script:source $mutated
    try {
        Run-MvnTest "$script:results/mvn-test-mutant-${number}.txt"
    }
    finally {
        Write-Utf8NoBom $script:source $script:original
    }
}

try {
    Run-Mutation 1 '        if (!student.active()) return "ERROR_INACTIVE_STUDENT";' '        // MUTANT 1: inactive-student check intentionally removed.'
    Run-Mutation 2 '        repository.saveRegistration(studentId, courseId);' '        // MUTANT 2: saveRegistration intentionally removed.'
    Run-Mutation 3 '        if (studentId == null || courseId == null) {' '        if (studentId == null) {'
}
finally {
    Write-Utf8NoBom $source $original
}

Run-MvnTest "$results/mvn-test.txt"
Write-Host "Mutation runs complete. Check all four files in $results and confirm each mutant log contains BUILD FAILURE."

$content = Get-Content results\mvn-test.txt -Raw
[System.IO.File]::WriteAllText("$PWD\results\mvn-test.txt", $content, (New-Object System.Text.UTF8Encoding($false)))