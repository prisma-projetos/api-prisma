$ErrorActionPreference = "Stop"

$certFile = if ($env:TLS_CERTFILE) { $env:TLS_CERTFILE } else { "certs\localhost.crt" }
$keyFile = if ($env:TLS_KEYFILE) { $env:TLS_KEYFILE } else { "certs\localhost.key" }

if (-not (Test-Path $certFile) -or -not (Test-Path $keyFile)) {
    throw "Certificado TLS não encontrado. Defina TLS_CERTFILE/TLS_KEYFILE ou crie certs\localhost.crt e certs\localhost.key."
}

py -m uvicorn app.main:app --host 127.0.0.1 --port 8443 --ssl-certfile $certFile --ssl-keyfile $keyFile --reload
