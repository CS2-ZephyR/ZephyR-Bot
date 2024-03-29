package com.github.ioloolo.zephyrbot.interaction.button;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import com.github.ioloolo.zephyrbot.data.Match;
import com.github.ioloolo.zephyrbot.data.User;
import com.github.ioloolo.zephyrbot.interaction.CommonMethod;
import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;
import com.github.ioloolo.zephyrbot.interaction.command.GenerateMatchCommand;
import com.github.ioloolo.zephyrbot.repository.MatchRepository;
import com.github.ioloolo.zephyrbot.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JoinMatchButton implements InteractionInterface<ButtonInteractionEvent> {

	private final ButtonBridge buttonBridge;

	private final UserRepository userRepository;
	private final MatchRepository matchRepository;

	private final CommonMethod commonMethod;

	private final GenerateMatchCommand generateMatchCommand;

	@PostConstruct
	public void init() {

		Button button = Button.primary("join_match", "참여");

		buttonBridge.registerButton(button, this);
	}

	@Override
	public void onInteraction(ButtonInteractionEvent event) {

		if (!commonMethod.checkHasMatch(event) || !commonMethod.checkUserRegister(event)) {
			return;
		}
		Match match = matchRepository.findByEndIsFalse().orElseThrow();
		User user = userRepository.findByDiscord(event.getUser().getIdLong()).orElseThrow();

		if (!checkAlreadyJoin(event, match.getTeam1().getMember(), user.getSteamId())) {
			return;
		}

		match.getTeam1().getMember().add(user.getSteamId());
		matchRepository.save(match);

		log.info("[Join Match] {}", user.getName());

		MessageEmbed originalMessageEmbed = event.getMessage().getEmbeds().get(0);

		String participants = originalMessageEmbed.getFields()
				.stream()
				.filter(x -> x.getName() != null)
				.filter(x -> x.getName().equals("참가자"))
				.findAny()
				.orElseThrow()
				.getValue();

		if (Objects.requireNonNull(participants).length() > 1) {
			participants += '\n';
		}
		participants += "%s".formatted(user.getName());
		int numOfParticipants = participants.split("\n").length;

		MessageEmbed messageEmbed = new EmbedBuilder(originalMessageEmbed).clearFields()
				.addField(new MessageEmbed.Field("인원",
												 "%d/%d 명".formatted(
														 numOfParticipants,
														 generateMatchCommand.getMaxPlayer()
												 ),
												 false
				))
				.addField(new MessageEmbed.Field("참가자", participants, false))
				.build();

		event.editMessageEmbeds(messageEmbed).queue(hook -> {
			if (numOfParticipants < generateMatchCommand.getMaxPlayer()) {
				return;
			}

			hook.deleteOriginal().queue(v -> sendSelectMemberMessage(event));
		});
	}

	private boolean checkAlreadyJoin(ButtonInteractionEvent event, List<Long> members, long member) {

		boolean alreadyJoin = members.contains(member);

		if (alreadyJoin) {
			event.deferReply(true).queue(hook -> {
				MessageEmbed messageEmbed = new EmbedBuilder().setTitle("오류가 발생했습니다.")
						.setDescription("이미 참가된 유저입니다.")
						.setFooter("Team ZephyR")
						.setColor(Color.RED)
						.build();

				hook.editOriginalEmbeds(messageEmbed).queue();
			});

			return false;
		}

		return true;
	}

	private void sendSelectMemberMessage(ButtonInteractionEvent event) {

		MessageEmbed messageEmbed = new EmbedBuilder().setTitle("팀원 구성")
				.setDescription("팀원 구성 방식을 선택해주세요.")
				.addField(new MessageEmbed.Field("랜덤", "모든 참가자를 랜덤으로 섞습니다.", false))
				.addField(new MessageEmbed.Field("지정", "모든 참가자의 팀을 지정합니다.", false))
				.setFooter("Team ZephyR")
				.setColor(Color.YELLOW)
				.build();

		Button randomButton = buttonBridge.getButton(TeamRandomButton.class);
		Button setButton = buttonBridge.getButton(TeamSetButton.class);

		event.getMessageChannel().sendMessageEmbeds(messageEmbed).setActionRow(randomButton, setButton).queue();
	}
}
