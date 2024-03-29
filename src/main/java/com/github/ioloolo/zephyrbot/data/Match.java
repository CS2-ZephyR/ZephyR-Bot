package com.github.ioloolo.zephyrbot.data;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "Match")
public class Match {

	@Id
	@MongoId
	private ObjectId id;

	@Field("End")
	private boolean end;

	@Field("Map")
	private String map;

	@Field("Team1")
	private Team team1;

	@Field("Team2")
	private Team team2;

	@Data
	@Builder
	public static class Team {

		@Field("Name")
		private String name;

		@Field("Member")
		private List<Long> member;
	}
}
