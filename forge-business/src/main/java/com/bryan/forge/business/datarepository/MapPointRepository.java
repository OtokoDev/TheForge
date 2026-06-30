package com.bryan.forge.business.datarepository;

import com.bryan.forge.business.datamodel.MapPoint;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MapPointRepository extends JpaRepository<MapPoint, UUID> {

    List<MapPoint> findByBusinessId(UUID businessId);
}
