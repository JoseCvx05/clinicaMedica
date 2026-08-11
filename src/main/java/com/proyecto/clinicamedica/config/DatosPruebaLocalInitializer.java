package com.proyecto.clinicamedica.config;

import com.proyecto.clinicamedica.entity.Especialidad;
import com.proyecto.clinicamedica.entity.Rol;
import com.proyecto.clinicamedica.entity.Sucursal;
import com.proyecto.clinicamedica.entity.Usuario;
import com.proyecto.clinicamedica.service.CifradoService;
import com.proyecto.clinicamedica.service.EspecialidadService;
import com.proyecto.clinicamedica.service.HashService;
import com.proyecto.clinicamedica.service.RolService;
import com.proyecto.clinicamedica.service.SucursalService;
import com.proyecto.clinicamedica.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * =========================================================
 * DATOS DE PRUEBA - PERFIL LOCAL
 * =========================================================
 *
 * Esta clase existe exclusivamente para desarrollo local.
 *
 * Crea:
 *
 * 1. Paciente de prueba.
 * 2. Médico de prueba.
 *
 * IMPORTANTE:
 *
 * - NO guarda DPI en texto plano.
 * - Utiliza AES-GCM para dpi_cifrado.
 * - Utiliza HMAC-SHA-256 para dpi_hash.
 * - Utiliza PasswordEncoder para las contraseñas.
 * - No se ejecutará fuera del perfil "local".
 * =========================================================
 */
@Component
@Profile("local")
public class DatosPruebaLocalInitializer
        implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    DatosPruebaLocalInitializer.class
            );

    /*
     * DPI completamente ficticios usados únicamente
     * para pruebas locales.
     */
    private static final String DPI_PACIENTE_PRUEBA =
            "1111111111111";

    private static final String DPI_MEDICO_PRUEBA =
            "2222222222222";


    private final UsuarioService usuarioService;
    private final RolService rolService;
    private final SucursalService sucursalService;
    private final EspecialidadService especialidadService;

    private final HashService hashService;
    private final CifradoService cifradoService;

    private final PasswordEncoder passwordEncoder;

    private final String passwordPaciente;
    private final String passwordMedico;


    public DatosPruebaLocalInitializer(
            UsuarioService usuarioService,
            RolService rolService,
            SucursalService sucursalService,
            EspecialidadService especialidadService,
            HashService hashService,
            CifradoService cifradoService,
            PasswordEncoder passwordEncoder,

            @Value("${app.test.paciente-password}")
            String passwordPaciente,

            @Value("${app.test.medico-password}")
            String passwordMedico
    ) {
        this.usuarioService = usuarioService;
        this.rolService = rolService;
        this.sucursalService = sucursalService;
        this.especialidadService = especialidadService;

        this.hashService = hashService;
        this.cifradoService = cifradoService;

        this.passwordEncoder = passwordEncoder;

        this.passwordPaciente = passwordPaciente;
        this.passwordMedico = passwordMedico;
    }


    @Override
    public void run(ApplicationArguments args) {

        LOGGER.info(
                "Iniciando verificación de datos de prueba locales..."
        );

        crearPacientePrueba();

        crearMedicoPrueba();

        LOGGER.info(
                "Datos de prueba locales preparados correctamente."
        );
    }


    // =====================================================
    // PACIENTE
    // =====================================================

    private void crearPacientePrueba() {

        String dpiHash =
                hashService.generarHash(
                        DPI_PACIENTE_PRUEBA
                );

        /*
         * Evitamos crear nuevamente el mismo usuario
         * cada vez que reinicia Spring Boot.
         */
        if (usuarioService.existeDpiHash(dpiHash)) {

            LOGGER.info(
                    "Paciente de prueba ya existente."
            );

            return;
        }


        Rol rolPaciente =
                rolService
                        .buscarActivoPorNombre("Paciente")
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "No existe el rol Paciente."
                                )
                        );


        Usuario paciente =
                new Usuario();

        paciente.setNombreCompleto(
                "Paciente de Prueba"
        );

        paciente.setCorreoElectronico(
                "paciente.prueba@local.test"
        );

        /*
         * Debe tener entre 8 y 9 caracteres.
         */
        paciente.setNombreUsuario(
                "paciente1"
        );

        paciente.setContrasenaHash(
                passwordEncoder.encode(
                        passwordPaciente
                )
        );


        // =============================================
        // DPI PROTEGIDO
        // =============================================

        paciente.setDpiHash(
                dpiHash
        );

        paciente.setDpiCifrado(
                cifradoService.cifrar(
                        DPI_PACIENTE_PRUEBA
                )
        );


        paciente.setTelefono(
                "55550001"
        );

        paciente.setRol(
                rolPaciente
        );

        paciente.setActivo(
                true
        );

        paciente.setIntentosFallidosLogin(
                (short) 0
        );


        usuarioService.guardar(
                paciente
        );


        LOGGER.info(
                "Paciente de prueba creado."
        );
    }


    // =====================================================
    // MÉDICO / USUARIO INTERNO
    // =====================================================

    private void crearMedicoPrueba() {

        String dpiHash =
                hashService.generarHash(
                        DPI_MEDICO_PRUEBA
                );


        if (usuarioService.existeDpiHash(dpiHash)) {

            LOGGER.info(
                    "Médico de prueba ya existente."
            );

            return;
        }


        Rol rolMedico =
                rolService
                        .buscarActivoPorNombre("Médico")
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "No existe el rol Médico."
                                )
                        );


        Sucursal sucursal =
                sucursalService
                        .buscarActivaPorNombre(
                                "Sucursal Central"
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "No existe Sucursal Central."
                                )
                        );


        Especialidad especialidad =
                especialidadService
                        .buscarActivaPorNombre(
                                "Medicina General"
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "No existe Medicina General."
                                )
                        );


        Usuario medico =
                new Usuario();

        medico.setNombreCompleto(
                "Medico de Prueba"
        );

        medico.setCorreoElectronico(
                "medico.prueba@local.test"
        );

        medico.setNombreUsuario(
                "medico01"
        );

        medico.setContrasenaHash(
                passwordEncoder.encode(
                        passwordMedico
                )
        );


        // =============================================
        // DPI PROTEGIDO
        // =============================================

        medico.setDpiHash(
                dpiHash
        );

        medico.setDpiCifrado(
                cifradoService.cifrar(
                        DPI_MEDICO_PRUEBA
                )
        );


        medico.setTelefono(
                "55550002"
        );

        medico.setRol(
                rolMedico
        );

        medico.setSucursal(
                sucursal
        );

        medico.setEspecialidad(
                especialidad
        );

        medico.setActivo(
                true
        );

        medico.setIntentosFallidosLogin(
                (short) 0
        );


        usuarioService.guardar(
                medico
        );


        LOGGER.info(
                "Médico de prueba creado."
        );
    }
}