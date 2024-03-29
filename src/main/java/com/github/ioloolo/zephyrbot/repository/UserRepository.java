package com.github.ioloolo.zephyrbot.repository;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.github.ioloolo.zephyrbot.data.User;

@Repository
public interface UserRepository extends MongoRepository<User, ObjectId> {

	Optional<User> findBySteamId(long steamId);

	Optional<User> findByDiscord(long discord);
}
