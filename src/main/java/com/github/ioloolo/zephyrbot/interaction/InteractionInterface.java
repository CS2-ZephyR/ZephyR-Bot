package com.github.ioloolo.zephyrbot.interaction;

import net.dv8tion.jda.api.events.Event;

public interface InteractionInterface<T extends Event> {

	void onInteraction(T event);
}
