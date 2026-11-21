import type { CategoriaAnimal, SexoAnimal, SolicitudRegistroAnimal, UbicacionIdentificacionTextil } from './animalesApi'

export type FormularioAnimal = {
  sexo: '' | SexoAnimal
  nombre: string
  areteFrente: string
  areteReverso: string
  coloresIdentificacionTextil: string
  ubicacionIdentificacionTextil: '' | UbicacionIdentificacionTextil
  raza: string
  fechaNacimiento: string
  nacimientoEstimado: boolean
  categoria: '' | CategoriaAnimal
  observaciones: string
}

export const formularioInicial: FormularioAnimal = {
  sexo: '',
  nombre: '',
  areteFrente: '',
  areteReverso: '',
  coloresIdentificacionTextil: '',
  ubicacionIdentificacionTextil: '',
  raza: '',
  fechaNacimiento: '',
  nacimientoEstimado: false,
  categoria: '',
  observaciones: '',
}

export function formularioAnimalModificado(formulario: FormularioAnimal) {
  return formulario.sexo !== ''
    || formulario.nombre !== ''
    || formulario.raza !== ''
    || formulario.areteFrente !== ''
    || formulario.areteReverso !== ''
    || formulario.coloresIdentificacionTextil !== ''
    || formulario.ubicacionIdentificacionTextil !== ''
    || formulario.fechaNacimiento !== ''
    || formulario.nacimientoEstimado
    || formulario.categoria !== ''
    || formulario.observaciones !== ''
}

export const categoriasPorSexo = {
  HEMBRA: ['TERNERA', 'VAQUILLA', 'VACA'],
  MACHO: ['TERNERO', 'TORETE', 'TORO'],
} as const

export const etiquetasCategoria: Record<CategoriaAnimal, string> = {
  TERNERA: 'Ternera',
  VAQUILLA: 'Vaquilla',
  VACA: 'Vaca',
  TERNERO: 'Ternero',
  TORETE: 'Torete',
  TORO: 'Toro',
}

const controlesNoAdmitidos = /[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F-\u009F]|\p{Cf}/u

export function fechaHoyLima() {
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

export function validarFormulario(formulario: FormularioAnimal) {
  const errores: Record<string, string> = {}
  if (!formulario.sexo) errores.sexo = 'Selecciona MACHO o HEMBRA.'

  const textos = [
    ['nombre', formulario.nombre, 100, false],
    ['areteFrente', formulario.areteFrente, 100, false],
    ['areteReverso', formulario.areteReverso, 100, false],
    ['coloresIdentificacionTextil', formulario.coloresIdentificacionTextil, 100, false],
    ['raza', formulario.raza, 100, false],
    ['observaciones', formulario.observaciones, 2000, true],
  ] as const
  for (const [campo, valor, maximo, permiteSaltos] of textos) {
    const texto = normalizarOpcional(valor, permiteSaltos)
    if (texto && Array.from(texto).length > maximo) errores[campo] = `Usa como máximo ${maximo} caracteres.`
    if (texto && controlesNoAdmitidos.test(texto)) errores[campo] = 'El texto contiene caracteres no admitidos.'
  }
  const coloresTextiles = normalizarOpcional(formulario.coloresIdentificacionTextil)
  if (coloresTextiles && !formulario.ubicacionIdentificacionTextil) {
    errores.ubicacionIdentificacionTextil = 'Selecciona la oreja donde está la lana o cinta.'
  }
  if (!coloresTextiles && formulario.ubicacionIdentificacionTextil) {
    errores.coloresIdentificacionTextil = 'Indica los colores de la lana o cinta.'
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

export function crearSolicitud(
  formulario: FormularioAnimal,
  animalId: string,
): SolicitudRegistroAnimal {
  return {
    operationId: crypto.randomUUID(),
    animalId,
    tipo: 'CREAR_ANIMAL',
    schemaVersion: 2,
    baseVersion: 0,
    sexo: formulario.sexo as SexoAnimal,
    ...(normalizarOpcional(formulario.nombre) && { nombre: normalizarOpcional(formulario.nombre) }),
    ...(normalizarOpcional(formulario.areteFrente) && { areteFrente: normalizarOpcional(formulario.areteFrente) }),
    ...(normalizarOpcional(formulario.areteReverso) && { areteReverso: normalizarOpcional(formulario.areteReverso) }),
    ...(normalizarOpcional(formulario.coloresIdentificacionTextil) && {
      coloresIdentificacionTextil: normalizarOpcional(formulario.coloresIdentificacionTextil),
      ubicacionIdentificacionTextil: formulario.ubicacionIdentificacionTextil as UbicacionIdentificacionTextil,
    }),
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
