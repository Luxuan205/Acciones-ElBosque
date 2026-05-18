package com.accioneselbosque.configuration.repository;

import com.accioneselbosque.configuration.model.GlobalParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GlobalParameterRepository extends JpaRepository<GlobalParameter, String> {
    List<GlobalParameter> findAllByOrderByCategoryAscKeyAsc();
}
