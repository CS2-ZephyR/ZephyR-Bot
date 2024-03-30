package com.github.ioloolo.zephyrbot.interaction.dropdown;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import com.github.ioloolo.zephyrbot.data.Match;
import com.github.ioloolo.zephyrbot.data.User;
import com.github.ioloolo.zephyrbot.interaction.CommonMethod;
import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;
import com.github.ioloolo.zephyrbot.repository.MatchRepository;
import com.github.ioloolo.zephyrbot.repository.UserRepository;
import com.github.ioloolo.zephyrbot.socket.WebSocketHandler;
import com.github.ioloolo.zephyrbot.util.MapList;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MapVoteDropdown implements InteractionInterface<StringSelectInteractionEvent> {

	private final DropdownBridge dropdownBridge;

	private final UserRepository  userRepository;
	private final MatchRepository matchRepository;

	private final CommonMethod commonMethod;

	private final MapList mapList;

	private final WebSocketHandler webSocketHandler;

	private Stack<String> blockedMap;
	private boolean isTeam1;
	private long lastPingMessage;

	@Value("${csgo.path}")
	private String serverPath;

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
		User leader = userRepository.findBySteamId(team.getMember().get(0)).orElseThrow();

		String map = event.getSelectedOptions().get(0).getLabel();

		if (!checkLeader(event, leader) || !checkAlreadyBan(event, blockedMap, map)) {
			return;
		}

		blockedMap.push(map);

		log.info("[Map Vote] {} - {}", leader.getName(), map);

		List<String> remainedMap = mapList.getMapList()
				.stream()
				.map(MapList.Map::getName)
				.filter(name -> !blockedMap.contains(name))
				.toList();

		if (remainedMap.size() > 1) {
			{
				MessageEmbed originalEmbed = event.getMessage().getEmbeds().get(0);
				MessageEmbed messageEmbed = new EmbedBuilder(originalEmbed).clearFields()
						.addField(new MessageEmbed.Field("남은 맵", String.join("\n", remainedMap), true))
						.addField(new MessageEmbed.Field("차단된 맵", String.join("\n", blockedMap), false))
						.build();

				List<SelectOption> mapListOption = mapList.getMapList()
						.stream()
						.filter(x -> !blockedMap.contains(x.getName()))
						.map(x -> SelectOption.of(x.getName(), x.getRaw()))
						.toList();

				StringSelectMenu dropdown = StringSelectMenu.create("map_vote").addOptions(mapListOption).build();

				event.editMessageEmbeds(messageEmbed).setActionRow(dropdown).queue();
			}

			{
				isTeam1 = !isTeam1;
				team = (isTeam1) ? match.getTeam1() : match.getTeam2();
				leader = userRepository.findBySteamId(team.getMember().get(0)).orElseThrow();

				User other = userRepository.findByDiscord(event.getUser().getIdLong()).orElseThrow();

				MessageEmbed messageEmbed = new EmbedBuilder().clearFields()
						.setTitle("맵 차단")
						.appendDescription("**%s**님이 **%s**를 차단했습니다.\n\n".formatted(other.getName(), map))
						.appendDescription("**%s**의 팀장인 **%s**님은 차단할 맵을 선택해주세요.\n<@%d>".formatted(team.getName(), leader.getName(), leader.getDiscord()))
						.setFooter("Team ZephyR")
						.setColor(Color.MAGENTA)
						.build();

				event.getMessageChannel().deleteMessageById(lastPingMessage).queue(v -> event.getChannel()
						.sendMessageEmbeds(messageEmbed)
						.queue(message -> lastPingMessage = message.getIdLong()));
			}
		} else {

			match.setMap(mapList.nameToRaw(remainedMap.get(0)));
			matchRepository.save(match);

			event.getMessageChannel().deleteMessageById(lastPingMessage).queue(v -> event.getMessageChannel()
					.deleteMessageById(event.getMessageIdLong())
					.queue(v2 -> sendMatchStartMessage(event)));
		}
	}

	public void initVar() {
		isTeam1 = true;
		blockedMap = new Stack<>();

		List<MapList.Map> mapList = new ArrayList<>(this.mapList.getMapList());
		Collections.shuffle(mapList);

		mapList.stream()
				.limit(mapList.size()-9)
				.map(MapList.Map::getName)
				.forEach(blockedMap::push);
	}

	public void sendMapVoteMessage(MessageChannel channel) {
		{
			List<String> remainedMap = mapList.getMapList()
					.stream()
					.map(MapList.Map::getName)
					.filter(name -> !blockedMap.contains(name))
					.toList();

			MessageEmbed messageEmbed = new EmbedBuilder().setTitle("맵 선택")
					.addField(new MessageEmbed.Field("남은 맵", String.join("\n", remainedMap), true))
					.addField(new MessageEmbed.Field("차단된 맵", String.join("\n", blockedMap), false))
					.setFooter("Team ZephyR")
					.setColor(Color.YELLOW)
					.build();

			List<SelectOption> mapListOption = this.mapList.getMapList()
					.stream()
					.filter(x -> !blockedMap.contains(x.getName()))
					.map(x -> SelectOption.of(x.getName(), x.getRaw()))
					.toList();

			StringSelectMenu dropdown = StringSelectMenu.create("map_vote").addOptions(mapListOption).build();

			channel.sendMessageEmbeds(messageEmbed).setActionRow(dropdown).queue();
		}

		{
			Match match = matchRepository.findByEndIsFalse().orElseThrow();
			Match.Team team = isTeam1 ? match.getTeam1() : match.getTeam2();
			User leader = userRepository.findBySteamId(team.getMember().get(0)).orElseThrow();

			MessageEmbed messageEmbed = new EmbedBuilder().clearFields()
					.setTitle("맵 차단")
					.setDescription("**%s**의 팀장인 **%s**님은 차단할 맵을 선택해주세요.\n<@%d>".formatted(team.getName(), leader.getName(), leader.getDiscord()))
					.setFooter("Team ZephyR")
					.setColor(Color.MAGENTA)
					.build();

			channel.sendMessageEmbeds(messageEmbed)
					.queue(message -> lastPingMessage = message.getIdLong());
		}
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

	private boolean checkAlreadyBan(StringSelectInteractionEvent event, Collection<String> blockedMap, String map) {

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

	private void sendMatchStartMessage(StringSelectInteractionEvent event) {

		Match match = matchRepository.findByEndIsFalse().orElseThrow();

		List<User> team1 = commonMethod.getMatchTeam1User(match);
		List<User> team2 = commonMethod.getMatchTeam2User(match);

		MessageEmbed messageEmbed = new EmbedBuilder().setTitle("경기 준비중")
				.setDescription("서버가 준비중입니다.\n\n잠시만 기다려주세요.")
				.addField(new MessageEmbed.Field("맵", mapList.rawToName(match.getMap()), true))
				.addField(new MessageEmbed.Field(
						match.getTeam1().getName(),
						team1.stream().map(User::getName).collect(Collectors.joining("\n")),
						true
				))
				.addField(new MessageEmbed.Field(
						match.getTeam2().getName(),
						team2.stream().map(User::getName).collect(Collectors.joining("\n")),
						true
				))
				.setThumbnail(mapList.fromRaw(match.getMap()).getLogo())
				.setFooter("Team ZephyR")
				.setColor(Color.YELLOW)
				.build();

		event.getChannel().sendMessageEmbeds(messageEmbed).queue(message -> {
			webSocketHandler.setMessage(message);

			String startCommand = ".\\game\\bin\\win64\\cs2.exe -dedicated -maxplayers 20 +game_mode 1 +game_type 0 +map lobby_mapveto";

			try {
				new ProcessBuilder("cmd.exe", "/C", startCommand).directory(new File(serverPath)).start();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
}
