from hmac import compare_digest
import os
from threading import Lock

from fastapi import FastAPI, Request
from starlette.concurrency import run_in_threadpool
from starlette.responses import JSONResponse

from .ollama import Consulta, MotorNoDisponible, MotorOllama


def crear_app() -> FastAPI:
    secreto = os.environ.get('GANADOCONECTA_IA_SECRETO', '').encode()
    if len(secreto) < 32:
        raise RuntimeError('Configura el secreto interno de IA')
    puerto = int(os.environ.get('GANADOCONECTA_OLLAMA_PUERTO', '11439'))
    plazo = float(os.environ.get('GANADOCONECTA_OLLAMA_PLAZO', '25'))
    if not 1 <= puerto <= 65535 or not 0 < plazo <= 25:
        raise RuntimeError('Configuración local de Ollama inválida')

    app = FastAPI(docs_url=None, redoc_url=None, openapi_url=None)
    motor = MotorOllama(puerto, plazo)
    ocupado = Lock()

    @app.post('/interno/v1/inferir')
    async def inferir(request: Request):
        if not compare_digest(request.headers.get('x-gc-ia-interno', '').encode(), secreto):
            return respuesta(401, {'codigo': 'ACCESO_INTERNO_DENEGADO'})
        datos = bytearray()
        async for bloque in request.stream():
            if len(datos) + len(bloque) > 8192:
                return respuesta(413, {'codigo': 'SOLICITUD_INVALIDA'})
            datos.extend(bloque)
        try:
            consulta = Consulta.model_validate_json(datos)
        except ValueError:
            return respuesta(400, {'codigo': 'SOLICITUD_INVALIDA'})

        def ejecutar():
            if not ocupado.acquire(blocking=False):
                return respuesta(429, {'codigo': 'INFERENCIA_OCUPADA'})
            try:
                return respuesta(200, motor.inferir(consulta).model_dump())
            except MotorNoDisponible:
                return respuesta(503, {'codigo': 'INFERENCIA_NO_DISPONIBLE'})
            except ValueError:
                return respuesta(400, {'codigo': 'SOLICITUD_INVALIDA'})
            finally:
                ocupado.release()

        return await run_in_threadpool(ejecutar)

    return app


def respuesta(estado: int, datos: dict) -> JSONResponse:
    return JSONResponse(datos, status_code=estado, headers={'Cache-Control': 'no-store'})
