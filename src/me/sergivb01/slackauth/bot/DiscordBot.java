package me.sergivb01.slackauth.bot;

import lombok.Getter;
import me.sergivb01.slackauth.Auth;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import javax.security.auth.login.LoginException;

public class DiscordBot extends ListenerAdapter{
	@Getter
	private JDA session;
	private Auth instance;
	private String channel;

	public DiscordBot(String token, String channel){
		instance = Auth.getInstance();
		this.channel = channel;
		new Thread(() -> init(token)).start(); //New thread => i don't want to crash :(
	}

	private void init(String token){
		try{
			session = new JDABuilder(AccountType.BOT).setToken(token).setAutoReconnect(true).setGame(Game.of(Game.GameType.DEFAULT, "2FA Bot")).buildBlocking();
			session.addEventListener(this); //Implement MessageRecivedEvent
		}catch(LoginException | InterruptedException | RateLimitedException e){
			e.printStackTrace();
		}
		sendMessage(instance.getConfig().getString("messages.bot.connected-message")); //Hello World!
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event){
		if(!event.getChannel().getName().equals(channel) || event.getAuthor().isBot()){
			return;
		}

		String message = event.getMessage().getContentDisplay();
		String[] args = message.split("\\s+"); //Space

		if(args.length > 1){
			String player = args[1];
			switch(args[0].toLowerCase()){
				case "auth":
					instance.getPlayerListener().setCode(player, "discord");
					break;
				case "delcode":
					instance.getPlayerListener().removeCode(player);
					sendMessage(instance.getConfig().getString("messages.bot.code-removed").replace("%player%", player));
					break;
				case "ban":
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), instance.getConfig().getString("commands.ban").replace("%player%", player));
					sendMessage(instance.getConfig().getString("messages.bot.player-banned").replace("%player%", player));
					break;
				case "unban":
				case "pardon":
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), instance.getConfig().getString("commands.unban").replace("%player%", player));
					sendMessage(instance.getConfig().getString("messages.bot.player-unbanned").replace("%player%", player));
					break;
				default:
					break;
			}
		}
	}

	public void sendMessage(String message){
		new Thread(() -> {
			MessageChannel messageChannel = session.getTextChannelsByName(channel, true).get(0);
			messageChannel.sendMessage(message).submit();
			Auth.getInstance().getLogger().info("[Discord] [Debug] Sent \"" + message + "\" to Discord channel.");
		}).start();
	}


}
