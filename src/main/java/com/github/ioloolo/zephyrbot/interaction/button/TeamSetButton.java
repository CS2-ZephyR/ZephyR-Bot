package com.github.ioloolo.zephyrbot.interaction.button;

import java.awt.*;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import com.github.ioloolo.zephyrbot.interaction.CommonMethod;
import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Component
@RequiredArgsConstructor
public class TeamSetButton implements InteractionInterface<ButtonInteractionEvent> {

	private final ButtonBridge buttonBridge;

	private final CommonMethod commonMethod;

	@Getter
	@Setter
	private static long message;

	@PostConstruct
	public void init() {

		Button button = Button.primary("team_set", "지정");

		buttonBridge.registerButton(button, this);
	}

	@Override
	public void onInteraction(ButtonInteractionEvent event) {

		if (!commonMethod.checkPermission(event) || !commonMethod.checkHasMatch(event)) {
			return;
		}

		event.getMessageChannel().deleteMessageById(event.getMessageIdLong()).queue(v -> {
			MessageEmbed messageEmbed = new EmbedBuilder().setTitle("팀원 구성")
					.addField(new MessageEmbed.Field("1팀", "", true))
					.addField(new MessageEmbed.Field("2팀", "", true))
					.setFooter("Team ZephyR")
					.setColor(Color.GREEN)
					.build();

			event.getMessageChannel()
					.sendMessageEmbeds(messageEmbed)
					.queue(message -> TeamSetButton.message = message.getIdLong());
		});
	}
}
