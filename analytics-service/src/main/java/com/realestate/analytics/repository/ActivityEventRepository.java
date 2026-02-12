package com.realestate.analytics.repository;

import com.realestate.analytics.entity.ActivityEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActivityEventRepository extends MongoRepository<ActivityEvent, String> {
}
