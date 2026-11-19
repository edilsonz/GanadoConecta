type DefinicionIndice = {
  keyPath: string
  unique: boolean
}

type EstadoAlmacen = {
  keyPath: string
  indices: Map<string, DefinicionIndice>
  registros: Map<string, unknown>
}

type EstadoBase = {
  version: number
  almacenes: Map<string, EstadoAlmacen>
}

type SolicitudMutable<T> = {
  result: T
  error: DOMException | null
  onsuccess: ((evento: Event) => void) | null
  onerror: ((evento: Event) => void) | null
}

type SolicitudAperturaMutable = SolicitudMutable<IDBDatabase> & {
  onupgradeneeded: ((evento: IDBVersionChangeEvent) => void) | null
  onblocked: ((evento: Event) => void) | null
}

function solicitudVacia<T>(): SolicitudMutable<T> {
  return {
    result: undefined as T,
    error: null,
    onsuccess: null,
    onerror: null,
  }
}

function copiarAlmacen(almacen: EstadoAlmacen): EstadoAlmacen {
  return {
    keyPath: almacen.keyPath,
    indices: new Map(almacen.indices),
    registros: new Map(Array.from(almacen.registros, ([clave, valor]) => [
      clave,
      structuredClone(valor),
    ])),
  }
}

function claveIndice(registro: unknown, keyPath: string): unknown {
  return (registro as Record<string, unknown>)[keyPath]
}

class TransaccionFalsa {
  oncomplete: ((evento: Event) => void) | null = null
  onerror: ((evento: Event) => void) | null = null
  onabort: ((evento: Event) => void) | null = null
  error: DOMException | null = null
  readonly mode: IDBTransactionMode
  private pendientes = 0
  private finalizada = false
  private finalizacionProgramada = false
  private readonly trabajo: EstadoAlmacen

  constructor(
    private readonly original: EstadoAlmacen,
    mode: IDBTransactionMode,
    private readonly controlador: FabricaIndexedDbFalsa,
  ) {
    this.mode = mode
    this.trabajo = mode === 'readonly' ? original : copiarAlmacen(original)
    this.programarFinalizacion()
  }

  objectStore(): IDBObjectStore {
    return new AlmacenFalso(this.trabajo, this) as unknown as IDBObjectStore
  }

  abort(): void {
    if (!this.finalizada) {
      this.fallar(new DOMException('Transacción cancelada', 'AbortError'))
    }
  }

  crearSolicitud<T>(accion: () => T, escritura = false): IDBRequest<T> {
    const solicitud = solicitudVacia<T>()
    this.pendientes += 1
    queueMicrotask(() => {
      if (this.finalizada) return
      try {
        if (escritura && this.controlador.consumirFalloEscritura()) {
          throw new DOMException('Cuota simulada', 'QuotaExceededError')
        }
        solicitud.result = structuredClone(accion())
        solicitud.onsuccess?.(new Event('success'))
        this.pendientes -= 1
        this.programarFinalizacion()
      } catch (error) {
        const domError = error instanceof DOMException
          ? error : new DOMException('Fallo simulado', 'UnknownError')
        solicitud.error = domError
        solicitud.onerror?.(new Event('error'))
        this.fallar(domError)
      }
    })
    return solicitud as unknown as IDBRequest<T>
  }

  private programarFinalizacion(): void {
    if (this.finalizacionProgramada || this.finalizada) return
    this.finalizacionProgramada = true
    queueMicrotask(() => {
      this.finalizacionProgramada = false
      if (this.finalizada || this.pendientes > 0) return
      this.finalizada = true
      if (this.mode !== 'readonly') {
        this.original.registros = this.trabajo.registros
        this.original.indices = this.trabajo.indices
      }
      this.oncomplete?.(new Event('complete'))
    })
  }

  private fallar(error: DOMException): void {
    if (this.finalizada) return
    this.finalizada = true
    this.error = error
    this.onerror?.(new Event('error'))
    this.onabort?.(new Event('abort'))
  }
}

class IndiceFalso {
  constructor(
    private readonly almacen: EstadoAlmacen,
    private readonly transaccion: TransaccionFalsa,
    private readonly definicion: DefinicionIndice,
  ) {}

  get(clave: IDBValidKey): IDBRequest<unknown> {
    return this.transaccion.crearSolicitud(() => {
      for (const registro of this.almacen.registros.values()) {
        if (claveIndice(registro, this.definicion.keyPath) === clave) {
          return registro
        }
      }
      return undefined
    })
  }

  getAll(clave: IDBValidKey): IDBRequest<unknown[]> {
    return this.transaccion.crearSolicitud(() => Array.from(
      this.almacen.registros.values(),
    ).filter(registro => claveIndice(registro, this.definicion.keyPath) === clave))
  }
}

class AlmacenFalso {
  constructor(
    private readonly estado: EstadoAlmacen,
    private readonly transaccion?: TransaccionFalsa,
  ) {}

  createIndex(nombre: string, keyPath: string, opciones?: IDBIndexParameters): IDBIndex {
    this.estado.indices.set(nombre, { keyPath, unique: opciones?.unique ?? false })
    return {} as IDBIndex
  }

  index(nombre: string): IDBIndex {
    const definicion = this.estado.indices.get(nombre)
    if (!definicion || !this.transaccion) {
      throw new DOMException('Índice inexistente', 'NotFoundError')
    }
    return new IndiceFalso(
      this.estado,
      this.transaccion,
      definicion,
    ) as unknown as IDBIndex
  }

  get(clave: IDBValidKey): IDBRequest<unknown> {
    return this.exigirTransaccion().crearSolicitud(
      () => this.estado.registros.get(String(clave)),
    )
  }

  add(valor: unknown): IDBRequest<IDBValidKey> {
    return this.guardar(valor, true)
  }

  put(valor: unknown): IDBRequest<IDBValidKey> {
    return this.guardar(valor, false)
  }

  private guardar(valor: unknown, soloNuevo: boolean): IDBRequest<IDBValidKey> {
    return this.exigirTransaccion().crearSolicitud(() => {
      const clave = String(claveIndice(valor, this.estado.keyPath))
      if (soloNuevo && this.estado.registros.has(clave)) {
        throw new DOMException('Clave duplicada', 'ConstraintError')
      }
      for (const definicion of this.estado.indices.values()) {
        if (!definicion.unique) continue
        const valorIndice = claveIndice(valor, definicion.keyPath)
        for (const [claveExistente, existente] of this.estado.registros) {
          if (claveExistente !== clave
              && claveIndice(existente, definicion.keyPath) === valorIndice) {
            throw new DOMException('Índice duplicado', 'ConstraintError')
          }
        }
      }
      this.estado.registros.set(clave, structuredClone(valor))
      return clave as IDBValidKey
    }, true)
  }

  private exigirTransaccion(): TransaccionFalsa {
    if (!this.transaccion) {
      throw new DOMException('Transacción requerida', 'TransactionInactiveError')
    }
    return this.transaccion
  }
}

class BaseFalsa {
  onversionchange: ((evento: IDBVersionChangeEvent) => void) | null = null

  constructor(
    private readonly estado: EstadoBase,
    private readonly controlador: FabricaIndexedDbFalsa,
  ) {}

  get objectStoreNames(): DOMStringList {
    return {
      contains: (nombre: string) => this.estado.almacenes.has(nombre),
    } as DOMStringList
  }

  createObjectStore(nombre: string, opciones?: IDBObjectStoreParameters): IDBObjectStore {
    const estado: EstadoAlmacen = {
      keyPath: String(opciones?.keyPath),
      indices: new Map(),
      registros: new Map(),
    }
    this.estado.almacenes.set(nombre, estado)
    return new AlmacenFalso(estado) as unknown as IDBObjectStore
  }

  transaction(nombre: string, modo?: IDBTransactionMode): IDBTransaction {
    const almacen = this.estado.almacenes.get(nombre)
    if (!almacen) throw new DOMException('Almacén inexistente', 'NotFoundError')
    return new TransaccionFalsa(
      almacen,
      modo ?? 'readonly',
      this.controlador,
    ) as unknown as IDBTransaction
  }

  close(): void {}
}

export class FabricaIndexedDbFalsa {
  private readonly bases = new Map<string, EstadoBase>()
  private fallarEscritura = false

  readonly factory = {
    open: (nombre: string, version = 1) => this.abrir(nombre, version),
  } as IDBFactory

  fallarProximaEscritura(): void {
    this.fallarEscritura = true
  }

  consumirFalloEscritura(): boolean {
    const debeFallar = this.fallarEscritura
    this.fallarEscritura = false
    return debeFallar
  }

  private abrir(nombre: string, version: number): IDBOpenDBRequest {
    const solicitud: SolicitudAperturaMutable = {
      ...solicitudVacia<IDBDatabase>(),
      onupgradeneeded: null,
      onblocked: null,
    }
    queueMicrotask(() => {
      let estado = this.bases.get(nombre)
      const versionAnterior = estado?.version ?? 0
      if (!estado) {
        estado = { version, almacenes: new Map() }
        this.bases.set(nombre, estado)
      }
      if (version < estado.version) {
        solicitud.error = new DOMException('Versión anterior', 'VersionError')
        solicitud.onerror?.(new Event('error'))
        return
      }
      const requiereActualizacion = version > versionAnterior
      estado.version = version
      solicitud.result = new BaseFalsa(
        estado,
        this,
      ) as unknown as IDBDatabase
      if (requiereActualizacion) {
        solicitud.onupgradeneeded?.({
          oldVersion: versionAnterior,
          newVersion: version,
        } as IDBVersionChangeEvent)
      }
      solicitud.onsuccess?.(new Event('success'))
    })
    return solicitud as unknown as IDBOpenDBRequest
  }
}
