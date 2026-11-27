package pe.ganadoconecta.configuracion.openapi;

import java.util.Map;

import static java.util.Map.entry;

final class OperacionesApi {
    private static final String IDENTIDAD = "Identidad y sesiones";
    private static final String GANADERIAS = "Ganaderías";
    private static final String FAMILIA = "Acceso familiar";
    private static final String GANADO = "Animales y peso";
    private static final String SANIDAD = "Sanidad y calendario";
    private static final String REPRODUCCION = "Reproducción y genealogía";
    private static final String CATALOGO = "Catálogo público";
    private static final String ANUNCIOS = "Anuncios y ventas";
    private static final String MODERACION = "Moderación";
    private static final String ADMIN = "Administración";
    private static final String REPORTES = "Reportes";
    private static final String ANALISIS = "Análisis, alertas y asistente";

    enum Acceso {
        PUBLICO("Acceso público."),
        SESION("Sesión de usuario vigente."),
        LECTURA("Membresía activa de propietario, editor o lector de la ganadería."),
        ESCRITURA("Membresía activa de propietario o editor de la ganadería."),
        PROPIETARIO("Solo el propietario vigente de la ganadería."),
        ADMIN("Permiso activo de administración de plataforma, independiente de moderación."),
        MODERADOR("Permiso activo de moderación de catálogo, independiente de administración.");

        final String descripcion;
        Acceso(String descripcion) { this.descripcion = descripcion; }
    }

    record Contrato(String modulo, Acceso acceso, String resumen, String detalle, String estados) {}

    private static Map.Entry<String, Contrato> op(String metodo, String modulo, Acceso acceso,
            String resumen, String detalle, String estados) {
        return entry(metodo, new Contrato(modulo, acceso, resumen, detalle, estados));
    }

    static final Map<String, Contrato> CONTRATOS = Map.ofEntries(
        op("CsrfControlador.obtener", IDENTIDAD, Acceso.PUBLICO, "Obtener CSRF",
            "Devuelve el token CSRF y establece la cookie protegida. Usa ambos valores en la misma sesión del navegador.", "200"),
        op("RegistroUsuarioControlador.registrar", IDENTIDAD, Acceso.PUBLICO, "Crear cuenta",
            "Correo normalizado y contraseña de 15 a 128 puntos de código Unicode. El cuerpo no admite roles ni ganaderías.", "201 400 409 422 429 503"),
        op("AutenticacionControlador.login", IDENTIDAD, Acceso.PUBLICO, "Iniciar sesión",
            "Verifica credenciales y emite un JWT de 10 minutos y una cookie de renovación. La sesión online dura hasta 24 horas.", "200 400 401 429 503"),
        op("AutenticacionControlador.renovar", IDENTIDAD, Acceso.PUBLICO, "Rotar la sesión",
            "Presenta la cookie de renovación vigente. Devuelve un JWT nuevo y rota la cookie. Respeta la caducidad absoluta de la sesión.", "200 401 503"),
        op("AutenticacionControlador.logout", IDENTIDAD, Acceso.PUBLICO, "Cerrar sesión",
            "Revoca la sesión identificada por la cookie si está disponible y devuelve la eliminación de la cookie.", "204 503"),
        op("AutenticacionControlador.me", IDENTIDAD, Acceso.SESION, "Consultar identidad",
            "Devuelve el identificador y estado de la cuenta autenticada.", "200"),
        op("GanaderiasControlador.crear", GANADERIAS, Acceso.SESION, "Crear una ganadería",
            "Usa UUIDv4 proporcionado por el cliente y un nombre válido. Crea atómicamente la membresía inicial de propietario.", "200 201 400 409 422 503"),
        op("GanaderiasControlador.listar", GANADERIAS, Acceso.SESION, "Listar ganaderías accesibles",
            "Solo incluye ganaderías activas con membresía vigente.", "200 503"),
        op("GanaderiasControlador.consultarAcceso", GANADERIAS, Acceso.LECTURA, "Consultar acceso",
            "Devuelve la autorización y el rol vigentes en PostgreSQL.", "200 400 503"),
        op("GanaderiasControlador.revocarMembresia", GANADERIAS, Acceso.PROPIETARIO, "Revocar una membresía",
            "Revoca un editor o lector. La membresía del titular no se puede revocar aquí.", "204 400 404 409 503"),
        op("AccesoFamiliarControlador.consultar", FAMILIA, Acceso.PROPIETARIO, "Consultar integrantes e invitaciones",
            "Proyección privada de integrantes e invitaciones de la ganadería.", "200 400"),
        op("AccesoFamiliarControlador.emitir", FAMILIA, Acceso.PROPIETARIO, "Emitir invitación",
            "Vincula el código al correo destinatario y al rol EDITOR o LECTOR. Entrega el código al propietario por única vez.", "201 400 409"),
        op("AccesoFamiliarControlador.cancelar", FAMILIA, Acceso.PROPIETARIO, "Cancelar invitación",
            "Cancela una invitación pendiente de su ganadería.", "204 400 409"),
        op("AccesoFamiliarControlador.cambiarRol", FAMILIA, Acceso.PROPIETARIO, "Cambiar rol familiar",
            "Permite alternar entre EDITOR y LECTOR. El titular permanece como propietario.", "204 400 409"),
        op("AccesoFamiliarControlador.aceptar", FAMILIA, Acceso.SESION, "Aceptar invitación",
            "Solo la cuenta destinataria puede aceptar un código vigente.", "200 400 409"),
        op("AnimalesControlador.registrar", GANADO, Acceso.ESCRITURA, "Registrar animal individual",
            "Usa UUIDv4 para operación y animal, sexo MACHO o HEMBRA y los campos opcionales de SPEC-01. Un reenvío idéntico devuelve el mismo comprobante.", "200 201 400 409 422 503"),
        op("ConsultasAnimalesControlador.listar", GANADO, Acceso.LECTURA, "Listar animales",
            "Paginación por cursor opaco. Usa siguienteCursor en la consulta siguiente de la misma ganadería y conserva el tamaño solicitado.", "200 400 503"),
        op("ConsultasAnimalesControlador.consultarFicha", GANADO, Acceso.LECTURA, "Consultar ficha animal",
            "Devuelve la ficha del animal dentro del ámbito de ganadería solicitado.", "200 400 404 503"),
        op("PesosControlador.listar", GANADO, Acceso.LECTURA, "Consultar historial de peso",
            "Devuelve las mediciones y el último peso individual.", "200 400 404"),
        op("PesosControlador.registrar", GANADO, Acceso.ESCRITURA, "Registrar un pesaje",
            "Kilogramos positivos, hasta tres decimales y fecha de medición válida. Conserva los UUID en los reintentos.", "200 201 400 404 409 422"),
        op("VentasControlador.registrar", ANUNCIOS, Acceso.PROPIETARIO, "Registrar venta",
            "Marca el animal como vendido y cierra sus anuncios en una transacción. Conserva los antecedentes y permite reenvío idempotente.", "200 201 400 404 409 422"),
        op("VacunacionesControlador.listar", SANIDAD, Acceso.LECTURA, "Listar vacunaciones",
            "Consulta privada del historial de vacunación del animal.", "200 400 404 503"),
        op("VacunacionesControlador.registrar", SANIDAD, Acceso.ESCRITURA, "Registrar vacunación",
            "Fecha de aplicación y nombre de vacuna informados por el usuario. Conserva los identificadores en reintentos.", "200 201 400 404 409 422 503"),
        op("HistorialSanitarioControlador.listar", SANIDAD, Acceso.LECTURA, "Listar historial sanitario",
            "Consulta cronológica del animal, con filtro opcional por tipo.", "200 400 404 503"),
        op("HistorialSanitarioControlador.registrar", SANIDAD, Acceso.ESCRITURA, "Registrar evento sanitario",
            "Admite los tipos de control definidos en SPEC-02. La fecha y la descripción corresponden al registro ingresado. Envío idempotente.", "200 201 400 404 409 422 503"),
        op("AgendaSanitariaControlador.listar", SANIDAD, Acceso.LECTURA, "Listar calendario",
            "Filtros opcionales por animal, tipo y estado. La fecha de negocio se evalúa en America/Lima.", "200 400 503"),
        op("AgendaSanitariaControlador.crear", SANIDAD, Acceso.ESCRITURA, "Programar actividad o campaña",
            "Informa tipo, fecha prevista, descripción, y animal o participantes según la actividad.", "201 400 404 409 422 503"),
        op("AgendaSanitariaControlador.completar", SANIDAD, Acceso.ESCRITURA, "Completar actividad o confirmar atenciones",
            "El cierre ordinario no genera evento clínico. Las campañas solo registran atenciones para participantes explícitamente confirmados.", "200 400 404 409 422 503"),
        op("ReproduccionControlador.listar", REPRODUCCION, Acceso.LECTURA, "Listar eventos reproductivos",
            "Monta, inseminación y evaluaciones de gestación registradas para el animal.", "200 400 404 503"),
        op("ReproduccionControlador.registrar", REPRODUCCION, Acceso.ESCRITURA, "Registrar evento reproductivo",
            "Evento de una hembra. Resultado de evaluación y fechas provienen de lo informado por el usuario. Registro idempotente.", "200 201 400 404 409 422 503"),
        op("PartosControlador.listar", REPRODUCCION, Acceso.LECTURA, "Listar partos",
            "Devuelve los partos y crías asociadas de la madre indicada.", "200 400 404 503"),
        op("PartosControlador.registrar", REPRODUCCION, Acceso.ESCRITURA, "Registrar parto",
            "Indica resultado, fecha y los UUID de crías de la misma ganadería cuando corresponda. Operación idempotente.", "200 201 400 404 409 422 503"),
        op("GenealogiaControlador.consultar", REPRODUCCION, Acceso.LECTURA, "Consultar genealogía",
            "Devuelve relaciones y antecedentes privados permitidos del animal.", "200 400 404 503"),
        op("GenealogiaControlador.corregir", REPRODUCCION, Acceso.ESCRITURA, "Corregir parentesco",
            "El tipo identifica madre o padre. La corrección idempotente se audita y rechaza ciclos y cruces de ganadería.", "200 201 400 404 409 422 503"),
        op("CatalogoControlador.buscar", CATALOGO, Acceso.PUBLICO, "Buscar anuncios públicos",
            "Solo anuncios activos y datos autorizados. Filtros por categoría, sexo, raza, ubicación y precios PEN. Página inicial 1; siguientePagina indica continuidad.", "200 400 422 503"),
        op("CatalogoControlador.foto", CATALOGO, Acceso.PUBLICO, "Consultar foto pública",
            "Devuelve JPEG si la foto pertenece a un anuncio actualmente público.", "200 400 404 503"),
        op("CatalogoControlador.privados", ANUNCIOS, Acceso.PROPIETARIO, "Listar anuncios privados",
            "Devuelve los anuncios del titular asociados al animal.", "200 400 404"),
        op("CatalogoControlador.crear", ANUNCIOS, Acceso.PROPIETARIO, "Crear anuncio",
            "Usa los datos autorizados de la ficha, precio PEN y los consentimientos explícitos de publicación.", "201 400 404 409 422"),
        op("CatalogoControlador.editar", ANUNCIOS, Acceso.PROPIETARIO, "Editar anuncio",
            "Usa la versión vigente del anuncio y el conjunto de datos autorizado.", "200 400 404 409 422"),
        op("CatalogoControlador.estado", ANUNCIOS, Acceso.PROPIETARIO, "Cambiar disponibilidad del anuncio",
            "La acción y versión deben respetar el estado del anuncio y animal. Aplican las transiciones aprobadas en SPEC-08.", "200 400 404 409 422"),
        op("CatalogoControlador.cargarFoto", ANUNCIOS, Acceso.PROPIETARIO, "Añadir foto a un anuncio",
            "Envía bytes JPEG o PNG, no multipart, y la versión vigente como parámetro. La fotografía se guarda a través del adaptador autorizado.", "201 400 404 409 413 415 422 503"),
        op("ModeracionControlador.disponible", MODERACION, Acceso.PUBLICO, "Consultar disponibilidad de reportes",
            "Expone si se habilitó la recepción experimental autenticada, desactivada de forma predeterminada.", "200"),
        op("ModeracionControlador.reportar", MODERACION, Acceso.SESION, "Reportar anuncio",
            "Requiere habilitación de la recepción experimental, sesión y motivo válido. La disponibilidad se confirma en la operación anterior.", "201 400 404 409 422"),
        op("ModeracionControlador.bandeja", MODERACION, Acceso.MODERADOR, "Consultar bandeja de moderación",
            "Lista los reportes para el rol de moderación de plataforma.", "200"),
        op("ModeracionControlador.revisar", MODERACION, Acceso.MODERADOR, "Revisar reporte",
            "Registra una decisión de revisión auditada. La retirada elimina la disponibilidad pública del anuncio.", "204 400 404 409 422"),
        op("AdministracionControlador.acceso", ADMIN, Acceso.SESION, "Consultar permiso administrativo propio",
            "Indica si la sesión dispone del permiso administrativo vigente.", "200 503"),
        op("AdministracionControlador.usuarios", ADMIN, Acceso.ADMIN, "Buscar cuentas",
            "Permite buscar por correo completo o UUID y filtrar por estado. Continuación por cursor opaco. La consulta se audita.", "200 400 503"),
        op("AdministracionControlador.estadisticas", ADMIN, Acceso.ADMIN, "Consultar indicadores de plataforma",
            "Devuelve totales de cuentas y ganaderías, incluidas activas. La consulta se audita.", "200 503"),
        op("ReportesControlador.reporte", REPORTES, Acceso.LECTURA, "Consultar reportes ganaderos",
            "Período inclusivo en America/Lima y animal opcional. El historial de peso del reporte requiere un animal.", "200 400 503"),
        op("AnalisisControlador.analizar", ANALISIS, Acceso.LECTURA, "Consultar análisis descriptivo",
            "Resumen de registros confirmados con período inclusivo y animal opcional. Las estadísticas son deterministas.", "200 400 503"),
        op("AnalisisControlador.alertas", ANALISIS, Acceso.LECTURA, "Consultar alertas de calendario",
            "Actividades vigentes para hoy o vencidas. Se calculan respecto de America/Lima.", "200 400 503"),
        op("AsistenteControlador.consultar", ANALISIS, Acceso.LECTURA, "Consultar asistente",
            "Preguntas sobre registros autorizados. Tamaño máximo del cuerpo 4096 bytes. Devuelve respuesta con fuentes o información insuficiente.", "200 400 415 429 503")
    );

    private OperacionesApi() {}
}
