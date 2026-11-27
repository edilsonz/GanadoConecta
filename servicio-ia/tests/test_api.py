import json
import os
from secrets import token_hex
import unittest
from unittest.mock import patch

from ganadoconecta_ia.api import crear_app
from ganadoconecta_ia.ollama import MotorNoDisponible, Seleccion


class ApiInternaTest(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        self.secreto = token_hex(32)
        with patch.dict(os.environ, {'GANADOCONECTA_IA_SECRETO': self.secreto}):
            self.app = crear_app()

    async def enviar(self, ruta='/interno/v1/inferir', metodo='POST', secreto=None, cuerpo=b''):
        mensajes = []
        headers = [] if secreto is None else [(b'x-gc-ia-interno', secreto.encode())]

        async def recibir():
            return {'type': 'http.request', 'body': cuerpo, 'more_body': False}

        async def responder(mensaje):
            mensajes.append(mensaje)

        await self.app({
            'type': 'http', 'asgi': {'version': '3.0'}, 'scheme': 'http',
            'http_version': '1.1', 'method': metodo, 'path': ruta,
            'raw_path': ruta.encode(), 'query_string': b'', 'headers': headers,
        }, recibir, responder)
        contenido = json.loads(mensajes[-1]['body'])
        return mensajes[0]['status'], contenido

    async def test_documenta_el_contrato_sin_publicar_las_rutas_de_documentacion(self):
        esquema = self.app.openapi()
        operacion = esquema['paths']['/interno/v1/inferir']['post']
        self.assertEqual(set(operacion['responses']), {'200', '400', '401', '413', '429', '503'})
        self.assertEqual(operacion['security'], [{'credencialInterna': []}])
        self.assertIn('Hecho', esquema['components']['schemas'])
        for ruta in ('/openapi.json', '/docs', '/redoc'):
            with self.subTest(ruta=ruta):
                self.assertEqual((await self.enviar(ruta=ruta, metodo='GET'))[0], 404)

    async def test_rechaza_autenticacion_invalida_y_limita_el_cuerpo(self):
        self.assertEqual((await self.enviar())[0], 401)
        self.assertEqual((await self.enviar(secreto=self.secreto, cuerpo=b'x' * 8193))[0], 413)
        self.assertEqual((await self.enviar(secreto=self.secreto, cuerpo=b'{}'))[0], 400)

    async def test_acepta_una_seleccion_del_adaptador_y_distingue_indisponibilidad(self):
        cuerpo = b'{"pregunta":"Consulta de prueba","hechos":[{"id":"H1","tipo":"ULTIMO_PESAJE","texto":"Dato sintetico"}]}'
        with patch('ganadoconecta_ia.api.MotorOllama.inferir',
                   return_value=Seleccion(estado='RESPUESTA', estilo='DIRECTO', referencias=['H1'])):
            self.assertEqual(await self.enviar(secreto=self.secreto, cuerpo=cuerpo),
                             (200, {'estado': 'RESPUESTA', 'estilo': 'DIRECTO', 'referencias': ['H1']}))
        with patch('ganadoconecta_ia.api.MotorOllama.inferir', side_effect=MotorNoDisponible):
            self.assertEqual((await self.enviar(secreto=self.secreto, cuerpo=cuerpo))[0], 503)
