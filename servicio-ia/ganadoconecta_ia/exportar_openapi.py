"""Exporta el contrato interno sin abrir un puerto ni usar credenciales persistentes."""

import json
import os
from secrets import token_hex

from .api import crear_app


def main():
    os.environ['GANADOCONECTA_IA_SECRETO'] = token_hex(32)
    print(json.dumps(crear_app().openapi(), ensure_ascii=True, indent=2, sort_keys=True))


if __name__ == '__main__':
    main()
