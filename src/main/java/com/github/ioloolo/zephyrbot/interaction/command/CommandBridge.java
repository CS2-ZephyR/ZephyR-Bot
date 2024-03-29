package com.github.ioloolo.zephyrbot.interaction.command;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;

@Component
public class CommandBridge extends ListenerAdapter {

	private final Map<String, InteractionInterface<SlashCommandInteractionEvent>> commands = new HashMap<>();

	public void registerCommand(
			CommandData command,
			InteractionInterface<SlashCommandInteractionEvent> commandInterface
	) {

		commands.put(command.getName(), commandInterface);
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

		String command = event.getName();

		if (!commands.containsKey(command)) {
			return;
		}

		commands.get(command).onInteraction(event);
	}
}
