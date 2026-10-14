package com.udea.business;

import com.udea.business.entities.Negocio;
import com.udea.business.repositories.NegocioRepository;
import com.udea.business.repositories.RecursoRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre los criterios de aceptación de la HU "Registrar recursos físicos" de punta a punta:
 * filtro JWT, controlador, servicio y base de datos H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecursoIntegrationTest {

    private static final int PROPIETARIO_ID = 10;
    private static final int OTRO_PROPIETARIO_ID = 20;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NegocioRepository negocioRepository;

    @Autowired
    private RecursoRepository recursoRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private Integer spaArmonia;
    private Integer spaNorte;
    private Integer barberiaSur;

    @BeforeEach
    void setUp() {
        recursoRepository.deleteAll();
        negocioRepository.deleteAll();
        spaArmonia = crearNegocio("Spa Armonía", PROPIETARIO_ID, "900000001");
        spaNorte = crearNegocio("Spa Norte", PROPIETARIO_ID, "900000002");
        barberiaSur = crearNegocio("Barbería Sur", OTRO_PROPIETARIO_ID, "900000003");
    }

    private Integer crearNegocio(String nombre, int propietarioId, String nit) {
        return negocioRepository.save(Negocio.builder()
                .propietarioId(propietarioId)
                .nombreNegocio(nombre)
                .direccionNegocio("Calle 1")
                .identificacionFiscal(nit)
                .monedaCodigoIso("COP")
                .build()).getIdNegocio();
    }

    private String token(int usuarioId, List<String> roles, String rolActivo) {
        var builder = Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim("email", "ana.gomez@correo.com")
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000));
        if (rolActivo != null) {
            builder.claim("rolActivo", rolActivo);
        }
        return builder.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret))).compact();
    }

    private String tokenPropietario() {
        return token(PROPIETARIO_ID, List.of("Propietario"), null);
    }

    private ResultActions registrar(Integer negocioId, String token, String json) throws Exception {
        return mockMvc.perform(post("/api/v1/negocios/{id}/recursos", negocioId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions listar(Integer negocioId, String token) throws Exception {
        return mockMvc.perform(get("/api/v1/negocios/{id}/recursos", negocioId)
                .header("Authorization", "Bearer " + token));
    }

    private String recurso(String nombre, String tipo, int cantidad) {
        return """
                {"nombre": "%s", "tipo": "%s", "cantidad": %d}
                """.formatted(nombre, tipo, cantidad);
    }

    @Test
    void ca01_registroExitoso() throws Exception {
        registrar(spaArmonia, tokenPropietario(), recurso("Camilla de masajes", "Equipo", 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Recurso registrado correctamente."))
                .andExpect(jsonPath("$.recurso.negocioId").value(spaArmonia))
                .andExpect(jsonPath("$.recurso.estado").value("Activo"));

        listar(spaArmonia, tokenPropietario())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recursos", hasSize(1)))
                .andExpect(jsonPath("$.recursos[0].nombre").value("Camilla de masajes"))
                .andExpect(jsonPath("$.recursos[0].tipo").value("Equipo"))
                .andExpect(jsonPath("$.recursos[0].cantidad").value(3));
    }

    @Test
    void ca02_consultarNegocioSinRecursosRetornaListaVacia() throws Exception {
        listar(spaArmonia, tokenPropietario())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recursos").isArray())
                .andExpect(jsonPath("$.recursos").isEmpty())
                .andExpect(jsonPath("$.mensaje").value("Este negocio aún no tiene recursos registrados."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"camilla de masajes", "CAMILLA DE MASAJES", "Camilla De Masajes", "Cámilla de masajes"})
    void ca03_rechazaNombreDuplicadoEnElMismoNegocio(String nombre) throws Exception {
        registrar(spaArmonia, tokenPropietario(), recurso("Camilla de masajes", "Equipo", 3))
                .andExpect(status().isCreated());

        registrar(spaArmonia, tokenPropietario(), recurso(nombre, "Equipo", 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe un recurso con este nombre en el negocio."));

        assertEquals(1, recursoRepository.findByNegocioIdNegocioOrderByNombreAsc(spaArmonia).size());
    }

    @Test
    void ca04_permiteElMismoNombreEnOtroNegocioDelPropietario() throws Exception {
        registrar(spaArmonia, tokenPropietario(), recurso("Sala 1", "Espacio", 2)).andExpect(status().isCreated());

        registrar(spaNorte, tokenPropietario(), recurso("Sala 1", "Espacio", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recurso.negocioId").value(spaNorte));

        listar(spaArmonia, tokenPropietario())
                .andExpect(jsonPath("$.recursos", hasSize(1)))
                .andExpect(jsonPath("$.recursos[0].nombre").value("Sala 1"))
                .andExpect(jsonPath("$.recursos[0].cantidad").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "2.5", "-1", "1000", "\"abc\"", "null", "\"\""})
    void ca05_rechazaCantidadQueNoEsEnteroEntre1Y999(String cantidad) throws Exception {
        String json = """
                {"nombre": "Camilla", "tipo": "Equipo", "cantidad": %s}
                """.formatted(cantidad);

        registrar(spaArmonia, tokenPropietario(), json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La cantidad debe ser un número entero entre 1 y 999."));

        assertEquals(0, recursoRepository.count());
    }

    @Test
    void ca05_rechazaCantidadAusente() throws Exception {
        registrar(spaArmonia, tokenPropietario(), """
                {"nombre": "Camilla", "tipo": "Equipo"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La cantidad debe ser un número entero entre 1 y 999."));

        assertEquals(0, recursoRepository.count());
    }

    @Test
    void ca06_aislamientoEntreNegocios() throws Exception {
        registrar(barberiaSur, tokenPropietario(), recurso("Silla", "Equipo", 2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tienes permiso para gestionar los recursos de este negocio."));

        listar(barberiaSur, tokenPropietario())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tienes permiso para gestionar los recursos de este negocio."));

        assertEquals(0, recursoRepository.count());
    }

    @Test
    void ca07_laDescripcionEsOpcional() throws Exception {
        registrar(spaArmonia, tokenPropietario(), recurso("Sala 2", "Espacio", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Recurso registrado correctamente."))
                .andExpect(jsonPath("$.recurso.descripcion").doesNotExist());
    }

    @Test
    void ca07_guardaLaDescripcionCuandoSeEnvia() throws Exception {
        registrar(spaArmonia, tokenPropietario(), """
                {"nombre": "Sala 2", "tipo": "Espacio", "cantidad": 1, "descripcion": "Sala con ducha"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recurso.descripcion").value("Sala con ducha"));
    }

    @Test
    void ca08_cuentaConVariosRolesOperandoComoProveedorEsRechazada() throws Exception {
        String tokenComoProveedor = token(PROPIETARIO_ID, List.of("Propietario", "Proveedor"), "Proveedor");

        registrar(spaArmonia, tokenComoProveedor, recurso("Sala 3", "Espacio", 1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo un Propietario puede gestionar los recursos del negocio."));

        listar(spaArmonia, tokenComoProveedor)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo un Propietario puede gestionar los recursos del negocio."));

        assertEquals(0, recursoRepository.count());
    }

    @Test
    void ca08_cuentaConVariosRolesOperandoComoPropietarioPuedeRegistrar() throws Exception {
        String tokenComoPropietario = token(PROPIETARIO_ID, List.of("Propietario", "Proveedor"), "Propietario");

        registrar(spaArmonia, tokenComoPropietario, recurso("Sala 3", "Espacio", 1))
                .andExpect(status().isCreated());
    }

    @Test
    void ca08_rolActivoQueLaCuentaNoTieneNoDaPermisos() throws Exception {
        String tokenManipulado = token(PROPIETARIO_ID, List.of("Proveedor"), "Propietario");

        registrar(spaArmonia, tokenManipulado, recurso("Sala 3", "Espacio", 1))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloPropietario_usuarioSinRolPropietarioEsRechazado() throws Exception {
        registrar(spaArmonia, token(PROPIETARIO_ID, List.of("Cliente"), null), recurso("Sala 3", "Espacio", 1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo un Propietario puede gestionar los recursos del negocio."));
    }

    @Test
    void ca09_laListaSoloIncluyeLosDelNegocioConsultado() throws Exception {
        registrar(spaArmonia, tokenPropietario(), recurso("Camilla de masajes", "Equipo", 3)).andExpect(status().isCreated());
        registrar(spaNorte, tokenPropietario(), recurso("Sala 1", "Espacio", 1)).andExpect(status().isCreated());

        listar(spaArmonia, tokenPropietario())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recursos[*].nombre", contains("Camilla de masajes")));
    }

    @Test
    void negocioInexistenteRetorna404() throws Exception {
        registrar(999_999, tokenPropietario(), recurso("Sala", "Espacio", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Negocio no encontrado."));
    }

    @Test
    void sinTokenNoSePuedeRegistrar() throws Exception {
        mockMvc.perform(post("/api/v1/negocios/{id}/recursos", spaArmonia)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recurso("Sala", "Espacio", 1)))
                .andExpect(status().isForbidden());

        assertEquals(0, recursoRepository.count());
    }
}
