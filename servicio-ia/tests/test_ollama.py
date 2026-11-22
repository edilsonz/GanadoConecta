import io
import json
import unittest
from unittest.mock import patch
from urllib.error import URLError

from ganadoconecta_ia.ollama import Consulta, Hecho, MotorNoDisponible, MotorOllama


class OllamaTest(unittest.TestCase):
    def setUp(self):
        self.motor = MotorOllama(11439, 1)
        self.consulta = Consulta(pregunta='¿Cuánto pesó?', hechos=[
            Hecho(id='H1', tipo='ULTIMO_PESAJE', texto='Pesaje: 140 kg.'),
        ])

    def simular(self, eleccion, motivo='stop'):
        content = json.dumps({'eleccion': eleccion})
        cuerpo = json.dumps({'message': {'content': content}, 'done': True, 'done_reason': motivo}).encode()
        return patch.object(self.motor.http, 'open', return_value=io.BytesIO(cuerpo))

    def test_valida_y_acota_referencias_del_modelo(self):
        with self.simular('H1') as abrir:
            self.assertEqual(self.motor.inferir(self.consulta).referencias, ['H1'])
        peticion = json.loads(abrir.call_args.args[0].data)
        self.assertFalse(peticion['stream'])
        self.assertEqual(peticion['format']['properties']['eleccion']['enum'], ['H1', 'INSUFICIENTE'])

    def test_rechaza_referencias_desconocidas_listados_no_admitidos_y_salidas_incompletas(self):
        for eleccion, motivo in [('H8', 'stop'), (['H1', 'H1'], 'stop'), ('H1', 'length'), ('LISTADO', 'stop')]:
            with self.subTest(eleccion=eleccion, motivo=motivo), self.simular(eleccion, motivo):
                with self.assertRaises(MotorNoDisponible):
                    self.motor.inferir(self.consulta)

    def test_distingue_abstencion_de_demora_o_caida(self):
        with self.simular('INSUFICIENTE'):
            self.assertEqual(self.motor.inferir(self.consulta).estado, 'INSUFICIENTE')
        for fallo in [TimeoutError(), URLError('sin conexión')]:
            with patch.object(self.motor.http, 'open', side_effect=fallo):
                with self.assertRaises(MotorNoDisponible):
                    self.motor.inferir(self.consulta)

    def test_expande_listado_solo_a_hechos_del_tema_entregado(self):
        consulta = Consulta(pregunta='¿Qué actividades pendientes hay?', hechos=[
            Hecho(id='H1', tipo='AGENDA', texto='Actividad pendiente A.'),
            Hecho(id='H2', tipo='AGENDA', texto='Actividad pendiente B.'),
        ])
        with self.simular('LISTADO'):
            self.assertEqual(self.motor.inferir(consulta).referencias, ['H1', 'H2'])
