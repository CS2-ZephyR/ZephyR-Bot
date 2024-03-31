package com.github.ioloolo.zephyrbot.interaction.button;

import java.awt.*;
import java.util.List;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import com.github.ioloolo.zephyrbot.data.User;
import com.github.ioloolo.zephyrbot.interaction.CommonMethod;
import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;
import com.github.ioloolo.zephyrbot.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkAccountButton implements InteractionInterface<ButtonInteractionEvent> {

	private final ButtonBridge buttonBridge;

	private final UserRepository userRepository;

	private final CommonMethod commonMethod;

	@PostConstruct
	public void init() {

		Button button = Button.success("link_account", "연동");

		buttonBridge.registerButton(button, this);
	}

	@Override
	public void onInteraction(ButtonInteractionEvent event) {

		List<User> notRegisteredUser = userRepository.findAllByBanIsTrue();
		List<SelectOption> options = notRegisteredUser.stream().map(x -> SelectOption.of(x.getName(), x.getSteamId()+"")).toList();

		if (!commonMethod.checkAlreadyRegistered(event) || !checkOptionsIsNotEmpty(event, options)) {
			return;
		}

		StringSelectMenu dropdown = StringSelectMenu.create("link_account").addOptions(options).build();

		event.deferReply(true).queue(hook -> {
			MessageEmbed messageEmbed = new EmbedBuilder().setTitle("가입하기")
					.setDescription("아래에서 본인 스팀 닉네임을 선택해주세요.")
					.setFooter("Team ZephyR")
					.setColor(Color.MAGENTA)
					.build();

			hook.editOriginalEmbeds(messageEmbed).setActionRow(dropdown).queue();
		});
	}

	private boolean checkOptionsIsNotEmpty(ButtonInteractionEvent event, List<SelectOption> options) {
		if (options.isEmpty()) {
			event.deferReply(true).queue(hook -> {
				MessageEmbed messageEmbed = new EmbedBuilder().setTitle("오류가 발생했습니다.")
						.setDescription("가입 대기중인 유저가 존재하지 않습니다.")
						.setFooter("Team ZephyR")
						.setColor(Color.RED)
						.build();

				hook.editOriginalEmbeds(messageEmbed).queue();
			});

			return false;
		}

		return true;
	}
}
