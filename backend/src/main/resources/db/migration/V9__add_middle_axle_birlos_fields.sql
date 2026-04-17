ALTER TABLE evaluacion
    ADD COLUMN llantas_birlos_media_izquierda_count INT NULL AFTER llantas_birlos_trasera_derecha_selected,
    ADD COLUMN llantas_birlos_media_izquierda_selected VARCHAR(120) NULL AFTER llantas_birlos_media_izquierda_count,
    ADD COLUMN llantas_birlos_media_derecha_count INT NULL AFTER llantas_birlos_media_izquierda_selected,
    ADD COLUMN llantas_birlos_media_derecha_selected VARCHAR(120) NULL AFTER llantas_birlos_media_derecha_count;
