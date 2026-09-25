package org.salva.task.court_reservation_system.dto.response;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

/** Respuesta paginada estándar de la API. La página empieza en 0. */
public record PageResponseDTO<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static <T> PageResponseDTO<T> of(Page<T> page) {
        return new PageResponseDTO<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /** Construye un Pageable seguro: página >= 0 y tamaño entre 1 y {@value #MAX_SIZE}. */
    public static Pageable pageable(int page, int size, Sort sort) {
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, sort);
    }
}
