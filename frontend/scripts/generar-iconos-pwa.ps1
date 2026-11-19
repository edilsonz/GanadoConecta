param(
  [string]$Destino = (Join-Path $PSScriptRoot '..\public\icons')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

function New-RoundedRectanglePath {
  param(
    [System.Drawing.RectangleF]$Rectangle,
    [float]$Radius
  )
  $diameter = $Radius * 2
  $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
  $path.AddArc($Rectangle.X, $Rectangle.Y, $diameter, $diameter, 180, 90)
  $path.AddArc($Rectangle.Right - $diameter, $Rectangle.Y, $diameter, $diameter, 270, 90)
  $path.AddArc($Rectangle.Right - $diameter, $Rectangle.Bottom - $diameter, $diameter, $diameter, 0, 90)
  $path.AddArc($Rectangle.X, $Rectangle.Bottom - $diameter, $diameter, $diameter, 90, 90)
  $path.CloseFigure()
  return $path
}

function New-PwaIcon {
  param(
    [int]$Size,
    [string]$FileName,
    [bool]$Maskable
  )

  $bitmap = [System.Drawing.Bitmap]::new($Size, $Size)
  $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
  try {
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $graphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#174b38'))

    $margin = if ($Maskable) { $Size * 0.22 } else { $Size * 0.17 }
    $inner = [System.Drawing.RectangleF]::new(
      $margin,
      $margin,
      $Size - (2 * $margin),
      $Size - (2 * $margin)
    )
    $path = New-RoundedRectanglePath -Rectangle $inner -Radius ($Size * 0.14)
    $cream = [System.Drawing.SolidBrush]::new(
      [System.Drawing.ColorTranslator]::FromHtml('#fffdf7')
    )
    $graphics.FillPath($cream, $path)

    $fontSize = if ($Maskable) { $Size * 0.25 } else { $Size * 0.34 }
    $font = [System.Drawing.Font]::new(
      'Arial',
      $fontSize,
      [System.Drawing.FontStyle]::Bold,
      [System.Drawing.GraphicsUnit]::Pixel
    )
    $dark = [System.Drawing.SolidBrush]::new(
      [System.Drawing.ColorTranslator]::FromHtml('#174b38')
    )
    $format = [System.Drawing.StringFormat]::new()
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $graphics.DrawString('GC', $font, $dark, $inner, $format)

    $dotSize = $Size * 0.095
    $dotX = if ($Maskable) { $Size * 0.66 } else { $Size * 0.69 }
    $dotY = if ($Maskable) { $Size * 0.65 } else { $Size * 0.67 }
    $sage = [System.Drawing.SolidBrush]::new(
      [System.Drawing.ColorTranslator]::FromHtml('#9fbd91')
    )
    $graphics.FillEllipse($sage, $dotX, $dotY, $dotSize, $dotSize)

    New-Item -ItemType Directory -Force -Path $Destino | Out-Null
    $pathOutput = Join-Path $Destino $FileName
    $bitmap.Save($pathOutput, [System.Drawing.Imaging.ImageFormat]::Png)
  }
  finally {
    $graphics.Dispose()
    $bitmap.Dispose()
  }
}

New-PwaIcon -Size 192 -FileName 'ganadoconecta-192.png' -Maskable $false
New-PwaIcon -Size 512 -FileName 'ganadoconecta-512.png' -Maskable $false
New-PwaIcon -Size 512 -FileName 'ganadoconecta-maskable-512.png' -Maskable $true
