export const NOMBRE_BASE_OFFLINE = 'ganadoconecta-offline'
export const VERSION_BASE_OFFLINE = 3

export const ALMACEN_OPERACIONES = 'operacionesPendientes'
export const INDICE_OPERACIONES_CONTEXTO = 'porContexto'
export const INDICE_OPERACION = 'porOperacion'
export const INDICE_ANIMAL_PENDIENTE = 'porAnimalPendiente'

export const ALMACEN_CONTEXTOS = 'contextosOffline'
export const INDICE_CONTEXTOS_USUARIO = 'porUsuario'

export const ALMACEN_EVENTOS_SANITARIOS = 'eventosSanitariosPendientes'
export const INDICE_EVENTOS_SANITARIOS_CONTEXTO = 'porContextoSanitario'
export const INDICE_EVENTO_SANITARIO_OPERACION = 'porOperacionSanitaria'

export function abrirBaseOffline(
  factory: IDBFactory,
  nombre: string = NOMBRE_BASE_OFFLINE,
): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const solicitud = factory.open(nombre, VERSION_BASE_OFFLINE)
    let terminada = false
    solicitud.onupgradeneeded = () => {
      const db = solicitud.result
      if (!db.objectStoreNames.contains(ALMACEN_OPERACIONES)) {
        const operaciones = db.createObjectStore(ALMACEN_OPERACIONES, {
          keyPath: 'clave',
        })
        operaciones.createIndex(INDICE_OPERACIONES_CONTEXTO, 'contextoClave')
        operaciones.createIndex(INDICE_OPERACION, 'operationId', { unique: true })
        operaciones.createIndex(INDICE_ANIMAL_PENDIENTE, 'animalClave', {
          unique: true,
        })
      }
      if (!db.objectStoreNames.contains(ALMACEN_CONTEXTOS)) {
        const contextos = db.createObjectStore(ALMACEN_CONTEXTOS, {
          keyPath: 'clave',
        })
        contextos.createIndex(INDICE_CONTEXTOS_USUARIO, 'usuarioId')
      }
      if (!db.objectStoreNames.contains(ALMACEN_EVENTOS_SANITARIOS)) {
        const eventos = db.createObjectStore(ALMACEN_EVENTOS_SANITARIOS, {
          keyPath: 'clave',
        })
        eventos.createIndex(INDICE_EVENTOS_SANITARIOS_CONTEXTO, 'contextoClave')
        eventos.createIndex(INDICE_EVENTO_SANITARIO_OPERACION, 'operationId', {
          unique: true,
        })
      }
    }
    solicitud.onerror = () => {
      terminada = true
      reject(solicitud.error)
    }
    solicitud.onblocked = () => {
      terminada = true
      reject(new DOMException('Actualización de IndexedDB bloqueada', 'BlockedError'))
    }
    solicitud.onsuccess = () => {
      const db = solicitud.result
      if (terminada) {
        db.close()
        return
      }
      db.onversionchange = () => db.close()
      resolve(db)
    }
  })
}
