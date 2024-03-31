package com.github.ioloolo.zephyrbot;

import java.awt.*;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import com.github.ioloolo.zephyrbot.interaction.button.ButtonBridge;
import com.github.ioloolo.zephyrbot.interaction.button.LinkAccountButton;
import com.github.ioloolo.zephyrbot.interaction.command.CommandBridge;
import com.github.ioloolo.zephyrbot.interaction.dropdown.DropdownBridge;
import com.github.ioloolo.zephyrbot.interaction.modal.ModalBridge;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@RequiredArgsConstructor
public class ZephyrBotApplication {

	public static void main(String[] args) {

		SpringApplication.run(ZephyrBotApplication.class, args);
	}

	@Slf4j
	@Component
	@RequiredArgsConstructor
	public static class ZephyrBot {

		@Value("${discord.token}")
		private String token;

		@Value("${discord.channel.chatting.bot}")
		private long botChannel;

		@Getter
		private JDA jda;

		private final CommandBridge  commandBridge;
		private final ButtonBridge   buttonBridge;
		private final DropdownBridge dropdownBridge;
		private final ModalBridge    modalBridge;

		@PostConstruct
		@SneakyThrows(InterruptedException.class)
		public void init() {

			this.jda = JDABuilder.createDefault(token)
					.setAutoReconnect(true)
					.setActivity(Activity.competing("ZephyR"))
					.addEventListeners(commandBridge, buttonBridge, dropdownBridge, modalBridge)
					.build()
					.awaitReady();

			sendRegisterInfo();

			log.info("Success fetch bot data. ({})", jda.getSelfUser().getName());
		}

		private void sendRegisterInfo() {

			TextChannel channel = jda.getTextChannelById(botChannel);
			if (channel == null) {
				return;
			}

			channel.retrievePinnedMessages().queue(pinned -> {
				if (!pinned.isEmpty()) {
					return;
				}

				List<String> process = List.of(
						"1. 위 페이지에 접속한다.",
						"2. 로그인 버튼을 누른 후, 스팀 로그인을 한다.",
						"3. 페이지에 \"디스코드에서 연동을 해주세요.\"라는 문구가 뜨면, 아래 \"연동\" 버튼을 누른다.",
						"4. 연동 완료 메시지를 확인한다."
				);

				MessageEmbed messageEmbed = new EmbedBuilder().setTitle("가입하기")
						.setDescription("서비스를 이용하기 위해 **ZephyR Network**에 가입해야 합니다.")
						.addField(new MessageEmbed.Field("주소", "http://teamzephyr.kro.kr", false))
						.addField(new MessageEmbed.Field("방법", String.join("\n", process), false))
						.setThumbnail("https://cdn.discordapp.com/app-icons/1081544900539076639/f58a0423c26e5fa72c1d1bbdc95f5311.png")
						.setFooter("Team ZephyR")
						.setColor(Color.MAGENTA)
						.build();

				Button button = buttonBridge.getButton(LinkAccountButton.class);

				channel.sendMessageEmbeds(messageEmbed).setActionRow(button)
						.queue(message -> channel.pinMessageById(message.getIdLong()).queue());
			});
		}
	}
}
