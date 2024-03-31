package com.github.ioloolo.zephyrbot.socket;

import java.awt.*;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import com.github.ioloolo.zephyrbot.data.Match;
import com.github.ioloolo.zephyrbot.interaction.button.ButtonBridge;
import com.github.ioloolo.zephyrbot.interaction.button.MatchEndButton;
import com.github.ioloolo.zephyrbot.repository.MatchRepository;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

	private final MatchRepository matchRepository;

	private final ButtonBridge buttonBridge;

	private Message message;

	@Getter
	private long category;

	@Getter
	private long voice1;
	@Getter
	private long voice2;

	@Value("${discord.channel.voice.default}")
	private long defaultVoice;

	@Override
	public void handleTextMessage(@Nonnull WebSocketSession session, @Nonnull TextMessage textMessage) {

		String payload = textMessage.getPayload();

		if (payload.equals("server_open")) {
			MessageEmbed originalEmbed = message.getEmbeds().get(0);
			MessageEmbed messageEmbed = new EmbedBuilder(originalEmbed).setTitle("경기 진행")
					.setDescription("경기가 진행중입니다.\n\n[경기 참여](https://teamzephyr.kro.kr/connect)\n\n\n*(만약 경기가 자동으로 종료되지 않을 경우, 아래 버튼을 사용해주세요.)*")
					.setColor(Color.GREEN)
					.build();

			Button button = buttonBridge.getButton(MatchEndButton.class);

			log.info("[Match Start] 서버 준비 완료.");

			message.getChannel()
					.deleteMessageById(message.getIdLong())
					.queue(v -> message.getChannel().sendMessageEmbeds(messageEmbed).setActionRow(button).queue(message1 -> message = message1));
		} else if (payload.equals("server_close")) {
			Match match = matchRepository.findByEndIsFalse().orElseThrow();
			match.setEnd(true);
			matchRepository.save(match);

			MessageEmbed originalEmbed = message.getEmbeds().get(0);
			MessageEmbed messageEmbed = new EmbedBuilder(originalEmbed).setTitle("경기 종료")
					.setDescription("경기가 종료되었습니다.")
					.setColor(Color.RED)
					.build();

			log.info("[Match End] 경기 종료.");

			Guild guild = message.getGuild();

			VoiceChannel defaultVoiceChannel = guild.getVoiceChannelById(defaultVoice);

			Category category = guild.getCategoryById(getCategory());
			VoiceChannel voice1 = guild.getVoiceChannelById(getVoice1());
			VoiceChannel voice2 = guild.getVoiceChannelById(getVoice2());

			if (defaultVoiceChannel == null || category == null || voice1 == null || voice2 == null) {
				return;
			}

			Stream.of(voice1.getMembers(), voice2.getMembers())
					.flatMap(Collection::stream)
					.forEach(member -> guild.moveVoiceMember(member, defaultVoiceChannel).queue());

			voice1.delete().queueAfter(1, TimeUnit.SECONDS);
			voice2.delete().queueAfter(1, TimeUnit.SECONDS);
			category.delete().queueAfter(1, TimeUnit.SECONDS);

			message.getChannel()
					.deleteMessageById(message.getIdLong())
					.queue(v -> message.getChannel().sendMessageEmbeds(messageEmbed).queue(v2 -> message = null));
		}
	}
}
