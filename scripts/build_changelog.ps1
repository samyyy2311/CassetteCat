# PowerShell version of build_changelog to run locally on Windows without Python
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$changelogPath = Join-Path $repoRoot "CHANGELOG.md"
$htmlPath = Join-Path $repoRoot "landing\changelog\index.html"

if (-not (Test-Path $changelogPath)) { throw "CHANGELOG.md not found" }
if (-not (Test-Path $htmlPath)) { throw "$htmlPath not found" }

$mdContent = [System.IO.File]::ReadAllText($changelogPath, [System.Text.Encoding]::UTF8)

function Format-MdText($text) {
    $escaped = [System.Net.WebUtility]::HtmlEncode($text)
    # Inline code
    $escaped = [regex]::Replace($escaped, '`([^`]+)`', '<code>$1</code>')
    # Bold
    $escaped = [regex]::Replace($escaped, '\*\*([^*]+)\*\*', '<strong>$1</strong>')
    # Italic
    $escaped = [regex]::Replace($escaped, '\*([^*]+)\*', '<em>$1</em>')
    # Links
    $escaped = [regex]::Replace($escaped, '\[([^\]]+)\]\(([^)]+)\)', '<a href="$2">$1</a>')
    # Quotes
    $escaped = $escaped.Replace('"', "&quot;")
    return $escaped
}

$versions = [System.Collections.Generic.List[PSObject]]::new()
$currentVer = $null
$currentCat = $null

$lines = $mdContent -split "\r?\n"
foreach ($line in $lines) {
    $lineClean = $line.Trim()
    if ($lineClean -match '^##\s+\[([^\]]+)\](?:\s*-\s*(.*))?$') {
        $vNum = $Matches[1].Trim()
        $vTitle = if ($Matches[2]) { $Matches[2].Trim() } else { "" }
        $currentVer = [PSCustomObject]@{
            Version = $vNum
            Title = $vTitle
            Sections = [System.Collections.Generic.List[PSObject]]::new()
        }
        $versions.Add($currentVer)
        $currentCat = $null
        continue
    }

    if ($null -eq $currentVer) { continue }

    if ($lineClean -match '^###\s+(.+)$') {
        $currentCat = $Matches[1].Trim()
        continue
    }

    if ($lineClean -match '^[*-]\s+(.+)$') {
        $itemText = $Matches[1].Trim()
        $sectionObj = $null
        if ($currentVer.Sections.Count -gt 0 -and $currentVer.Sections[$currentVer.Sections.Count - 1].Category -eq $currentCat) {
            $sectionObj = $currentVer.Sections[$currentVer.Sections.Count - 1]
        } else {
            $sectionObj = [PSCustomObject]@{
                Category = $currentCat
                Items = [System.Collections.Generic.List[string]]::new()
            }
            $currentVer.Sections.Add($sectionObj)
        }
        $sectionObj.Items.Add($itemText)
    }
}

if ($versions.Count -eq 0) { throw "No versions found in CHANGELOG.md" }

$latestVersion = $versions[0].Version

# Generate TOC
$tocLines = [System.Collections.Generic.List[string]]::new()
for ($i = 0; $i -lt $versions.Count; $i++) {
    $idx = $i + 1
    $vNum = $versions[$i].Version
    $secId = "v" + $vNum.Replace('.', '-')
    $numStr = "{0:D2}" -f $idx
    $tocLines.Add("          <li><a href=""#$secId""><span class=""toc-num"">$numStr</span>v$vNum</a></li>")
}
$tocHtml = $tocLines -join "`n"

# Generate Sections
$bodySections = [System.Collections.Generic.List[string]]::new()
for ($i = 0; $i -lt $versions.Count; $i++) {
    $idx = $i + 1
    $vNum = $versions[$i].Version
    $secId = "v" + $vNum.Replace('.', '-')
    $numStr = "{0:D2}" -f $idx
    $titleSuffix = if ($versions[$i].Title) { " - " + (Format-MdText $versions[$i].Title) } else { "" }

    $sLines = [System.Collections.Generic.List[string]]::new()
    $sLines.Add("        <section id=""$secId"">")
    $sLines.Add("          <h2><span class=""sec-num"">$numStr.</span>v$vNum$titleSuffix</h2>")

    foreach ($sec in $versions[$i].Sections) {
        if ($sec.Category) {
            $sLines.Add("          <p><strong>" + (Format-MdText $sec.Category) + "</strong></p>")
        }
        $sLines.Add("          <ul>")
        foreach ($it in $sec.Items) {
            $sLines.Add("            <li>" + (Format-MdText $it) + "</li>")
        }
        $sLines.Add("          </ul>")
    }
    $sLines.Add("        </section>")
    $bodySections.Add(($sLines -join "`n"))
}
$sectionsHtml = $bodySections -join "`n`n"

$htmlContent = [System.IO.File]::ReadAllText($htmlPath, [System.Text.Encoding]::UTF8)

# 1. Update Latest
$htmlContent = [regex]::Replace($htmlContent, '(<span><b>Latest</b>\s*)v[^<]*(</span>)', "`${1}v$latestVersion`${2}")

# 2. Update TOC
$tocRegex = [regex]'(?s)(<aside class="doc-toc">\s*<h4>Versions</h4>\s*<ol>).*?(</ol>\s*</aside>)'
$htmlContent = $tocRegex.Replace($htmlContent, "`${1}`n$tocHtml`n        `${2}")

# 3. Update Body sections
$bodyRegex = [regex]'(?s)(<div class="doc-body">\s*<p class="doc-lede">.*?</p>\s*)<section id=.*?</section>(\s*</div>\s*</div>\s*</main>)'
$htmlContent = $bodyRegex.Replace($htmlContent, "`${1}`n$sectionsHtml`n`n      `${2}")

[System.IO.File]::WriteAllText($htmlPath, $htmlContent, [System.Text.Encoding]::UTF8)
Write-Output "Successfully updated $htmlPath for version $latestVersion ($($versions.Count) versions)"
