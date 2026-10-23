
package com.proyecto.clinicamedica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * =========================================================
 * SANTOS CLÍNICA MÉDICA
 * CLASE PRINCIPAL
 * =========================================================
 *
 * Permite controlar los procesos automáticos
 * sin modificar los casos de uso existentes.
 *
 * En producción:
 * - Los schedulers continúan habilitados.
 *
 * En Docker de pruebas:
 * - Se podrán desactivar mediante una variable.
 *
 * =========================================================
 */
@SpringBootApplication
@Import(ClinicamedicaApplication.ConfiguracionScheduling.class)
public class ClinicamedicaApplication {

	public static void main(String[] args) {

		SpringApplication.run(
				ClinicamedicaApplication.class,
				args
		);
	}


	// =====================================================
	// CONFIGURACIÓN CONDICIONAL DE SCHEDULERS
	// =====================================================

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnProperty(
			name = "app.scheduling.enabled",
			havingValue = "true",
			matchIfMissing = true
	)
	static class ConfiguracionScheduling {

	}
}
