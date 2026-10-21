
package com.proyecto.clinicamedica;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import java.util.Arrays;

/**
 * =====================================================
 * GENERADOR TEMPORAL DE CONTRASEÑAS BCrypt
 * =====================================================
 *
 * Uso exclusivo para preparar los Administradores
 * iniciales de SANTOS Clínica Médica en Azure.
 *
 * No guarda contraseñas en archivos ni PostgreSQL.
 * No incluir esta herramienta en el commit.
 * =====================================================
 */
public class GeneradorBCryptTemporal {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();


        String[] usuarios = {
                "medmelvin",
                "medvaleri",
                "medcarlos",
                "medsofia",
                "medalex",
                "meddaniel",
                "medsebas",
                "medlucia"
        };


        for (String usuario : usuarios) {

            JPasswordField campo =
                    new JPasswordField(20);

            int opcion = JOptionPane.showConfirmDialog(
                    null,
                    campo,
                    "Contraseña para " + usuario,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }

            char[] contrasena = campo.getPassword();

            if (contrasena.length < 12) {

                Arrays.fill(contrasena, '\0');

                JOptionPane.showMessageDialog(
                        null,
                        "Utilice al menos 12 caracteres."
                );

                return;
            }

            String hash = encoder.encode(
                    new String(contrasena)
            );

            Arrays.fill(contrasena, '\0');

            System.out.println(
                    usuario + " -> " + hash
            );
        }
    }
}
