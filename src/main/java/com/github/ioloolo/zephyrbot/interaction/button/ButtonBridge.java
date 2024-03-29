package com.github.ioloolo.zephyrbot.interaction.button;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;

@Component
public class ButtonBridge extends ListenerAdapter {

	private final Map<String, InteractionInterface<ButtonInteractionEvent>>                  buttons   = new HashMap<>();
	private final Map<Class<? extends InteractionInterface<ButtonInteractionEvent>>, Button> instances = new HashMap<>();

	public void registerButton(Button button, InteractionInterface<ButtonInteractionEvent> buttonInterface) {

		buttons.put(button.getId(), buttonInterface);

		//noinspection unchecked
		instances.put((Class<? extends InteractionInterface<ButtonInteractionEvent>>) buttonInterface.getClass(),
					  button
		);
	}

	public Button getButton(Class<? extends InteractionInterface<ButtonInteractionEvent>> clazz) {

		return instances.get(clazz);
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {

		String id = event.getComponentId();

		if (!buttons.containsKey(id)) {
			return;
		}

		buttons.get(id).onInteraction(event);
	}
}
