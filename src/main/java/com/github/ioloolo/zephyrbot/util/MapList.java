package com.github.ioloolo.zephyrbot.util;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;

@Getter
@Component
public class MapList {

	@Data(staticConstructor = "of")
	public static class Map {

		private final String name;
		private final String raw;
		private final String logo;
	}

	private final List<Map> mapList = List.of(Map.of("더스트 2",
													 "de_dust2",
													 "https://static.wikia.nocookie.net/cswikia/images/d/db/Map_icon_de_dust2.png/revision/latest"
	), Map.of("신기루",
			  "de_mirage",
			  "https://static.wikia.nocookie.net/cswikia/images/9/96/Set_mirage.png/revision/latest"
	), Map.of("인페르노",
			  "de_inferno",
			  "https://static.wikia.nocookie.net/cswikia/images/0/0a/CS2_inferno_logo.png/revision/latest"
	), Map.of("버티고",
			  "de_vertigo",
			  "https://static.wikia.nocookie.net/cswikia/images/4/46/Vertigo-logo-new.png/revision/latest"
	), Map.of("고대",
			  "de_ancient",
			  "https://static.wikia.nocookie.net/cswikia/images/7/7c/Map_icon_de_ancient.png/revision/latest"
	), Map.of("핵시설",
			  "de_nuke",
			  "https://static.wikia.nocookie.net/cswikia/images/e/ef/Set_nuke_2.png/revision/latest"
	), Map.of("아누비스",
			  "de_anubis",
			  "https://static.wikia.nocookie.net/cswikia/images/f/f8/Map_icon_de_anubis.png/revision/latest"
	), Map.of("사무실",
			  "cs_office",
			  "https://static.wikia.nocookie.net/cswikia/images/a/a0/Set_office.png/revision/latest"
	), Map.of("이탈리아",
			  "cs_italy",
			  "https://static.wikia.nocookie.net/cswikia/images/f/fd/CS2_italy_logo.png/revision/latest"
	), Map.of("무기창고",
			  "workshop:3070596702",
			  "https://static.wikia.nocookie.net/cswikia/images/2/23/Set_cache.png/revision/latest"
	), Map.of("기차",
			  "workshop:3070284539",
			  "https://static.wikia.nocookie.net/cswikia/images/6/60/Set_train.png/revision/latest"
	), Map.of("아웃페르노",
			  "workshop:3196360672",
			  null
	), Map.of("Aurelia",
			  "workshop:3107067103",
			  null
	), Map.of("Amalia",
			  "workshop:3120867158",
			  null
	));

	public Map fromRaw(String raw) {

		return mapList.stream().filter(x -> x.getRaw().equals(raw)).findAny().orElseThrow(null);
	}

	public Map fromName(String name) {

		return mapList.stream().filter(x -> x.getName().equals(name)).findAny().orElseThrow(null);
	}

	public String rawToName(String raw) {

		return mapList.stream()
				.filter(x -> x.getRaw().equals(raw))
				.findAny()
				.map(MapList.Map::getName)
				.orElseThrow(null);
	}

	public String nameToRaw(String name) {

		return mapList.stream()
				.filter(x -> x.getName().equals(name))
				.findAny()
				.map(MapList.Map::getRaw)
				.orElseThrow(null);
	}
}
