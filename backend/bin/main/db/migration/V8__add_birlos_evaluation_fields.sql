ALTER TABLE evaluacion
    ADD COLUMN llantas_birlos_delantera_izquierda_count INT NULL AFTER llantas_tuercas_trasera_derecha_rotas,
    ADD COLUMN llantas_birlos_delantera_izquierda_selected VARCHAR(120) NULL AFTER llantas_birlos_delantera_izquierda_count,
    ADD COLUMN llantas_birlos_delantera_derecha_count INT NULL AFTER llantas_birlos_delantera_izquierda_selected,
    ADD COLUMN llantas_birlos_delantera_derecha_selected VARCHAR(120) NULL AFTER llantas_birlos_delantera_derecha_count,
    ADD COLUMN llantas_birlos_trasera_izquierda_count INT NULL AFTER llantas_birlos_delantera_derecha_selected,
    ADD COLUMN llantas_birlos_trasera_izquierda_selected VARCHAR(120) NULL AFTER llantas_birlos_trasera_izquierda_count,
    ADD COLUMN llantas_birlos_trasera_derecha_count INT NULL AFTER llantas_birlos_trasera_izquierda_selected,
    ADD COLUMN llantas_birlos_trasera_derecha_selected VARCHAR(120) NULL AFTER llantas_birlos_trasera_derecha_count;
