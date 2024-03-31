package com.github.ioloolo.zephyrbot.interaction.modal;

import java.awt.*;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

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
public class LinkAccountModal implements InteractionInterface<ModalInteractionEvent> {

	private final ModalBridge modalBridge;

	private final UserRepository userRepository;

	private final CommonMethod commonMethod;

	@Value("${discord.channel.chatting.default}")
	private long chattingChannelId;

	@Value("${discord.role.member}")
	private long memberRoleId;

	@PostConstruct
	public void init() {

		TextInput nicknameField = TextInput.create("nickname", "닉네임", TextInputStyle.SHORT)
				.setRequired(true)
				.setPlaceholder("사용할 닉네임을 입력해주세요.")
				.build();

		TextInput steamField = TextInput.create("steam", "스팀", TextInputStyle.SHORT)
				.setRequired(true)
				.setPlaceholder("스팀 ID를 입력해주세요.")
				.build();

		TextInput discordField = TextInput.create("discord", "디스코드", TextInputStyle.SHORT)
				.setRequired(true)
				.setPlaceholder("스팀 ID를 입력해주세요.")
				.build();

		Modal modal = Modal.create("link_account", "계정 연동하기")
				.addComponents(ActionRow.of(nicknameField), ActionRow.of(steamField), ActionRow.of(discordField))
				.build();

		modalBridge.registerModal(modal, this);
	}

	@Override
	public void onInteraction(ModalInteractionEvent event) {

		if (!commonMethod.checkAlreadyRegistered(event)) {
			return;
		}

		String nickname = Objects.requireNonNull(event.getValue("nickname")).getAsString();
		String steam = Objects.requireNonNull(event.getValue("steam")).getAsString();
		String discord = Objects.requireNonNull(event.getValue("discord")).getAsString();

		if (!verifySteam(event, steam) || !verifyDiscord(event, discord)) {
			return;
		}

		User user = userRepository.findBySteamId(Long.parseLong(steam)).orElseThrow();
		user.setName(nickname);
		user.setDiscord(Long.parseLong(discord));
		user.setBan(false);

		userRepository.save(user);

		Guild guild = Objects.requireNonNull(event.getGuild());
		Member member = Objects.requireNonNull(event.getMember());

		member.modifyNickname(nickname).queue();
		guild.modifyMemberRoles(member, guild.getRoleById(memberRoleId)).queue();

		MessageEmbed messageEmbed = new EmbedBuilder().setTitle("가입 완료")
				.setDescription("<@%d>님이 **ZephyR Network**에 가입하셨습니다.".formatted(member.getIdLong()))
				.setThumbnail(event.getUser().getAvatarUrl())
				.setFooter("Team ZephyR")
				.setColor(Color.GREEN)
				.build();

		Objects.requireNonNull(guild.getTextChannelById(chattingChannelId))
				.sendMessageEmbeds(messageEmbed)
				.queue();

		event.deferReply(true).queue(hook -> {
			MessageEmbed messageEmbed2 = new EmbedBuilder().setTitle("가입 완료")
					.setDescription("**ZephyR Network**에 가입되었습니다.")
					.setFooter("Team ZephyR")
					.setColor(Color.GREEN)
					.build();

			hook.editOriginalEmbeds(messageEmbed2).queue();
		});
	}

	private boolean verifySteam(ModalInteractionEvent event, String strSteamId) {

		try {
			long steamId = Long.parseLong(strSteamId);

			if (userRepository.findBySteamId(steamId).isEmpty()) {
				event.deferReply(true).queue(hook -> {
					MessageEmbed messageEmbed = new EmbedBuilder().setTitle("오류가 발생했습니다.")
							.setDescription("%d로 가입 대기중인 유저가 존재하지 않습니다.".formatted(steamId))
							.setFooter("Team ZephyR")
							.setColor(Color.RED)
							.build();

					hook.editOriginalEmbeds(messageEmbed).queue();
				});

				return false;
			}

			return true;
		} catch (NumberFormatException e) {
			event.deferReply(true).queue(hook -> {
				MessageEmbed messageEmbed = new EmbedBuilder().setTitle("오류가 발생했습니다.")
						.setDescription("스팀 필드 값이 수정되었습니다.")
						.setFooter("Team ZephyR")
						.setColor(Color.RED)
						.build();

				hook.editOriginalEmbeds(messageEmbed).queue();
			});

			return false;
		}
	}

	private boolean verifyDiscord(ModalInteractionEvent event, String strDiscordId) {

		try {
			Long.parseLong(strDiscordId);

			return true;
		} catch (NumberFormatException e) {
			event.deferReply(true).queue(hook -> {
				MessageEmbed messageEmbed = new EmbedBuilder().setTitle("오류가 발생했습니다.")
						.setDescription("디스코드 필드 값이 수정되었습니다.")
						.setFooter("Team ZephyR")
						.setColor(Color.RED)
						.build();

				hook.editOriginalEmbeds(messageEmbed).queue();
			});

			return false;
		}
	}
}
