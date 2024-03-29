package com.github.ioloolo.zephyrbot.interaction.button;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import com.github.ioloolo.zephyrbot.data.Match;
import com.github.ioloolo.zephyrbot.data.User;
import com.github.ioloolo.zephyrbot.interaction.CommonMethod;
import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;
import com.github.ioloolo.zephyrbot.interaction.dropdown.MapVoteDropdown;
import com.github.ioloolo.zephyrbot.repository.MatchRepository;
import com.github.ioloolo.zephyrbot.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamRandomButton implements InteractionInterface<ButtonInteractionEvent> {

	private final ButtonBridge buttonBridge;

	private final UserRepository  userRepository;
	private final MatchRepository matchRepository;

	private final CommonMethod commonMethod;

	private final MapVoteDropdown mapVoteDropdown;

	@Getter
	private long voice1;
	@Getter
	private long voice2;

	@PostConstruct
	public void init() {

		Button button = Button.primary("team_random", "랜덤");

		buttonBridge.registerButton(button, this);
	}

	@Override
	public void onInteraction(ButtonInteractionEvent event) {

		if (!commonMethod.checkPermission(event) || !commonMethod.checkHasMatch(event)) {
			return;
		}

		Match match = shuffleTeam();

		log.info("[Team Set] 랜덤으로 팀을 섞었습니다.");

		List<User> team1 = commonMethod.getMatchTeam1User(match);
		List<User> team2 = commonMethod.getMatchTeam2User(match);

		MoveMemberTeamChannel(event, team1, team2);

		event.getMessageChannel().deleteMessageById(event.getMessageIdLong()).queue(v -> {
			MessageEmbed messageEmbed = new EmbedBuilder().setTitle("팀원 구성")
					.setDescription("팀원이 구성되었습니다.\n\n10초 후 맵 선택이 진행됩니다.")
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
					.setFooter("Team ZephyR")
					.setColor(Color.GREEN)
					.build();

			event.getMessageChannel()
					.sendMessageEmbeds(messageEmbed)
					.queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS, v2 -> {
						mapVoteDropdown.setTeam1(true);
						mapVoteDropdown.sendMapVoteMessage(message.getChannel());
					}));
		});
	}

	private Match shuffleTeam() {

		Match match = matchRepository.findByEndIsFalse().orElseThrow();

		List<Long> member = new ArrayList<>(match.getTeam1().getMember());
		Collections.shuffle(member);

		List<Long> member1 = member.subList(0, 5);
		List<Long> member2 = member.subList(5, 10);

		User leader1 = userRepository.findBySteamId(member1.get(0)).orElseThrow();
		User leader2 = userRepository.findBySteamId(member2.get(0)).orElseThrow();

		match.getTeam1().setName(leader1.getName() + " 팀");
		match.getTeam1().setMember(member1);

		match.getTeam2().setName(leader2.getName() + " 팀");
		match.getTeam2().setMember(member2);

		matchRepository.save(match);

		return match;
	}

	private void MoveMemberTeamChannel(ButtonInteractionEvent event, List<User> team1, List<User> team2) {

		Guild guild = event.getGuild();

		if (guild == null) {
			return;
		}

		guild.createVoiceChannel("[내전] %s 팀".formatted(team1.get(0).getName())).queue(channel -> {
			this.voice1 = channel.getIdLong();

			team1.stream()
					.map(User::getDiscord)
					.map(guild::getMemberById)
					.filter(Objects::nonNull)
					.filter(x -> x.getVoiceState() != null)
					.filter(x -> x.getVoiceState().inAudioChannel())
					.forEach(member -> guild.moveVoiceMember(member, channel).queue());
		});

		guild.createVoiceChannel("[내전] %s 팀".formatted(team2.get(0).getName())).queue(channel -> {
			this.voice2 = channel.getIdLong();

			team2.stream()
					.map(User::getDiscord)
					.map(guild::getMemberById)
					.filter(Objects::nonNull)
					.filter(x -> x.getVoiceState() != null)
					.filter(x -> x.getVoiceState().inAudioChannel())
					.forEach(member -> guild.moveVoiceMember(member, channel).queue());
		});
	}
}
