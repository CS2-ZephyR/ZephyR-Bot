package com.github.ioloolo.zephyrbot.interaction.dropdown;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;

@Component
public class DropdownBridge extends ListenerAdapter {

	private final Map<String, InteractionInterface<StringSelectInteractionEvent>>                            dropdowns = new HashMap<>();
	private final Map<Class<? extends InteractionInterface<StringSelectInteractionEvent>>, StringSelectMenu> instances = new HashMap<>();

	public void registerDropdown(
			StringSelectMenu dropdown, InteractionInterface<StringSelectInteractionEvent> dropdownInterface
	) {

		dropdowns.put(dropdown.getId(), dropdownInterface);

		//noinspection unchecked
		instances.put(
				(Class<? extends InteractionInterface<StringSelectInteractionEvent>>) dropdownInterface.getClass(),
				dropdown
		);
	}

	public StringSelectMenu getDropdown(Class<? extends InteractionInterface<StringSelectInteractionEvent>> clazz) {

		return instances.get(clazz);
	}

	@Override
	public void onStringSelectInteraction(StringSelectInteractionEvent event) {

		String id = event.getComponentId();

		if (!dropdowns.containsKey(id)) {
			return;
		}

		dropdowns.get(id).onInteraction(event);
	}
}
