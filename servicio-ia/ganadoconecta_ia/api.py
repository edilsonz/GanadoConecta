from hmac import compare_digest
import os
from threading import Lock

from fastapi import FastAPI, Request
from pydantic import BaseModel
from starlette.concurrency import run_in_threadpool
from starlette.responses import JSONResponse

from .ollama import Consulta, MotorNoDisponible, MotorOllama, Seleccion


class ErrorInferencia(BaseModel):
    codigo: str


_ERRORES = {
    400: 'Cuerpo o consulta inválidos.',
    401: 'Credencial interna ausente o inválida.',
    413: 'El cuerpo supera 8192 bytes.',
    429: 'Ya se está procesando otra inferencia.',
    503: 'El motor no está disponible o respondió con un resultado inválido.',
}


def crear_app() -> FastAPI:
    secreto = os.environ.get('GANADOCONECTA_IA_SECRETO', '').encode()
    if len(secreto) < 32:
        raise RuntimeError('Configura el secreto interno de IA')
    puerto = int(os.environ.get('GANADOCONECTA_OLLAMA_PUERTO', '11439'))
    plazo = float(os.environ.get('GANADOCONECTA_OLLAMA_PLAZO', '25'))
    if not 1 <= puerto <= 65535 or not 0 < plazo <= 25:
        raise RuntimeError('Configuración local de Ollama inválida')

    app = FastAPI(title='GanadoConecta · Inferencia interna', version='1.0',
                  docs_url=None, redoc_url=None, openapi_url=None)
    motor = MotorOllama(puerto, plazo)
    ocupado = Lock()

    @app.post('/interno/v1/inferir', summary='Seleccionar referencias autorizadas',
              response_model=Seleccion,
              responses={estado: {'model': ErrorInferencia, 'description': descripcion}
                         for estado, descripcion in _ERRORES.items()},
              openapi_extra={
                  'security': [{'credencialInterna': []}],
                  'requestBody': {'required': True, 'content': {'application/json': {
                      'schema': {'$ref': '#/components/schemas/Consulta'},
                  }}},
              })
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

    generar = app.openapi

    def contrato():
        documento = generar()
        componentes = documento['components']
        consulta = Consulta.model_json_schema(ref_template='#/components/schemas/{model}')
        esquemas = componentes['schemas']
        esquemas.update(consulta.pop('$defs', {}))
        esquemas['Consulta'] = consulta
        componentes['securitySchemes'] = {
            'credencialInterna': {'type': 'apiKey', 'in': 'header', 'name': 'X-GC-IA-Interno'},
        }
        return documento

    app.openapi = contrato
    return app


def respuesta(estado: int, datos: dict) -> JSONResponse:
    return JSONResponse(datos, status_code=estado, headers={'Cache-Control': 'no-store'})
