-- =========================================================
-- CU-06 - COBRO DE CONSULTA EN CAJA
-- =========================================================

ALTER TABLE pago
    ADD COLUMN IF NOT EXISTS monto_recibido NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS cambio NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS tipo_tarjeta VARCHAR(20),
    ADD COLUMN IF NOT EXISTS ultimos4_tarjeta VARCHAR(4),
    ADD COLUMN IF NOT EXISTS canal_pago VARCHAR(20),
    ADD COLUMN IF NOT EXISTS id_usuario_cajero INTEGER;


-- =========================================================
-- FK CAJERO
-- =========================================================

ALTER TABLE pago
DROP CONSTRAINT IF EXISTS fk_pago_usuario_cajero;

ALTER TABLE pago
    ADD CONSTRAINT fk_pago_usuario_cajero
        FOREIGN KEY (id_usuario_cajero)
            REFERENCES usuario(id);


-- =========================================================
-- ÍNDICE
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_pago_usuario_cajero
    ON pago(id_usuario_cajero);