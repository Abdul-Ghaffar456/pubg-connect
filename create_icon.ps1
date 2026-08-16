Add-Type -AssemblyName System.Drawing

function Create-GamepadBitmap([int]$size) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    # Background rounded rectangle
    $radius = [float]($size * 0.22)
    $d = $radius * 2
    $w = [float]($size - 2)
    $h = [float]($size - 2)
    
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddArc(1.0, 1.0, $d, $d, 180.0, 90.0)
    $path.AddArc(1.0 + $w - $d, 1.0, $d, $d, 270.0, 90.0)
    $path.AddArc(1.0 + $w - $d, 1.0 + $h - $d, $d, $d, 0.0, 90.0)
    $path.AddArc(1.0, 1.0 + $h - $d, $d, $d, 90.0, 90.0)
    $path.CloseFigure()

    # Fill background with dark theme (#0B0F19)
    $bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 11, 15, 25))
    $g.FillPath($bgBrush, $path)

    # Border with emerald green (#10B981)
    $borderPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 16, 185, 129), [float]($size * 0.04))
    $g.DrawPath($borderPen, $path)

    # Draw Gamepad Body
    $gpWidth = [float]($size * 0.70)
    $gpHeight = [float]($size * 0.44)
    $gpX = [float](($size - $gpWidth) / 2.0)
    $gpY = [float](($size - $gpHeight) / 2.0)
    $gpd = [float]($gpHeight * 0.90)

    $gpPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $gpPath.AddArc($gpX, $gpY, $gpd, $gpd, 180.0, 90.0)
    $gpPath.AddArc($gpX + $gpWidth - $gpd, $gpY, $gpd, $gpd, 270.0, 90.0)
    $gpPath.AddArc($gpX + $gpWidth - $gpd, $gpY + $gpHeight - $gpd, $gpd, $gpd, 0.0, 90.0)
    $gpPath.AddArc($gpX, $gpY + $gpHeight - $gpd, $gpd, $gpd, 90.0, 90.0)
    $gpPath.CloseFigure()

    # Fill gamepad with neon emerald
    $gpBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 16, 185, 129))
    $g.FillPath($gpBrush, $gpPath)

    # D-Pad (Left Side Cross)
    $darkBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 11, 15, 25))
    $dpadCenterX = $gpX + ($gpWidth * 0.28)
    $dpadCenterY = $gpY + ($gpHeight * 0.50)
    $dpadArm = $size * 0.055
    $dpadThick = $size * 0.038

    # Horizontal bar
    $g.FillRectangle($darkBrush, [float]($dpadCenterX - $dpadArm), [float]($dpadCenterY - ($dpadThick / 2)), [float]($dpadArm * 2), [float]$dpadThick)
    # Vertical bar
    $g.FillRectangle($darkBrush, [float]($dpadCenterX - ($dpadThick / 2)), [float]($dpadCenterY - $dpadArm), [float]$dpadThick, [float]($dpadArm * 2))

    # Action Buttons (Right Side 4 Dots)
    $btnCenterX = $gpX + ($gpWidth * 0.72)
    $btnCenterY = $gpY + ($gpHeight * 0.50)
    $btnOffset = $size * 0.045
    $btnSize = $size * 0.036

    # North, South, East, West buttons
    $g.FillEllipse($darkBrush, [float]($btnCenterX - ($btnSize / 2)), [float]($btnCenterY - $btnOffset - ($btnSize / 2)), [float]$btnSize, [float]$btnSize)
    $g.FillEllipse($darkBrush, [float]($btnCenterX - ($btnSize / 2)), [float]($btnCenterY + $btnOffset - ($btnSize / 2)), [float]$btnSize, [float]$btnSize)
    $g.FillEllipse($darkBrush, [float]($btnCenterX + $btnOffset - ($btnSize / 2)), [float]($btnCenterY - ($btnSize / 2)), [float]$btnSize, [float]$btnSize)
    $g.FillEllipse($darkBrush, [float]($btnCenterX - $btnOffset - ($btnSize / 2)), [float]($btnCenterY - ($btnSize / 2)), [float]$btnSize, [float]$btnSize)

    # Middle Center Indicator Dot
    $whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 240, 253, 244))
    $dotSize = $size * 0.03
    $g.FillEllipse($whiteBrush, [float]($gpX + ($gpWidth * 0.50) - ($dotSize / 2)), [float]($gpY + ($gpHeight * 0.50) - ($dotSize / 2)), [float]$dotSize, [float]$dotSize)

    $g.Dispose()
    return $bmp
}

# Generate Multi-Resolution ICO File
$sizes = @(256, 128, 64, 48, 32, 16)
$icoPath = "src\PubgConnect.Client\app_icon.ico"

$pngStreams = @()
foreach ($s in $sizes) {
    $bmp = Create-GamepadBitmap $s
    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngStreams += $ms
    $bmp.Dispose()
}

$fs = [System.IO.File]::Create($icoPath)
$bw = New-Object System.IO.BinaryWriter($fs)

# ICONHEADER
$bw.Write([uint16]0) # Reserved
$bw.Write([uint16]1) # Type 1 = ICO
$bw.Write([uint16]$sizes.Count) # Count of images

$offset = 6 + ($sizes.Count * 16)

# ICONDIRENTRY list
for ($i = 0; $i -lt $sizes.Count; $i++) {
    $s = $sizes[$i]
    $stream = $pngStreams[$i]
    $bytes = $stream.ToArray()

    $dimByte = [byte]0
    if ($s -ne 256) { $dimByte = [byte]$s }

    $bw.Write($dimByte) # Width
    $dimByteHeight = [byte]0
    if ($s -ne 256) { $dimByteHeight = [byte]$s }
    $bw.Write($dimByteHeight) # Height
    $bw.Write([byte]0) # Color Count
    $bw.Write([byte]0) # Reserved
    $bw.Write([uint16]1) # Color Planes
    $bw.Write([uint16]32) # Bits per pixel
    $bw.Write([uint32]$bytes.Length) # Image Size
    $bw.Write([uint32]$offset) # Offset

    $offset += $bytes.Length
}

# Image Data
for ($i = 0; $i -lt $sizes.Count; $i++) {
    $stream = $pngStreams[$i]
    $bytes = $stream.ToArray()
    $bw.Write($bytes)
    $stream.Dispose()
}

$bw.Close()
$fs.Close()
Write-Host "Created valid multi-resolution Windows ICO icon: $icoPath" -ForegroundColor Green
