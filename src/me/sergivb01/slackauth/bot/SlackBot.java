package me.sergivb01.slackauth.bot;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import lombok.Getter;
import me.sergivb01.slackauth.Auth;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.Proxy;

public class SlackBot{
	@Getter
	private SlackSession session;
	private Auth instance;
	private String channel;

	public SlackBot(String token, String channel, boolean proxy, String proxyURL, int proxyPort){
		instance = Auth.getInstance();
		this.channel = channel;
		new Thread(() -> { //New thread => i don't want to crash :(
			init(token, proxy, proxyURL, proxyPort);
			registerEvents();
		}).start();
	}

	private void init(String token, boolean proxy, String proxyURL, int proxyPort){
		if(proxy){ //Using proxy?
			session = SlackSessionFactory.getSlackSessionBuilder(token).withAutoreconnectOnDisconnection(true).withProxy(Proxy.Type.SOCKS, proxyURL, proxyPort).build();
		}else{
			session = SlackSessionFactory.getSlackSessionBuilder(token).withAutoreconnectOnDisconnection(true).build();
		}

		try{
			session.connect();
		}catch(IOException e){
			e.printStackTrace();
		}
		sendMessage(instance.getConfig().getString("messages.bot.connected-message")); //Hello World!
	}

	private void registerEvents(){
		session.addMessagePostedListener((slackMessagePosted, slackSession) -> {
			if(!slackMessagePosted.getChannel().getName().equals(channel) || slackMessagePosted.getSender().isBot()){
				return;
			}

			String[] args = slackMessagePosted.getMessageContent().split("\\s+"); //Space

			if(args.length > 0){
				String player = args[1];
				switch(args[0].toLowerCase()){
					case "auth":
						instance.getPlayerListener().setCode(player, "slack");
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
		});
		instance.getLogger().info("Events registered");
	}

	public void sendMessage(String message){
		new Thread(() -> {
			SlackChannel slackChannel = session.findChannelByName(channel);
			session.sendMessage(slackChannel, message);
			Auth.getInstance().getLogger().info("[Slack] [Debug] Sent \"" + message + "\" to Slack channel.");
		}).start();
	}


}
