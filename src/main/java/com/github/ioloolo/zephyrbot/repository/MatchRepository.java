package com.github.ioloolo.zephyrbot.repository;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.github.ioloolo.zephyrbot.data.Match;

@Repository
public interface MatchRepository extends MongoRepository<Match, ObjectId> {

	Optional<Match> findByEndIsFalse();
}
