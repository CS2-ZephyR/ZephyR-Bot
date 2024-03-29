package com.github.ioloolo.zephyrbot.interaction.button;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import com.github.ioloolo.zephyrbot.data.Match;
import com.github.ioloolo.zephyrbot.data.User;
import com.github.ioloolo.zephyrbot.interaction.CommonMethod;
import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;
import com.github.ioloolo.zephyrbot.repository.MatchRepository;
import com.github.ioloolo.zephyrbot.util.MapList;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchStartButton implements InteractionInterface<ButtonInteractionEvent> {

	private final ButtonBridge buttonBridge;

	private final MatchRepository matchRepository;

	private final MapList mapList;

	private final CommonMethod commonMethod;

	@PostConstruct
	public void init() {

		Button button = Button.danger("match_start", "서버 준비 완료");

		buttonBridge.registerButton(button, this);
	}

	@Override
	public void onInteraction(ButtonInteractionEvent event) {

		if (!commonMethod.checkPermission(event) || !commonMethod.checkHasMatch(event)) {
			return;
		}

		MessageEmbed originalEmbed = event.getMessage().getEmbeds().get(0);
		MessageEmbed messageEmbed = new EmbedBuilder(originalEmbed).setTitle("경기 진행")
				.setDescription("경기가 진행중입니다.\n\n[경기 참여](http://teamzephyr.kro.kr/connect)")
				.setColor(Color.GREEN)
				.build();

		log.info("[Match Start] 서버 준비 완료.");

		Button button = buttonBridge.getButton(MatchEndButton.class);

		event.getMessageChannel()
				.deleteMessageById(event.getMessageIdLong())
				.queue(v -> event.getMessageChannel().sendMessageEmbeds(messageEmbed).setActionRow(button).queue());
	}

	public void sendMatchStartMessage(MessageChannel channel) {

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

		channel.sendMessageEmbeds(messageEmbed).setActionRow(buttonBridge.getButton(this.getClass())).queue();
	}
}
