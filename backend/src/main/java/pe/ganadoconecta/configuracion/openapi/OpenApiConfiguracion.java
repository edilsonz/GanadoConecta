package pe.ganadoconecta.configuracion.openapi;

import static pe.ganadoconecta.configuracion.openapi.OperacionesApi.CONTRATOS;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import pe.ganadoconecta.configuracion.SeguridadConfiguracion;
import pe.ganadoconecta.modulos.analisis.aplicacion.ConsultarAsistente;

@Configuration(proxyBeanMethods = false)
@Profile(SeguridadConfiguracion.PERFIL_OPENAPI)
class OpenApiConfiguracion {
    private static final String JSON = "application/json";
    private static final String JWT = "sesionJWT";
    private static final String CSRF = "cookieCSRF";
    private static final String RENOVACION = "cookieRenovacion";
    private static final String ESQUEMA_ERROR = "#/components/schemas/ErrorDeSeguridad";
    private static final Set<String> EMITEN_COOKIE = Set.of("CsrfControlador.obtener",
            "AutenticacionControlador.login", "AutenticacionControlador.renovar", "AutenticacionControlador.logout");
    private static final Set<String> ERRORES_CON_CAMPOS = Set.of(
            "RegistroUsuarioControlador", "AutenticacionControlador", "GanaderiasControlador",
            "AnimalesControlador", "ConsultasAnimalesControlador", "VacunacionesControlador",
            "HistorialSanitarioControlador", "AgendaSanitariaControlador", "ReproduccionControlador",
            "PartosControlador", "GenealogiaControlador");

    @Bean
    OpenAPI contratoApi() {
        return new OpenAPI()
                .info(new Info().title("GanadoConecta · API REST").version("v1")
                        .description("API de identidad y gestión ganadera. Las escrituras conservan la validación de origen, "
                                + "CSRF y permisos del servidor. Los identificadores y las credenciales se suministran en tiempo de ejecución."))
                .addServersItem(new Server().url("/"))
                .components(new Components()
                        .addSecuritySchemes(JWT, new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                                .bearerFormat("JWT").description("Token de acceso vinculado a una sesión vigente."))
                        .addSecuritySchemes(CSRF, cookie("__Host-GC-CSRF", "Se emite en GET /api/v1/auth/csrf."))
                        .addSecuritySchemes(RENOVACION, cookie("__Host-GC-Refresh", "Cookie HttpOnly de renovación."))
                        .addSchemas("ErrorDeSeguridad", new ObjectSchema()
                                .addProperty("codigo", new StringSchema())
                                .addProperty("mensajeSeguro", new StringSchema())
                                .addProperty("correlacionId", new UUIDSchema())));
    }

    @Bean
    OperationCustomizer documentarOperaciones() {
        return (operacion, handler) -> {
            String clase = handler.getBeanType().getSimpleName();
            String clave = clase + "." + handler.getMethod().getName();
            var contrato = CONTRATOS.get(clave);
            if (contrato == null) {
                throw new IllegalStateException("Falta describir la operación REST " + clave);
            }
            operacion.setOperationId(clave.replace('.', '_'));
            operacion.setTags(java.util.List.of(contrato.modulo()));
            operacion.summary(contrato.resumen()).description(contrato.detalle() + " " + contrato.acceso().descripcion);
            ocultarArgumentosInternos(operacion, handler);
            aplicarSeguridad(operacion, contrato.acceso(), clave, escritura(handler));
            ajustarCuerpos(operacion, clave);
            aplicarCodigos(operacion, contrato, clase, clave, escritura(handler));
            return operacion;
        };
    }

    @Bean
    OpenApiCustomizer documentarSalud() {
        return api -> {
            var estado = new ObjectSchema().addProperty("status", new StringSchema());
            var respuestas = new ApiResponses()
                    .addApiResponse("200", new ApiResponse().description("Servicio disponible.").content(cuerpo(JSON, estado)))
                    .addApiResponse("503", new ApiResponse().description("Servicio no disponible.").content(cuerpo(JSON, estado)));
            api.path("/api/health", new PathItem().get(new Operation()
                    .operationId("Salud_consultar").addTagsItem("Operación")
                    .summary("Consultar salud del servicio")
                    .description("Actuator informa el estado general sin componentes ni detalles. Con el perfil postgres se verifica la conexión.")
                    .responses(respuestas)));
        };
    }

    private static SecurityScheme cookie(String nombre, String descripcion) {
        return new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE)
                .name(nombre).description(descripcion);
    }

    private static boolean escritura(HandlerMethod handler) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), RequestMapping.class);
        return mapping != null && Arrays.stream(mapping.method()).anyMatch(m -> m != RequestMethod.GET);
    }

    private static void ocultarArgumentosInternos(Operation operacion, HandlerMethod handler) {
        if (operacion.getParameters() == null) return;
        for (var argumento : handler.getMethodParameters()) {
            var anotacion = argumento.getParameterAnnotation(RequestParam.class);
            boolean mapaDeConsulta = Map.class.isAssignableFrom(argumento.getParameterType())
                    && anotacion != null && anotacion.name().isEmpty() && anotacion.value().isEmpty();
            if (argumento.hasParameterAnnotation(AuthenticationPrincipal.class)
                    || argumento.getParameterType() == CsrfToken.class || mapaDeConsulta) {
                String nombre = argumento.getParameter().getName();
                operacion.getParameters().removeIf(p -> nombre.equals(p.getName()));
            }
        }
        operacion.getParameters().forEach(p -> {
            if ("path".equals(p.getIn()) && p.getDescription() == null) {
                p.setDescription("Identificador o clave dentro del ámbito indicado por la ruta.");
            }
            if ("cursor".equals(p.getName())) p.setDescription("Cursor opaco emitido en la página anterior.");
        });
    }

    private static void aplicarSeguridad(Operation operacion, OperacionesApi.Acceso acceso,
            String clave, boolean escritura) {
        SecurityRequirement seguridad = new SecurityRequirement();
        if (acceso != OperacionesApi.Acceso.PUBLICO) seguridad.addList(JWT);
        if (escritura) {
            seguridad.addList(CSRF);
            operacion.addParametersItem(new Parameter().name("X-CSRF-TOKEN").in("header").required(true)
                    .schema(new StringSchema())
                    .description("Token de GET /api/v1/auth/csrf, asociado a la cookie del mismo navegador."));
            operacion.addParametersItem(new Parameter().name("Origin").in("header")
                    .schema(new StringSchema())
                    .description("Debe corresponder al origen configurado. Si falta Origin, se valida el encabezado Referer."));
        }
        if ("AutenticacionControlador.renovar".equals(clave)) seguridad.addList(RENOVACION);
        if (!seguridad.isEmpty()) operacion.addSecurityItem(seguridad);
    }

    private static void ajustarCuerpos(Operation operacion, String clave) {
        switch (clave) {
            case "AsistenteControlador.consultar" -> operacion.requestBody(new RequestBody().required(true)
                    .content(cuerpo(JSON, new ObjectSchema()
                            .addProperty("pregunta", new StringSchema().minLength(1).maxLength(400))
                            .addProperty("tema", new StringSchema()._enum(Arrays.stream(ConsultarAsistente.Tema.values())
                                    .map(Enum::name).toList()))
                            .addProperty("desde", new DateSchema())
                            .addProperty("hasta", new DateSchema())
                            .addProperty("animalId", new UUIDSchema())
                            .required(java.util.List.of("pregunta", "tema", "desde", "hasta")))));
            case "CatalogoControlador.cargarFoto" -> operacion.requestBody(new RequestBody().required(true)
                    .content(cuerpo("image/jpeg", new BinarySchema())
                            .addMediaType("image/png", new MediaType().schema(new BinarySchema()))));
            default -> { }
        }
    }

    private static void aplicarCodigos(Operation op, OperacionesApi.Contrato contrato, String clase,
            String clave, boolean escritura) {
        ApiResponses inferidas = op.getResponses();
        Content exito = inferidas.values().stream().map(ApiResponse::getContent)
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (exito != null && exito.containsKey("*/*")) {
            exito.addMediaType(JSON, exito.remove("*/*"));
        }
        if (op.getRequestBody() != null && op.getRequestBody().getContent().containsKey("*/*")) {
            Content entrada = op.getRequestBody().getContent();
            entrada.addMediaType(JSON, entrada.remove("*/*"));
        }
        ApiResponses finales = new ApiResponses();
        for (String estado : contrato.estados().split(" ")) {
            ApiResponse respuesta = new ApiResponse().description(descripcion(estado));
            if ("200".equals(estado) || "201".equals(estado)) {
                respuesta.content("CatalogoControlador.foto".equals(clave) ? cuerpo("image/jpeg", new BinarySchema()) : exito);
            } else if (estado.startsWith("4") || estado.startsWith("5")) {
                if (ERRORES_CON_CAMPOS.contains(clase) && !"415".equals(estado)) {
                    respuesta.content(cuerpo(JSON, new ObjectSchema().addProperty("codigo", new StringSchema())
                            .addProperty("mensajeSeguro", new StringSchema())
                            .addProperty("erroresPorCampo", new MapSchema().additionalProperties(new StringSchema()))
                            .addProperty("correlacionId", new UUIDSchema())));
                }
                if ("PesosControlador.registrar".equals(clave) && "409".equals(estado)) respuesta.content(exito);
            }
            if (estado.startsWith("2") && EMITEN_COOKIE.contains(clave)) {
                respuesta.addHeaderObject("Set-Cookie", new Header().schema(new StringSchema()).description(
                        "Cookie de seguridad HttpOnly, Secure, SameSite=Strict, Path=/ y sin Domain. "
                        + "En el cierre de sesión se elimina la cookie de renovación."));
            }
            finales.addApiResponse(estado, respuesta);
        }
        if (contrato.acceso() != OperacionesApi.Acceso.PUBLICO) {
            finales.addApiResponse("401", new ApiResponse().description(descripcion("401"))
                    .content(cuerpo(JSON, new Schema<>().$ref(ESQUEMA_ERROR))));
        }
        if (escritura || contrato.acceso() != OperacionesApi.Acceso.PUBLICO) {
            finales.addApiResponse("403", new ApiResponse().description(
                    "Permiso insuficiente o solicitud rechazada por CSRF/origen. Los filtros de seguridad devuelven codigo y mensaje. "
                    + "El cuerpo de rechazos generados por los controladores depende del módulo."));
        }
        if (escritura) finales.putIfAbsent("413", new ApiResponse().description(descripcion("413")));
        if (op.getRequestBody() != null) finales.putIfAbsent("415", new ApiResponse().description(descripcion("415")));
        op.responses(finales);
    }

    private static final Map<String, String> ESTADOS = Map.ofEntries(
            Map.entry("200", "Operación correcta o reenvío confirmado, cuando aplica."),
            Map.entry("201", "Registro creado."),
            Map.entry("204", "Operación correcta sin cuerpo."),
            Map.entry("400", "Formato, parámetros o cuerpo de la solicitud inválidos."),
            Map.entry("401", "Credenciales, token o sesión ausentes o inválidos."),
            Map.entry("404", "Recurso no disponible en el ámbito solicitado."),
            Map.entry("409", "Conflicto de estado, unicidad, versión o idempotencia."),
            Map.entry("413", "El cuerpo supera el tamaño aceptado."),
            Map.entry("415", "Formato del cuerpo no admitido."),
            Map.entry("422", "Datos que incumplen una regla del recurso."),
            Map.entry("429", "Límite de solicitudes o capacidad de procesamiento alcanzado."),
            Map.entry("503", "Servicio o dependencia temporalmente no disponible."));

    private static String descripcion(String estado) { return ESTADOS.get(estado); }

    private static Content cuerpo(String tipo, Schema<?> schema) {
        return new Content().addMediaType(tipo, new MediaType().schema(schema));
    }
}
