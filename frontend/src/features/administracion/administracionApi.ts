import { solicitudPrivada } from '../../shared/api/solicitudPrivada'

const ruta = '/api/v1/administracion'

export type EstadoCuenta = 'ACTIVO' | 'INACTIVO'
export type UsuarioAdministrado = { id: string; correo: string; estado: EstadoCuenta; creadoEn: string }
export type PaginaUsuarios = { items: UsuarioAdministrado[]; siguienteCursor: string | null }
export type EstadisticasPlataforma = { cuentas: number; cuentasActivas: number; ganaderias: number; ganaderiasActivas: number }
export type FiltrosUsuarios = { buscar: string; estado: EstadoCuenta | ''; cursor?: string }

export async function consultarPermisoAdministrativo(token: string) {
  return (await solicitudPrivada<{ permitido: boolean }>(ruta + '/acceso', token)).permitido === true
}

export function listarUsuarios(token: string, filtros: FiltrosUsuarios) {
  const query = new URLSearchParams()
  for (const [nombre, valor] of Object.entries(filtros)) if (valor) query.set(nombre, valor)
  return solicitudPrivada<PaginaUsuarios>(ruta + '/usuarios?' + query, token)
}

export function consultarEstadisticasPlataforma(token: string) {
  return solicitudPrivada<EstadisticasPlataforma>(ruta + '/estadisticas', token)
}
