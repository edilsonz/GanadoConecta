import { FormEvent, useEffect, useRef, useState } from 'react'
import {
  consultarAnimal,
  ErrorAnimal,
  listarAnimales,
  registrarAnimal,
  type Animal,
  type CategoriaAnimal,
  type SolicitudRegistroAnimal,
  type SexoAnimal,
} from './animalesApi'
import {
  consultarAccesoGanaderia,
  ErrorGanaderia,
  type AccesoGanaderia,
  type Ganaderia,
} from './ganaderiasApi'
import type { SesionActiva } from './sesionApi'

type AnimalesPanelProps = {
  ganaderia: Ganaderia
  sesion: SesionActiva
  onSesionInvalida: () => void
}

type FormularioAnimal = {
  sexo: '' | SexoAnimal
  nombre: string
  areteFrente: string
  areteReverso: string
  raza: string
  fechaNacimiento: string
  nacimientoEstimado: boolean
  categoria: '' | CategoriaAnimal
  observaciones: string
}

const formularioInicial: FormularioAnimal = {
  sexo: '',
  nombre: '',
  areteFrente: '',
  areteReverso: '',
  raza: '',
  fechaNacimiento: '',
  nacimientoEstimado: false,
  categoria: '',
  observaciones: '',
}

const categoriasPorSexo = {
  HEMBRA: ['TERNERA', 'VAQUILLA', 'VACA'],
  MACHO: ['TERNERO', 'TORETE', 'TORO'],
} as const

const etiquetasCategoria: Record<CategoriaAnimal, string> = {
  TERNERA: 'Ternera',
  VAQUILLA: 'Vaquilla',
  VACA: 'Vaca',
  TERNERO: 'Ternero',
  TORETE: 'Torete',
  TORO: 'Toro',
}

const mensajesError = {
  validacion: 'Revisa los campos indicados antes de volver a enviar.',
  conflicto: 'El registro entra en conflicto con una operación existente. Actualiza la lista antes de intentarlo otra vez.',
  permiso: 'Tu acceso a esta ganadería cambió. Selecciónala nuevamente para revalidarlo.',
  seguridad: 'No pudimos validar la seguridad de la solicitud. Actualiza la página e inténtalo otra vez.',
  limite: 'Se alcanzó el límite temporal de solicitudes. Espera unos minutos antes de volver a intentar.',
  sesion: 'Tu sesión terminó. Vuelve a iniciar sesión para continuar.',
  noEncontrado: 'No se encontró el animal solicitado en esta ganadería.',
  conexion: 'No se recibió una confirmación. Puedes reintentar exactamente el mismo registro.',
  servicio: 'El servicio no confirmó la operación. Puedes reintentar exactamente el mismo registro.',
} as const

const controlesNoAdmitidos = /[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F-\u009F]|\p{Cf}/u

function fechaHoyLima() {
  const partes = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date())
  const valor = (tipo: Intl.DateTimeFormatPartTypes) => partes.find(parte => parte.type === tipo)?.value ?? ''
  return `${valor('year')}-${valor('month')}-${valor('day')}`
}

function normalizarOpcional(valor: string, permiteSaltos = false) {
  const normalizado = (permiteSaltos ? valor.replace(/\r\n?/g, '\n') : valor).normalize('NFC').trim()
  return normalizado || undefined
}

function validarFormulario(formulario: FormularioAnimal) {
  const errores: Record<string, string> = {}
  if (!formulario.sexo) errores.sexo = 'Selecciona MACHO o HEMBRA.'

  const textos = [
    ['nombre', formulario.nombre, 100, false],
    ['areteFrente', formulario.areteFrente, 100, false],
    ['areteReverso', formulario.areteReverso, 100, false],
    ['raza', formulario.raza, 100, false],
    ['observaciones', formulario.observaciones, 2000, true],
  ] as const
  for (const [campo, valor, maximo, permiteSaltos] of textos) {
    const texto = normalizarOpcional(valor, permiteSaltos)
    if (texto && Array.from(texto).length > maximo) errores[campo] = `Usa como máximo ${maximo} caracteres.`
    if (texto && controlesNoAdmitidos.test(texto)) errores[campo] = 'El texto contiene caracteres no admitidos.'
  }
  if (formulario.fechaNacimiento && formulario.fechaNacimiento > fechaHoyLima()) {
    errores.fechaNacimiento = 'La fecha de nacimiento no puede ser futura.'
  }
  if (formulario.categoria && formulario.sexo
      && !categoriasPorSexo[formulario.sexo].includes(formulario.categoria as never)) {
    errores.categoria = 'La categoría no corresponde al sexo seleccionado.'
  }
  return errores
}

function crearSolicitud(
  formulario: FormularioAnimal,
  animalId: string,
): SolicitudRegistroAnimal {
  return {
    operationId: crypto.randomUUID(),
    animalId,
    tipo: 'CREAR_ANIMAL',
    schemaVersion: 1,
    baseVersion: 0,
    sexo: formulario.sexo as SexoAnimal,
    ...(normalizarOpcional(formulario.nombre) && { nombre: normalizarOpcional(formulario.nombre) }),
    ...(normalizarOpcional(formulario.areteFrente) && { areteFrente: normalizarOpcional(formulario.areteFrente) }),
    ...(normalizarOpcional(formulario.areteReverso) && { areteReverso: normalizarOpcional(formulario.areteReverso) }),
    ...(normalizarOpcional(formulario.raza) && { raza: normalizarOpcional(formulario.raza) }),
    ...(formulario.fechaNacimiento && {
      fechaNacimiento: formulario.fechaNacimiento,
      nacimientoEstimado: formulario.nacimientoEstimado,
    }),
    ...(formulario.categoria && { categoria: formulario.categoria }),
    ...(normalizarOpcional(formulario.observaciones, true)
      && { observaciones: normalizarOpcional(formulario.observaciones, true) }),
  }
}

function tituloAnimal(animal: Animal) {
  return animal.nombre || animal.areteFrente || `${animal.sexo === 'HEMBRA' ? 'Hembra' : 'Macho'} sin nombre`
}

function descripcionAnimal(animal: Animal) {
  return [animal.categoria ? etiquetasCategoria[animal.categoria] : null, animal.raza, animal.areteFrente]
    .filter(Boolean).join(' · ') || 'Sin datos descriptivos adicionales'
}

export function AnimalesPanel({ ganaderia, sesion, onSesionInvalida }: AnimalesPanelProps) {
  const [acceso, setAcceso] = useState<AccesoGanaderia | null>(null)
  const [animales, setAnimales] = useState<Animal[]>([])
  const [cursor, setCursor] = useState<string | null>(null)
  const [formulario, setFormulario] = useState<FormularioAnimal>(formularioInicial)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [cargando, setCargando] = useState(true)
  const [cargandoMas, setCargandoMas] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [pendienteIncierto, setPendienteIncierto] = useState<SolicitudRegistroAnimal | null>(null)
  const [animalIdActual, setAnimalIdActual] = useState<string | null>(null)
  const [ficha, setFicha] = useState<Animal | null>(null)
  const [cargandoFicha, setCargandoFicha] = useState(false)
  const envioEnCurso = useRef(false)

  function fallo(error: unknown) {
    return error instanceof ErrorAnimal ? error : new ErrorAnimal('servicio')
  }

  function aplicarFallo(error: ErrorAnimal) {
    setMensaje(mensajesError[error.tipo])
    if (error.tipo === 'validacion') setErrores(error.erroresPorCampo)
    if (error.tipo === 'sesion') onSesionInvalida()
    if (error.tipo === 'permiso') {
      setAcceso(null)
      setAnimales([])
      setCursor(null)
      setFicha(null)
    }
  }

  async function cargarPrimeraPagina() {
    const pagina = await listarAnimales(ganaderia.id, sesion.accessToken)
    setAnimales(pagina.items)
    setCursor(pagina.siguienteCursor)
  }

  useEffect(() => {
    let activo = true
    setAcceso(null)
    setAnimales([])
    setCursor(null)
    setFicha(null)
    setMensaje(null)
    setErrores({})
    setFormulario(formularioInicial)
    setPendienteIncierto(null)
    setAnimalIdActual(null)
    setCargando(true)

    async function cargar() {
      try {
        const contexto = await consultarAccesoGanaderia(ganaderia.id, sesion.accessToken)
        if (!activo) return
        setAcceso(contexto)
        const pagina = await listarAnimales(ganaderia.id, sesion.accessToken)
        if (!activo) return
        setAnimales(pagina.items)
        setCursor(pagina.siguienteCursor)
      } catch (error) {
        if (!activo) return
        if (error instanceof ErrorGanaderia) {
          const tipo = error.tipo === 'sesion' ? 'sesion'
            : error.tipo === 'seguridad' ? 'permiso' : 'servicio'
          aplicarFallo(new ErrorAnimal(tipo))
        } else {
          aplicarFallo(fallo(error))
        }
      } finally {
        if (activo) setCargando(false)
      }
    }
    void cargar()
    return () => { activo = false }
  }, [ganaderia.id, sesion.accessToken, onSesionInvalida])

  function cambiar<K extends keyof FormularioAnimal>(campo: K, valor: FormularioAnimal[K]) {
    if (pendienteIncierto) return
    setFormulario(actual => ({ ...actual, [campo]: valor }))
    setErrores(actual => {
      const siguientes = { ...actual }
      delete siguientes[campo]
      return siguientes
    })
    setMensaje(null)
  }

  function cambiarSexo(sexo: '' | SexoAnimal) {
    if (pendienteIncierto) return
    setFormulario(actual => ({
      ...actual,
      sexo,
      categoria: actual.categoria && sexo
        && categoriasPorSexo[sexo].includes(actual.categoria as never) ? actual.categoria : '',
    }))
    setErrores(actual => {
      const siguientes = { ...actual }
      delete siguientes.sexo
      delete siguientes.categoria
      return siguientes
    })
    setMensaje(null)
  }

  async function registrar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!acceso || acceso.rol === 'LECTOR' || envioEnCurso.current) return

    let solicitud = pendienteIncierto
    if (!solicitud) {
      const erroresFormulario = validarFormulario(formulario)
      setErrores(erroresFormulario)
      if (Object.keys(erroresFormulario).length > 0) {
        setMensaje(mensajesError.validacion)
        return
      }
      const animalId = animalIdActual ?? crypto.randomUUID()
      setAnimalIdActual(animalId)
      solicitud = crearSolicitud(formulario, animalId)
    }

    envioEnCurso.current = true
    setEnviando(true)
    setMensaje(null)
    try {
      const resultado = await registrarAnimal(ganaderia.id, solicitud, sesion.accessToken)
      setPendienteIncierto(null)
      setAnimalIdActual(null)
      setFormulario(formularioInicial)
      setErrores({})
      const aviso = resultado.advertencias.includes('ARETE_COINCIDENTE')
        ? ' Revisa el arete: coincide con otro registro de esta ganadería.' : ''
      setMensaje(`Animal registrado correctamente.${aviso}`)
      try {
        await cargarPrimeraPagina()
      } catch (error) {
        const errorAnimal = fallo(error)
        if (errorAnimal.tipo === 'sesion' || errorAnimal.tipo === 'permiso') aplicarFallo(errorAnimal)
        else setMensaje(`Animal registrado correctamente.${aviso} Actualiza el listado para verlo.`)
      }
    } catch (error) {
      const errorAnimal = fallo(error)
      if (errorAnimal.tipo === 'conexion' || errorAnimal.tipo === 'servicio'
          || errorAnimal.tipo === 'limite') {
        setPendienteIncierto(solicitud)
      } else {
        setPendienteIncierto(null)
        if (errorAnimal.tipo === 'conflicto') setAnimalIdActual(null)
      }
      aplicarFallo(errorAnimal)
    } finally {
      envioEnCurso.current = false
      setEnviando(false)
    }
  }

  async function actualizarLista() {
    setCargando(true)
    setMensaje(null)
    try {
      const contexto = await consultarAccesoGanaderia(ganaderia.id, sesion.accessToken)
      setAcceso(contexto)
      await cargarPrimeraPagina()
    } catch (error) {
      if (error instanceof ErrorGanaderia) {
        const tipo = error.tipo === 'sesion' ? 'sesion'
          : error.tipo === 'seguridad' ? 'permiso' : 'servicio'
        aplicarFallo(new ErrorAnimal(tipo))
      } else {
        aplicarFallo(fallo(error))
      }
    } finally {
      setCargando(false)
    }
  }

  async function cargarMas() {
    if (!cursor || cargandoMas) return
    setCargandoMas(true)
    setMensaje(null)
    try {
      const pagina = await listarAnimales(ganaderia.id, sesion.accessToken, cursor)
      setAnimales(actuales => {
        const porId = new Map(actuales.map(animal => [animal.animalId, animal]))
        pagina.items.forEach(animal => porId.set(animal.animalId, animal))
        return Array.from(porId.values())
      })
      setCursor(pagina.siguienteCursor)
    } catch (error) {
      aplicarFallo(fallo(error))
    } finally {
      setCargandoMas(false)
    }
  }

  async function abrirFicha(animalId: string) {
    setCargandoFicha(true)
    setMensaje(null)
    try {
      setFicha(await consultarAnimal(ganaderia.id, animalId, sesion.accessToken))
    } catch (error) {
      aplicarFallo(fallo(error))
    } finally {
      setCargandoFicha(false)
    }
  }

  const puedeRegistrar = acceso?.rol === 'PROPIETARIO' || acceso?.rol === 'EDITOR'
  const camposBloqueados = enviando || Boolean(pendienteIncierto)

  return (
    <section className="animals-panel" id="mis-animales" aria-labelledby="animals-title">
      <div className="animals-heading">
        <div>
          <p className="eyebrow"><span className="small-rule" /> MI GANADO</p>
          <h3 id="animals-title">Mis animales</h3>
          <p>{ganaderia.nombre}</p>
        </div>
        {acceso && <span className="access-badge">Acceso: {acceso.rol.toLowerCase()}</span>}
      </div>

      {mensaje && <div className="form-notice animals-notice" role="status">{mensaje}</div>}
      {cargando ? (
        <p className="animals-loading" role="status">Verificando acceso y consultando animales…</p>
      ) : !acceso ? (
        <div className="animals-empty"><p>No hay acceso vigente para mostrar esta ganadería.</p></div>
      ) : (
        <div className="animals-layout">
          <div className="animals-list-column">
            <div className="animals-list-heading">
              <h4>Animales registrados</h4>
              <button className="button button-outline" type="button" onClick={() => void actualizarLista()}>
                Actualizar
              </button>
            </div>
            {animales.length === 0 ? (
              <div className="animals-empty"><p>Aún no hay animales registrados en esta ganadería.</p></div>
            ) : (
              <div className="animal-list" aria-label="Animales de la ganadería">
                {animales.map(animal => (
                  <button className="animal-option" type="button" key={animal.animalId}
                    onClick={() => void abrirFicha(animal.animalId)}>
                    <span><strong>{tituloAnimal(animal)}</strong><small>{descripcionAnimal(animal)}</small></span>
                    <span aria-hidden="true">→</span>
                  </button>
                ))}
              </div>
            )}
            {cursor && (
              <button className="button button-outline load-more" type="button"
                onClick={() => void cargarMas()} disabled={cargandoMas}>
                {cargandoMas ? 'Cargando…' : 'Cargar más'}
              </button>
            )}
          </div>

          {puedeRegistrar ? (
            <form className="animal-form" onSubmit={registrar} noValidate aria-busy={enviando}>
              <div className="animal-form-heading">
                <h4>Registrar un animal</h4>
                <p>Solo el sexo es obligatorio. Los identificadores técnicos se generan de forma segura.</p>
              </div>
              <div className="form-field">
                <label htmlFor="animal-sexo">Sexo *</label>
                <select id="animal-sexo" value={formulario.sexo}
                  onChange={event => cambiarSexo(event.target.value as '' | SexoAnimal)}
                  disabled={camposBloqueados} aria-invalid={Boolean(errores.sexo)}
                  aria-describedby={errores.sexo ? 'animal-error-sexo' : undefined}>
                  <option value="">Selecciona una opción</option>
                  <option value="HEMBRA">Hembra</option>
                  <option value="MACHO">Macho</option>
                </select>
                {errores.sexo && <span className="field-error" id="animal-error-sexo">{errores.sexo}</span>}
              </div>
              <div className="animal-fields-grid">
                <CampoTexto id="animal-nombre" etiqueta="Nombre" valor={formulario.nombre}
                  maximo={100} error={errores.nombre} disabled={camposBloqueados}
                  onChange={valor => cambiar('nombre', valor)} />
                <CampoTexto id="animal-raza" etiqueta="Raza" valor={formulario.raza}
                  maximo={100} error={errores.raza} disabled={camposBloqueados}
                  onChange={valor => cambiar('raza', valor)} />
                <CampoTexto id="animal-arete-frente" etiqueta="Arete frontal" valor={formulario.areteFrente}
                  maximo={100} error={errores.areteFrente} disabled={camposBloqueados}
                  onChange={valor => cambiar('areteFrente', valor)} />
                <CampoTexto id="animal-arete-reverso" etiqueta="Arete reverso" valor={formulario.areteReverso}
                  maximo={100} error={errores.areteReverso} disabled={camposBloqueados}
                  onChange={valor => cambiar('areteReverso', valor)} />
              </div>
              <div className="animal-fields-grid">
                <div className="form-field">
                  <label htmlFor="animal-fecha">Fecha de nacimiento</label>
                  <input id="animal-fecha" type="date" value={formulario.fechaNacimiento}
                    max={fechaHoyLima()} disabled={camposBloqueados}
                    onChange={event => cambiar('fechaNacimiento', event.target.value)}
                    aria-invalid={Boolean(errores.fechaNacimiento)}
                    aria-describedby={errores.fechaNacimiento ? 'animal-error-fecha' : undefined} />
                  {errores.fechaNacimiento
                    && <span className="field-error" id="animal-error-fecha">{errores.fechaNacimiento}</span>}
                </div>
                <div className="form-field">
                  <label htmlFor="animal-categoria">Categoría</label>
                  <select id="animal-categoria" value={formulario.categoria}
                    onChange={event => cambiar('categoria', event.target.value as '' | CategoriaAnimal)}
                    disabled={camposBloqueados || !formulario.sexo}
                    aria-invalid={Boolean(errores.categoria)}>
                    <option value="">Sin categoría</option>
                    {formulario.sexo && categoriasPorSexo[formulario.sexo].map(categoria => (
                      <option value={categoria} key={categoria}>{etiquetasCategoria[categoria]}</option>
                    ))}
                  </select>
                  {!formulario.sexo && <span className="field-help">Selecciona primero el sexo.</span>}
                </div>
              </div>
              {formulario.fechaNacimiento && (
                <label className="checkbox-field">
                  <input type="checkbox" checked={formulario.nacimientoEstimado}
                    onChange={event => cambiar('nacimientoEstimado', event.target.checked)}
                    disabled={camposBloqueados} />
                  La fecha de nacimiento es aproximada
                </label>
              )}
              <div className="form-field">
                <label htmlFor="animal-observaciones">Observaciones</label>
                <textarea id="animal-observaciones" value={formulario.observaciones}
                  rows={3} disabled={camposBloqueados}
                  onChange={event => cambiar('observaciones', event.target.value)}
                  aria-invalid={Boolean(errores.observaciones)}
                  aria-describedby={errores.observaciones
                    ? 'animal-observaciones-error animal-observaciones-contador'
                    : 'animal-observaciones-contador'} />
                <span className="field-help" id="animal-observaciones-contador">
                  {Array.from(formulario.observaciones).length}/2000 caracteres
                </span>
                {errores.observaciones
                  && <span className="field-error" id="animal-observaciones-error">{errores.observaciones}</span>}
              </div>
              {pendienteIncierto && (
                <p className="retry-note">No se generarán identificadores nuevos: este botón reenvía la misma operación.</p>
              )}
              <button className="button button-primary submit-button" type="submit" disabled={enviando}>
                {enviando ? 'Registrando…' : pendienteIncierto ? 'Reintentar mismo registro' : 'Registrar animal'}
              </button>
            </form>
          ) : (
            <div className="reader-note">
              <strong>Acceso de lectura</strong>
              <p>Puedes consultar las fichas, pero este rol no permite registrar animales.</p>
            </div>
          )}
        </div>
      )}

      {(cargandoFicha || ficha) && (
        <div className="animal-detail" aria-live="polite">
          {cargandoFicha ? <p role="status">Consultando ficha…</p> : ficha && (
            <>
              <div className="animal-detail-heading">
                <div><p className="eyebrow">FICHA INDIVIDUAL</p><h4>{tituloAnimal(ficha)}</h4></div>
                <button className="button button-outline" type="button" onClick={() => setFicha(null)}>Cerrar</button>
              </div>
              <dl>
                <div><dt>Sexo</dt><dd>{ficha.sexo === 'HEMBRA' ? 'Hembra' : 'Macho'}</dd></div>
                <div><dt>Categoría</dt><dd>{ficha.categoria ? etiquetasCategoria[ficha.categoria] : 'No indicada'}</dd></div>
                <div><dt>Raza</dt><dd>{ficha.raza || 'No indicada'}</dd></div>
                <div><dt>Arete frontal</dt><dd>{ficha.areteFrente || 'No indicado'}</dd></div>
                <div><dt>Arete reverso</dt><dd>{ficha.areteReverso || 'No indicado'}</dd></div>
                <div><dt>Nacimiento</dt><dd>{ficha.fechaNacimiento || 'No indicado'}{ficha.nacimientoEstimado ? ' (aproximado)' : ''}</dd></div>
                <div><dt>Estado</dt><dd>Activo</dd></div>
                <div><dt>Versión</dt><dd>{ficha.version}</dd></div>
              </dl>
              {ficha.observaciones && <p className="animal-observations"><strong>Observaciones</strong>{ficha.observaciones}</p>}
            </>
          )}
        </div>
      )}
    </section>
  )
}

type CampoTextoProps = {
  id: string
  etiqueta: string
  valor: string
  maximo: number
  error?: string
  disabled: boolean
  onChange: (valor: string) => void
}

function CampoTexto({ id, etiqueta, valor, maximo, error, disabled, onChange }: CampoTextoProps) {
  const ayudaId = `${id}-contador`
  return (
    <div className="form-field">
      <label htmlFor={id}>{etiqueta}</label>
      <input id={id} value={valor} disabled={disabled}
        onChange={event => onChange(event.target.value)} aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error ${ayudaId}` : ayudaId} />
      <span className="field-help" id={ayudaId}>{Array.from(valor).length}/{maximo} caracteres</span>
      {error && <span className="field-error" id={`${id}-error`}>{error}</span>}
    </div>
  )
}
