package com.github.ioloolo.zephyrbot.interaction;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

import com.github.ioloolo.zephyrbot.data.Match;
import com.github.ioloolo.zephyrbot.data.User;
import com.github.ioloolo.zephyrbot.repository.MatchRepository;
import com.github.ioloolo.zephyrbot.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommonMethod {

	private final UserRepository  userRepository;
	private final MatchRepository matchRepository;

	@Value("${discord.owner}")
	private long owner;

	public boolean checkPermission(IReplyCallback callback) {

		if (callback.getUser().getIdLong() != owner) {
			callback.deferReply(true).queue(hook -> {
				MessageEmbed messageEmbed = new EmbedBuilder().setTitle("권한이 없습니다.")
						.setDescription("당신은 권한이 없습니다.")
						.setFooter("Team ZephyR")
						.setColor(Color.RED)
						.build();

				hook.editOriginalEmbeds(messageEmbed).queue();
			});

			return false;
		}

		return true;
	}

	public boolean checkRunningMatch(IReplyCallback callback) {

		if (matchRepository.findByEndIsFalse().isPresent()) {
			MessageEmbed messageEmbed = new EmbedBuilder().setTitle("오류가 발생했습니다.")
					.setDescription("진행중인 경기가 존재합니다. 경기 종료 후 시도해주세요.")
					.setFooter("Team ZephyR")
					.setColor(Color.RED)
					.build();

			callback.replyEmbeds(messageEmbed).queue(hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));

			return false;
		}

		return true;
	}

	public boolean checkHasMatch(IReplyCallback callback) {

		Optional<Match> matchOptional = matchRepository.findByEndIsFalse();
		if (matchOptional.isEmpty()) {
			MessageEmbed messageEmbed = new EmbedBuilder().setTitle("오류가 발생했습니다.")
					.setDescription("매치 데이터가 존재하지 않습니다.")
					.setFooter("Team ZephyR")
					.setColor(Color.RED)
					.build();

			callback.replyEmbeds(messageEmbed).queue(hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));

			return false;
		}

		return true;
	}

	public boolean checkUserRegister(IReplyCallback callback) {

		Optional<User> userOptional = userRepository.findByDiscord(callback.getUser().getIdLong());

		if (userOptional.isEmpty() || userOptional.get().isBan()) {
			callback.deferReply(true).queue(hook -> {
				MessageEmbed messageEmbed = new EmbedBuilder().setTitle("권한이 없습니다.")
						.setDescription("가입된 유저가 아닙니다.")
						.setFooter("Team ZephyR")
						.setColor(Color.RED)
						.build();

				hook.editOriginalEmbeds(messageEmbed).queue();
			});

			return false;
		}

		return true;
	}

	public List<User> getMatchTeam1User(Match match) {

		return match.getTeam1()
				.getMember()
				.stream()
				.map(userRepository::findBySteamId)
				.map(Optional::orElseThrow)
				.toList();
	}

	public List<User> getMatchTeam2User(Match match) {

		return match.getTeam2()
				.getMember()
				.stream()
				.map(userRepository::findBySteamId)
				.map(Optional::orElseThrow)
				.toList();
	}
}
