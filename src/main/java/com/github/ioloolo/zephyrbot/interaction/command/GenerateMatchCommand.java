package com.github.ioloolo.zephyrbot.interaction.command;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import com.github.ioloolo.zephyrbot.ZephyrBotApplication;
import com.github.ioloolo.zephyrbot.data.Match;
import com.github.ioloolo.zephyrbot.interaction.CommonMethod;
import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;
import com.github.ioloolo.zephyrbot.interaction.button.ButtonBridge;
import com.github.ioloolo.zephyrbot.interaction.button.JoinMatchButton;
import com.github.ioloolo.zephyrbot.repository.MatchRepository;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateMatchCommand implements InteractionInterface<SlashCommandInteractionEvent> {

	private final ZephyrBotApplication.ZephyrBot zephyrBot;
	private final CommandBridge                  commandBridge;
	private final ButtonBridge                   buttonBridge;

	private final MatchRepository matchRepository;

	private final CommonMethod commonMethod;

	@Getter
	private int maxPlayer;

	@PostConstruct
	public void init() {

		SlashCommandData command = Commands.slash("생성", "커스텀 경기를 생성합니다.")
				.addOption(OptionType.INTEGER, "인원", "최대 게임 인원", true);

		zephyrBot.getJda()
				.upsertCommand(command)
				.queue(commands -> log.info("[Register Command] {} - {}", command.getName(), command.getDescription()));

		commandBridge.registerCommand(command, this);
	}

	@Override
	public void onInteraction(SlashCommandInteractionEvent event) {

		if (!commonMethod.checkPermission(event) || !commonMethod.checkRunningMatch(event)) {
			return;
		}

		maxPlayer = Objects.requireNonNull(event.getOption("인원")).getAsInt();

		generateNewMatch();
		sendSuccessMessage(event);
	}

	private void generateNewMatch() {

		Match match = Match.builder()
				.end(false)
				.team1(Match.Team.builder().name("1팀").member(List.of()).build())
				.team2(Match.Team.builder().name("2팀").member(List.of()).build())
				.build();

		log.info("[Generate Match] 경기 DB 생성 완료.");

		matchRepository.save(match);
	}

	private void sendSuccessMessage(SlashCommandInteractionEvent event) {

		MessageEmbed messageEmbed = new EmbedBuilder().setTitle("경기가 생성되었습니다.")
				.setDescription("경기에 참여할 분들은 아래 ***[참여]*** 버튼을 눌러주세요.")
				.addField(new MessageEmbed.Field("인원", "0/%d 명".formatted(maxPlayer), true))
				.addField(new MessageEmbed.Field("참가자", "", false))
				.setFooter("Team ZephyR")
				.setColor(Color.CYAN)
				.build();

		Button button = buttonBridge.getButton(JoinMatchButton.class);

		event.replyEmbeds(messageEmbed).setActionRow(button).queue();
	}
}
