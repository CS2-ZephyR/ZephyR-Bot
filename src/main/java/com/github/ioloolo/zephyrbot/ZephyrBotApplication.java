package com.github.ioloolo.zephyrbot;

import java.awt.*;

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

import com.github.ioloolo.zephyrbot.interaction.button.ButtonBridge;
import com.github.ioloolo.zephyrbot.interaction.command.CommandBridge;
import com.github.ioloolo.zephyrbot.interaction.dropdown.DropdownBridge;

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

		@Value("${discord.advertise-channel}")
		private long advertiseChannel;

		@Getter
		private JDA jda;

		private final CommandBridge  commandBridge;
		private final ButtonBridge   buttonBridge;
		private final DropdownBridge dropdownBridge;

		@PostConstruct
		@SneakyThrows(InterruptedException.class)
		public void init() {

			this.jda = JDABuilder.createDefault(token)
					.setAutoReconnect(true)
					.setActivity(Activity.competing("ZephyR"))
					.addEventListeners(commandBridge, buttonBridge, dropdownBridge)
					.build()
					.awaitReady();

			sendWebAd();

			log.info("Success fetch bot data. ({})", jda.getSelfUser().getName());
		}

		private void sendWebAd() {

			TextChannel channel = jda.getTextChannelById(advertiseChannel);
			if (channel == null) {
				return;
			}

			channel.retrievePinnedMessages().queue(pinned -> {
				if (!pinned.isEmpty()) {
					return;
				}

				MessageEmbed messageEmbed = new EmbedBuilder().setTitle("웹페이지")
						.addField(new MessageEmbed.Field("주소", "http://teamzephyr.kro.kr", true))
						.setThumbnail("https://cdn.discordapp.com/app-icons/1081544900539076639/f58a0423c26e5fa72c1d1bbdc95f5311.png")
						.setFooter("Team ZephyR")
						.setColor(Color.YELLOW)
						.build();

				channel.sendMessageEmbeds(messageEmbed).queue(message -> channel.pinMessageById(message.getIdLong()).queue());
			});
		}
	}
}
