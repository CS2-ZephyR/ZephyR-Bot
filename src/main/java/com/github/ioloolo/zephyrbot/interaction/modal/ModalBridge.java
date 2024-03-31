package com.github.ioloolo.zephyrbot.interaction.modal;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.Modal;

import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;

@Component
public class ModalBridge extends ListenerAdapter {

	private final Map<String, InteractionInterface<ModalInteractionEvent>>                 modals    = new HashMap<>();
	private final Map<Class<? extends InteractionInterface<ModalInteractionEvent>>, Modal> instances = new HashMap<>();

	public void registerModal(
			Modal modal, InteractionInterface<ModalInteractionEvent> modalInterface
	) {

		modals.put(modal.getId(), modalInterface);

		//noinspection unchecked
		instances.put(
				(Class<? extends InteractionInterface<ModalInteractionEvent>>) modalInterface.getClass(),
				modal
		);
	}

	public Modal getModal(Class<? extends InteractionInterface<ModalInteractionEvent>> clazz) {

		return instances.get(clazz);
	}

	@Override
	public void onModalInteraction(ModalInteractionEvent event) {

		String id = event.getModalId();

		if (!modals.containsKey(id)) {
			return;
		}

		modals.get(id).onInteraction(event);
	}
}
