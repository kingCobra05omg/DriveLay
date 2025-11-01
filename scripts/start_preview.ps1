Add-Type -AssemblyName System.Net
Add-Type -AssemblyName System.IO

$root = "c:\xampp\htdocs\DriveLay"
$prefix = "http://localhost:8002/"

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add($prefix)
try {
    $listener.Start()
} catch {
    Write-Error "No se pudo iniciar el servidor: $_"
    exit 1
}
Write-Output "Preview URL: $prefix"

while ($true) {
    try {
        $context = $listener.GetContext()
        $request = $context.Request
        $relativePath = $request.Url.LocalPath.TrimStart('/')
        $path = Join-Path $root $relativePath

        if ([System.IO.File]::Exists($path)) {
            $bytes = [System.IO.File]::ReadAllBytes($path)
            $context.Response.ContentType = 'application/octet-stream'
            $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
        } else {
            $context.Response.StatusCode = 404
            $writer = New-Object System.IO.StreamWriter($context.Response.OutputStream)
            $writer.WriteLine('Not found')
            $writer.Dispose()
        }
        $context.Response.Close()
    } catch {
        Write-Error "Error atendiendo la solicitud: $_"
        break
    }
}