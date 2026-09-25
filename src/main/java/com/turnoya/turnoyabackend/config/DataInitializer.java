package com.turnoya.turnoyabackend.config;

import com.turnoya.turnoyabackend.entity.Especialidad;
import com.turnoya.turnoyabackend.entity.RolUsuario;
import com.turnoya.turnoyabackend.entity.Usuario;
import com.turnoya.turnoyabackend.repository.EspecialidadRepository;
import com.turnoya.turnoyabackend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Crea el administrador inicial (con las variables app.admin.*) y algunas
 * especialidades básicas la primera vez que se levanta la aplicación.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.display-name}")
    private String adminNombre;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           EspecialidadRepository especialidadRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.especialidadRepository = especialidadRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        crearAdministrador();
        crearEspecialidadesBase();
    }

    private void crearAdministrador() {
        if (usuarioRepository.existsByEmail(adminEmail)) {
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .nombre(adminNombre)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .rol(RolUsuario.ADMINISTRADOR)
                .build());
        log.info("Administrador inicial creado: {}", adminEmail);
    }

    private void crearEspecialidadesBase() {
        if (especialidadRepository.count() > 0) {
            return;
        }
        List<Especialidad> especialidades = List.of(
                Especialidad.builder().nombre("Medicina General").descripcion("Atención primaria").build(),
                Especialidad.builder().nombre("Pediatría").descripcion("Atención de niños y adolescentes").build(),
                Especialidad.builder().nombre("Obstetricia").descripcion("Control prenatal y salud materna").build(),
                Especialidad.builder().nombre("Odontología").descripcion("Salud bucal").build()
        );
        especialidadRepository.saveAll(especialidades);
        log.info("Especialidades base creadas");
    }
}