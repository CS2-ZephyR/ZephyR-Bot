package com.github.ioloolo.zephyrbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

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

			log.info("Success fetch bot data. ({})", jda.getSelfUser().getName());
		}
	}
}
