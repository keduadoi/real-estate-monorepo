package com.realestate.analytics.repository;

import com.realestate.analytics.entity.PriceChangeRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PriceChangeRepository extends MongoRepository<PriceChangeRecord, String> {
}
