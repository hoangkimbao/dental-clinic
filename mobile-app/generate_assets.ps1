# ========================================================
# DentalCare Luxury Mobile PNG Asset Generator (PowerShell GDI+)
# Generates authentic binary PNG files with 8-byte PNG magic:
# 0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A (\x89PNG\r\n\x1a\n)
# ========================================================

Add-Type -AssemblyName System.Drawing

function Generate-DentalPng($filePath, $width, $height, $isFavicon) {
    $bmp = New-Object System.Drawing.Bitmap $width, $height
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic

    # Premium DentalCare Emerald / Teal Gradient (#042f2e -> #0d9488)
    $rect = New-Object System.Drawing.Rectangle 0, 0, $width, $height
    $cTop = [System.Drawing.Color]::FromArgb(255, 4, 47, 46)        # #042f2e
    $cBottom = [System.Drawing.Color]::FromArgb(255, 13, 148, 136)   # #0d9488
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush $rect, $cTop, $cBottom, 60.0
    $g.FillRectangle($brush, $rect)

    # Stylized Tooth Emblem (White & Gold)
    $cx = [int]($width / 2)
    $cy = [int]($height / 2)
    $scale = [Math]::Min($width, $height) / 512.0

    if (-not $isFavicon) {
        # Outer Glowing Circle
        $glowColor = [System.Drawing.Color]::FromArgb(60, 45, 212, 191)
        $glowBrush = New-Object System.Drawing.SolidBrush $glowColor
        $g.FillEllipse($glowBrush, [int]($cx - 180 * $scale), [int]($cy - 180 * $scale), [int](360 * $scale), [int](360 * $scale))
        $glowBrush.Dispose()

        # Tooth Crown
        $toothBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
        $g.FillEllipse($toothBrush, [int]($cx - 110 * $scale), [int]($cy - 130 * $scale), [int](120 * $scale), [int](130 * $scale))
        $g.FillEllipse($toothBrush, [int]($cx - 10 * $scale), [int]($cy - 130 * $scale), [int](120 * $scale), [int](130 * $scale))
        $g.FillRectangle($toothBrush, [int]($cx - 100 * $scale), [int]($cy - 70 * $scale), [int](200 * $scale), [int](100 * $scale))
        
        # Tooth Roots
        $g.FillEllipse($toothBrush, [int]($cx - 95 * $scale), [int]($cy + 20 * $scale), [int](70 * $scale), [int](110 * $scale))
        $g.FillEllipse($toothBrush, [int]($cx + 25 * $scale), [int]($cy + 20 * $scale), [int](70 * $scale), [int](110 * $scale))
        $toothBrush.Dispose()

        # Golden Luxury Cross Emblem (#f59e0b)
        $goldBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 245, 158, 11))
        $g.FillRectangle($goldBrush, [int]($cx + 20 * $scale), [int]($cy - 60 * $scale), [int](60 * $scale), [int](20 * $scale))
        $g.FillRectangle($goldBrush, [int]($cx + 40 * $scale), [int]($cy - 80 * $scale), [int](20 * $scale), [int](60 * $scale))
        $goldBrush.Dispose()
    } else {
        # Favicon simplified tooth
        $toothBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
        $g.FillEllipse($toothBrush, 8, 8, 48, 48)
        $toothBrush.Dispose()
        $crossBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 13, 148, 136))
        $g.FillRectangle($crossBrush, 24, 16, 16, 32)
        $g.FillRectangle($crossBrush, 16, 24, 32, 16)
        $crossBrush.Dispose()
    }

    # Ensure parent directory exists
    $parentDir = [System.IO.Path]::GetDirectoryName($filePath)
    if (-not [System.IO.Directory]::Exists($parentDir)) {
        [System.IO.Directory]::CreateDirectory($parentDir) | Out-Null
    }

    # Save as authentic PNG
    $bmp.Save($filePath, [System.Drawing.Imaging.ImageFormat]::Png)
    $brush.Dispose()
    $g.Dispose()
    $bmp.Dispose()

    # Read back first 8 bytes for signature verification
    $bytes = [System.IO.File]::ReadAllBytes($filePath)
    $magicHex = ($bytes[0..7] | ForEach-Object { $_.ToString("X2") }) -join " "
    $expectedHex = "89 50 4E 47 0D 0A 1A 0A"
    $isValid = ($magicHex -eq $expectedHex)

    Write-Host "[OK] Saved: $filePath ($width x $height, $($bytes.Length) bytes) | Magic: $magicHex | Valid: $isValid"
}

# Resolve target directory
$baseDir = if ($PSScriptRoot) { $PSScriptRoot } else { "d:\java\dental-clinic\mobile-app" }
$assetsDir = Join-Path $baseDir "assets"

Write-Host "========================================================"
Write-Host "Generating True Binary PNG Assets in: $assetsDir"
Write-Host "========================================================"

Generate-DentalPng (Join-Path $assetsDir "icon.png") 1024 1024 $false
Generate-DentalPng (Join-Path $assetsDir "adaptive-icon.png") 1024 1024 $false
Generate-DentalPng (Join-Path $assetsDir "splash.png") 1242 2436 $false
Generate-DentalPng (Join-Path $assetsDir "favicon.png") 64 64 $true

Write-Host ""
Write-Host "All 4 authentic binary PNG files generated and verified."
