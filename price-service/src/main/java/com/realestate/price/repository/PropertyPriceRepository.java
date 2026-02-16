package com.realestate.price.repository;

import com.realestate.price.entity.PropertyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyPriceRepository extends JpaRepository<PropertyPrice, Long> {

    Optional<PropertyPrice> findByPropertyId(Long propertyId);

    List<PropertyPrice> findByPropertyIdIn(List<Long> propertyIds);

    boolean existsByPropertyId(Long propertyId);
}
