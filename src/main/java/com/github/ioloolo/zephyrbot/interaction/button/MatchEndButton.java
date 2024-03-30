package com.github.ioloolo.zephyrbot.interaction.button;

import java.awt.*;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import com.github.ioloolo.zephyrbot.data.Match;
import com.github.ioloolo.zephyrbot.interaction.CommonMethod;
import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;
import com.github.ioloolo.zephyrbot.repository.MatchRepository;
import com.github.ioloolo.zephyrbot.socket.WebSocketHandler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchEndButton implements InteractionInterface<ButtonInteractionEvent> {

	private final ButtonBridge buttonBridge;

	private final MatchRepository matchRepository;

	private final WebSocketHandler webSocketHandler;

	private final CommonMethod commonMethod;

	@Value("${discord.default-voice-channel}")
	private long defaultVoice;

	@PostConstruct
	public void init() {

		Button button = Button.danger("match_end", "경기 종료");

		buttonBridge.registerButton(button, this);
	}

	@Override
	public void onInteraction(ButtonInteractionEvent event) {

		if (!commonMethod.checkPermission(event) || !commonMethod.checkHasMatch(event)) {
			return;
		}

		Match match = matchRepository.findByEndIsFalse().orElseThrow();
		match.setEnd(true);
		matchRepository.save(match);

		MessageEmbed originalEmbed = event.getMessage().getEmbeds().get(0);
		MessageEmbed messageEmbed = new EmbedBuilder(originalEmbed).setTitle("경기 종료")
				.setDescription("경기가 종료되었습니다.")
				.setColor(Color.RED)
				.build();

		log.info("[Match End] 경기 종료.");

		Guild guild = event.getGuild();
		assert guild != null;

		VoiceChannel defaultVoiceChannel = guild.getVoiceChannelById(defaultVoice);

		Category category = guild.getCategoryById(webSocketHandler.getCategory());
		VoiceChannel voice1 = guild.getVoiceChannelById(webSocketHandler.getVoice1());
		VoiceChannel voice2 = guild.getVoiceChannelById(webSocketHandler.getVoice2());

		if (defaultVoiceChannel == null || category == null || voice1 == null || voice2 == null) {
			return;
		}

		Stream.of(voice1.getMembers(), voice2.getMembers())
				.flatMap(Collection::stream)
				.forEach(member -> guild.moveVoiceMember(member, defaultVoiceChannel).queue());

		voice1.delete().queueAfter(1, TimeUnit.SECONDS);
		voice2.delete().queueAfter(1, TimeUnit.SECONDS);
		category.delete().queueAfter(1, TimeUnit.SECONDS);

		event.getMessageChannel()
				.deleteMessageById(event.getMessageIdLong())
				.queue(v -> event.getMessageChannel().sendMessageEmbeds(messageEmbed).queue());
	}
}
