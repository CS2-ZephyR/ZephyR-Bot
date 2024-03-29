package com.github.ioloolo.zephyrbot.interaction.dropdown;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import com.github.ioloolo.zephyrbot.data.Match;
import com.github.ioloolo.zephyrbot.data.User;
import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;
import com.github.ioloolo.zephyrbot.interaction.button.MatchStartButton;
import com.github.ioloolo.zephyrbot.repository.MatchRepository;
import com.github.ioloolo.zephyrbot.repository.UserRepository;
import com.github.ioloolo.zephyrbot.util.MapList;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MapVoteDropdown implements InteractionInterface<StringSelectInteractionEvent> {

	private final DropdownBridge dropdownBridge;

	private final UserRepository  userRepository;
	private final MatchRepository matchRepository;

	private final MatchStartButton matchStartButton;

	private final MapList mapList;

	@Setter
	private boolean isTeam1;

	@PostConstruct
	public void init() {

		List<SelectOption> list = mapList.getMapList()
				.stream()
				.map(x -> SelectOption.of(x.getName(), x.getRaw()))
				.toList();

		StringSelectMenu dropdown = StringSelectMenu.create("map_vote").addOptions(list).build();

		dropdownBridge.registerDropdown(dropdown, this);
	}

	@Override
	public void onInteraction(StringSelectInteractionEvent event) {

		Match match = matchRepository.findByEndIsFalse().orElseThrow();
		Match.Team team = isTeam1 ? match.getTeam1() : match.getTeam2();
		isTeam1 = !isTeam1;
		User leader = userRepository.findBySteamId(team.getMember().get(0)).orElseThrow();

		if (!checkLeader(event, leader)) {
			return;
		}

		String map = event.getSelectedOptions().get(0).getLabel();

		MessageEmbed originalEmbed = event.getMessage().getEmbeds().get(0);

		List<String> remainedMap = getFieldMapList(originalEmbed, "남은 맵");
		List<String> blockedMap = getFieldMapList(originalEmbed, "차단된 맵");

		if (!checkAlreadyBan(event, blockedMap, map)) {
			return;
		}

		remainedMap = remainedMap.stream().filter(x -> !x.equals(map)).toList();
		blockedMap.add(map);

		log.info("[Map Vote] {} - {}", leader.getName(), map);

		if (remainedMap.size() > 1) {
			MessageEmbed messageEmbed = new EmbedBuilder(originalEmbed).clearFields()
					.setDescription("**%s**의 팀장인 **%s님**은 차단할 맵을 선택해주세요.".formatted(team.getName(), leader.getName()))
					.addField(new MessageEmbed.Field("남은 맵", String.join("\n", remainedMap), true))
					.addField(new MessageEmbed.Field("차단된 맵", String.join("\n", blockedMap), false))
					.build();

			StringSelectMenu dropdown = dropdownBridge.getDropdown(MapVoteDropdown.class);

			event.editMessageEmbeds(messageEmbed).setActionRow(dropdown).queue();
		} else {

			match.setMap(mapList.nameToRaw(remainedMap.get(0)));
			matchRepository.save(match);

			event.getMessageChannel()
					.deleteMessageById(event.getMessageIdLong())
					.queue(v -> matchStartButton.sendMatchStartMessage(event.getMessageChannel()));
		}
	}

	public void sendMapVoteMessage(MessageChannel channel) {

		List<String> list = mapList.getMapList().stream().map(MapList.Map::getName).toList();

		Match match = matchRepository.findByEndIsFalse().orElseThrow();
		Match.Team team = isTeam1 ? match.getTeam1() : match.getTeam2();
		isTeam1 = !isTeam1;
		User leader = userRepository.findBySteamId(team.getMember().get(0)).orElseThrow();

		MessageEmbed messageEmbed = new EmbedBuilder().setTitle("맵 선택")
				.setDescription("**%s**의 팀장인 **%s님**은 차단할 맵을 선택해주세요.".formatted(team.getName(), leader.getName()))
				.addField(new MessageEmbed.Field("남은 맵", String.join("\n", list), true))
				.addField(new MessageEmbed.Field("차단된 맵", "", false))
				.setFooter("Team ZephyR")
				.setColor(Color.YELLOW)
				.build();

		StringSelectMenu dropdown = dropdownBridge.getDropdown(MapVoteDropdown.class);

		channel.sendMessageEmbeds(messageEmbed).setActionRow(dropdown).queue();
	}

	private List<String> getFieldMapList(MessageEmbed message, String name) {

		String rawString = message.getFields()
				.stream()
				.filter(x -> Objects.requireNonNull(x.getName()).equals(name))
				.findAny()
				.orElseThrow()
				.getValue();

		if (rawString == null || rawString.length() == 1) {
			return new ArrayList<>();
		}

		return new ArrayList<>(Arrays.stream(rawString.split("\n")).toList());
	}

	private boolean checkLeader(StringSelectInteractionEvent event, User leader) {

		if (event.getUser().getIdLong() != leader.getDiscord()) {
			event.deferReply(true).queue(hook -> {
				MessageEmbed messageEmbed = new EmbedBuilder().setTitle("권한이 없습니다.")
						.setDescription("당신은 권한이 없습니다.")
						.setFooter("Team ZephyR")
						.setColor(Color.RED)
						.build();

				hook.editOriginalEmbeds(messageEmbed).queue();
			});

			return false;
		}

		return true;
	}

	private boolean checkAlreadyBan(StringSelectInteractionEvent event, List<String> blockedMap, String map) {

		if (blockedMap.contains(map)) {
			event.deferReply(true).queue(hook -> {
				MessageEmbed messageEmbed = new EmbedBuilder().setTitle("오류가 발생했습니다.")
						.setDescription("이미 차단된 맵입니다.\n\n다른 맵을 선택해주세요.")
						.setFooter("Team ZephyR")
						.setColor(Color.RED)
						.build();

				hook.editOriginalEmbeds(messageEmbed).queue();
			});

			return false;
		}

		return true;
	}
}
