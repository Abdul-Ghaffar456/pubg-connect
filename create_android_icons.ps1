Add-Type -AssemblyName System.Drawing

function Create-AndroidGamepadBitmap([int]$size, [bool]$isCircle = $false) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    if ($isCircle) {
        $path.AddEllipse(1.0, 1.0, [float]($size - 2), [float]($size - 2))
    } else {
        # Squircle / Rounded rectangle
        $radius = [float]($size * 0.22)
        $d = $radius * 2
        $w = [float]($size - 2)
        $h = [float]($size - 2)
        $path.AddArc(1.0, 1.0, $d, $d, 180.0, 90.0)
        $path.AddArc(1.0 + $w - $d, 1.0, $d, $d, 270.0, 90.0)
        $path.AddArc(1.0 + $w - $d, 1.0 + $h - $d, $d, $d, 0.0, 90.0)
        $path.AddArc(1.0, 1.0 + $h - $d, $d, $d, 90.0, 90.0)
        $path.CloseFigure()
    }

    # Fill background with dark theme (#0B0F19)
    $bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 11, 15, 25))
    $g.FillPath($bgBrush, $path)

    # Border with emerald green (#10B981)
    $borderPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 16, 185, 129), [float]($size * 0.045))
    $g.DrawPath($borderPen, $path)

    # Draw Gamepad Pill Body (Neon Emerald #10B981)
    $gpWidth = [float]($size * 0.68)
    $gpHeight = [float]($size * 0.42)
    $gpX = [float](($size - $gpWidth) / 2.0)
    $gpY = [float](($size - $gpHeight) / 2.0)
    $gpd = [float]($gpHeight * 0.92)

    $gpPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $gpPath.AddArc($gpX, $gpY, $gpd, $gpd, 180.0, 90.0)
    $gpPath.AddArc($gpX + $gpWidth - $gpd, $gpY, $gpd, $gpd, 270.0, 90.0)
    $gpPath.AddArc($gpX + $gpWidth - $gpd, $gpY + $gpHeight - $gpd, $gpd, $gpd, 0.0, 90.0)
    $gpPath.AddArc($gpX, $gpY + $gpHeight - $gpd, $gpd, $gpd, 90.0, 90.0)
    $gpPath.CloseFigure()

    # Fill gamepad with neon emerald
    $gpBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 16, 185, 129))
    $g.FillPath($gpBrush, $gpPath)

    # D-Pad (Left Side Black Cross)
    $darkBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 11, 15, 25))
    $dpadCenterX = $gpX + ($gpWidth * 0.28)
    $dpadCenterY = $gpY + ($gpHeight * 0.50)
    $dpadArm = $size * 0.052
    $dpadThick = $size * 0.036

    # Horizontal bar
    $g.FillRectangle($darkBrush, [float]($dpadCenterX - $dpadArm), [float]($dpadCenterY - ($dpadThick / 2)), [float]($dpadArm * 2), [float]$dpadThick)
    # Vertical bar
    $g.FillRectangle($darkBrush, [float]($dpadCenterX - ($dpadThick / 2)), [float]($dpadCenterY - $dpadArm), [float]$dpadThick, [float]($dpadArm * 2))

    # Action Buttons (Right Side 4 Dots)
    $btnCenterX = $gpX + ($gpWidth * 0.72)
    $btnCenterY = $gpY + ($gpHeight * 0.50)
    $btnOffset = $size * 0.042
    $btnSize = $size * 0.034

    # North, South, East, West buttons
    $g.FillEllipse($darkBrush, [float]($btnCenterX - ($btnSize / 2)), [float]($btnCenterY - $btnOffset - ($btnSize / 2)), [float]$btnSize, [float]$btnSize)
    $g.FillEllipse($darkBrush, [float]($btnCenterX - ($btnSize / 2)), [float]($btnCenterY + $btnOffset - ($btnSize / 2)), [float]$btnSize, [float]$btnSize)
    $g.FillEllipse($darkBrush, [float]($btnCenterX + $btnOffset - ($btnSize / 2)), [float]($btnCenterY - ($btnSize / 2)), [float]$btnSize, [float]$btnSize)
    $g.FillEllipse($darkBrush, [float]($btnCenterX - $btnOffset - ($btnSize / 2)), [float]($btnCenterY - ($btnSize / 2)), [float]$btnSize, [float]$btnSize)

    # Middle Center Indicator Dot
    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 240, 253, 244))
    $dotSize = $size * 0.028
    $g.FillEllipse($whiteBrush, [float]($gpX + ($gpWidth * 0.50) - ($dotSize / 2)), [float]($gpY + ($gpHeight * 0.50) - ($dotSize / 2)), [float]$dotSize, [float]$dotSize)

    $g.Dispose()
    return $bmp
}

# Android Mipmap densities and pixel sizes
$mipmapSizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

$resDir = "src\PubgConnect.Android\app\src\main\res"

foreach ($entry in $mipmapSizes.GetEnumerator()) {
    $folder = Join-Path $resDir $entry.Key
    if (-not (Test-Path $folder)) { New-Item -ItemType Directory -Path $folder | Out-Null }

    # 1. Square / Rounded ic_launcher.png
    $bmpSquare = Create-AndroidGamepadBitmap -size $entry.Value -isCircle $false
    $squarePath = Join-Path $folder "ic_launcher.png"
    $bmpSquare.Save($squarePath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmpSquare.Dispose()

    # 2. Circle ic_launcher_round.png
    $bmpRound = Create-AndroidGamepadBitmap -size $entry.Value -isCircle $true
    $roundPath = Join-Path $folder "ic_launcher_round.png"
    $bmpRound.Save($roundPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmpRound.Dispose()

    Write-Host "Generated $($entry.Key) ($($entry.Value)x$($entry.Value)) icons" -ForegroundColor Green
}

Write-Host "Android app launcher icons generated successfully!" -ForegroundColor Cyan
