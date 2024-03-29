package com.github.ioloolo.zephyrbot.interaction.command;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import com.github.ioloolo.zephyrbot.ZephyrBotApplication;
import com.github.ioloolo.zephyrbot.data.Match;
import com.github.ioloolo.zephyrbot.interaction.CommonMethod;
import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;
import com.github.ioloolo.zephyrbot.interaction.button.TeamSetButton;
import com.github.ioloolo.zephyrbot.interaction.dropdown.MapVoteDropdown;
import com.github.ioloolo.zephyrbot.repository.MatchRepository;
import com.github.ioloolo.zephyrbot.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamSetCommand implements InteractionInterface<SlashCommandInteractionEvent> {

	private final ZephyrBotApplication.ZephyrBot zephyrBot;

	private final CommandBridge commandBridge;

	private final UserRepository  userRepository;
	private final MatchRepository matchRepository;

	private final CommonMethod commonMethod;

	private final GenerateMatchCommand generateMatchCommand;
	private final MapVoteDropdown      mapVoteDropdown;

	@PostConstruct
	public void init() {

		SlashCommandData command = Commands.slash("팀지정", "경기의 팀원을 지정합니다.")
				.addOption(OptionType.INTEGER, "팀", "팀원을 지정할 팀을 지정합니다.", true)
				.addOption(OptionType.USER, "팀원1", "팀원 1을 지정합니다.", true)
				.addOption(OptionType.USER, "팀원2", "팀원 2을 지정합니다.", false)
				.addOption(OptionType.USER, "팀원3", "팀원 3을 지정합니다.", false)
				.addOption(OptionType.USER, "팀원4", "팀원 4을 지정합니다.", false)
				.addOption(OptionType.USER, "팀원5", "팀원 5을 지정합니다.", false);

		zephyrBot.getJda()
				.upsertCommand(command)
				.queue(commands -> log.info("[Register Command] {} - {}", command.getName(), command.getDescription()));

		commandBridge.registerCommand(command, this);
	}

	@Override
	public void onInteraction(SlashCommandInteractionEvent event) {

		if (!commonMethod.checkPermission(event) || !commonMethod.checkHasMatch(event)) {
			return;
		}

		int teamNum = Objects.requireNonNull(event.getOption("팀")).getAsInt();

		List<User> members = new ArrayList<>();

		if (event.getOption("팀원1") != null) {
			members.add(Objects.requireNonNull(event.getOption("팀원1")).getAsUser());
		}
		if (event.getOption("팀원2") != null) {
			members.add(Objects.requireNonNull(event.getOption("팀원2")).getAsUser());
		}
		if (event.getOption("팀원3") != null) {
			members.add(Objects.requireNonNull(event.getOption("팀원3")).getAsUser());
		}
		if (event.getOption("팀원4") != null) {
			members.add(Objects.requireNonNull(event.getOption("팀원4")).getAsUser());
		}
		if (event.getOption("팀원5") != null) {
			members.add(Objects.requireNonNull(event.getOption("팀원5")).getAsUser());
		}

		Match match = matchRepository.findByEndIsFalse().orElseThrow();

		log.info("[Team Set] {}팀 - {}", teamNum, members.stream().map(User::getName).collect(Collectors.joining(", ")));

		updateDatabase(match, teamNum, members);
		sendMessage(event, match);
	}

	private void updateDatabase(Match match, int teamNum, List<User> members) {

		Match.Team team = teamNum == 1 ? match.getTeam1() : match.getTeam2();

		List<com.github.ioloolo.zephyrbot.data.User> users = members.stream()
				.map(User::getIdLong)
				.map(userRepository::findByDiscord)
				.map(Optional::orElseThrow)
				.toList();

		team.setName("%s 팀".formatted(users.get(0).getName()));

		team.setMember(users.stream().map(com.github.ioloolo.zephyrbot.data.User::getSteamId).toList());

		matchRepository.save(match);
	}

	private void sendMessage(SlashCommandInteractionEvent event, Match match) {

		event.deferReply().queue(hook -> hook.deleteOriginal().queue());

		List<com.github.ioloolo.zephyrbot.data.User> team1 = commonMethod.getMatchTeam1User(match);
		List<com.github.ioloolo.zephyrbot.data.User> team2 = commonMethod.getMatchTeam2User(match);

		boolean end = team1.size() >= generateMatchCommand.getMaxPlayer() / 2 && team2.size() >= generateMatchCommand.getMaxPlayer() / 2;

		MessageEmbed messageEmbed = new EmbedBuilder().setTitle("팀원 구성")
				.setDescription("팀원이 구성되었습니다." + (end ? "\n\n10초 후 맵 선택이 진행됩니다." : ""))
				.addField(new MessageEmbed.Field(
						match.getTeam1().getName(),
						team1.stream()
								.map(com.github.ioloolo.zephyrbot.data.User::getName)
								.collect(Collectors.joining("\n")),
						true
				))
				.addField(new MessageEmbed.Field(
						match.getTeam2().getName(),
						team2.stream()
								.map(com.github.ioloolo.zephyrbot.data.User::getName)
								.collect(Collectors.joining("\n")),
						true
				))
				.setFooter("Team ZephyR")
				.setColor(Color.GREEN)
				.build();

		event.getMessageChannel().editMessageEmbedsById(TeamSetButton.getMessage(), messageEmbed).queue(message -> {
			if (!end) {
				return;
			}

			message.delete().queueAfter(10, TimeUnit.SECONDS, v2 -> {
				mapVoteDropdown.setTeam1(true);
				mapVoteDropdown.sendMapVoteMessage(event.getMessageChannel());
			});
		});
	}
}
