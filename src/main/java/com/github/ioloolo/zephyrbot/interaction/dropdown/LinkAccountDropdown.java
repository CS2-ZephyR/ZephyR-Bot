package com.github.ioloolo.zephyrbot.interaction.dropdown;

import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import com.github.ioloolo.zephyrbot.interaction.CommonMethod;
import com.github.ioloolo.zephyrbot.interaction.InteractionInterface;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkAccountDropdown implements InteractionInterface<StringSelectInteractionEvent> {

	private final DropdownBridge dropdownBridge;

	private final CommonMethod commonMethod;

	@PostConstruct
	public void init() {

		StringSelectMenu dropdown = StringSelectMenu.create("link_account").addOption("1", "1").build();

		dropdownBridge.registerDropdown(dropdown, this);
	}

	@Override
	public void onInteraction(StringSelectInteractionEvent event) {

		if (!commonMethod.checkAlreadyRegistered(event)) {
			return;
		}

		SelectOption steamInfo = event.getSelectedOptions().get(0);

		TextInput nicknameField = TextInput.create("nickname", "닉네임", TextInputStyle.SHORT)
				.setRequired(true)
				.setPlaceholder("사용할 닉네임을 입력해주세요.")
				.build();

		TextInput steamField = TextInput.create("steam", "스팀 (수정 금지)", TextInputStyle.SHORT)
				.setValue(steamInfo.getValue())
				.build();

		TextInput discordField = TextInput.create("discord", "디스코드 (수정 금지)", TextInputStyle.SHORT)
				.setValue(event.getUser().getIdLong() + "")
				.build();

		Modal modal = Modal.create("link_account", "계정 연동하기")
				.addComponents(ActionRow.of(nicknameField), ActionRow.of(steamField), ActionRow.of(discordField))
				.build();

		event.replyModal(modal).queue();
	}
}
