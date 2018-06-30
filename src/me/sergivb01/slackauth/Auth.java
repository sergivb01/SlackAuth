package me.sergivb01.slackauth;

import lombok.Getter;
import me.sergivb01.slackauth.bot.DiscordBot;
import me.sergivb01.slackauth.bot.SlackBot;
import me.sergivb01.slackauth.listener.PlayerListener;
import me.sergivb01.slackauth.utils.License;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.io.IOException;
/*
 *      Slack Auth Plugin - Developed by @sergivb01
 *      © Copyright 2017 - 2018 ~ Do not redistribute
 */

public class Auth extends JavaPlugin{
	@Getter
	public static SlackBot slackBot;
	@Getter
	public static DiscordBot discordBot;
	@Getter
	private static Auth instance;
	@Getter
	private JedisPool pool;
	@Getter
	private PlayerListener playerListener;
	private boolean slackEnabled = false;
	private boolean discordEnabled = false;
	private String disconnectMessage;

	public void onEnable(){
		instance = this;

		final File configFile = new File(this.getDataFolder() + "/config.yml");
		if(!configFile.exists()){
			this.saveDefaultConfig();
		}
		this.getConfig().options().copyDefaults(true);

		getConfig().set("hwid", License.getHWID());
		try{
			getConfig().save(configFile);
		}catch(IOException ignored){
		}

        /*if(License.checkLicense()){
            System.out.println("License has been verified");
        }else{
            System.out.println("Invalid HWID. Please send your HWID (found in config file) and send it to @sergivb01 on MCM/Discord.");
            System.out.println(License.getHWID());
            Bukkit.getPluginManager().disablePlugins();
            return; //I don't want to load all the following shit
        }*/

		if(getConfig().getBoolean("redis.auth")){
			pool = new JedisPool(new JedisPoolConfig(), getConfig().getString("redis.host"), getConfig().getInt("redis.port"), getConfig().getInt("redis.timeout"), getConfig().getString("redis.password"));
		}else{
			pool = new JedisPool(new JedisPoolConfig(), getConfig().getString("redis.host"), getConfig().getInt("redis.port"), getConfig().getInt("redis.timeout"));
		}

		if(getConfig().getBoolean("slack.enabled")){
			slackBot = new SlackBot(getConfig().getString("slack.bot-token"), getConfig().getString("slack.channel"), getConfig().getBoolean("connection.proxy"), getConfig().getString("connection.url"), getConfig().getInt("connection.port"));
			slackEnabled = true;
		}

		if(getConfig().getBoolean("discord.enabled")){
			discordBot = new DiscordBot(getConfig().getString("discord.bot-token"), getConfig().getString("discord.channel"));
			discordEnabled = true;
		}

		disconnectMessage = getConfig().getString("messages.bot.disconnected-message");

		playerListener = new PlayerListener();

	}

	public void onDisable(){
		if(slackEnabled){
			slackBot.sendMessage(disconnectMessage);
			try{
				slackBot.getSession().disconnect();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		if(discordEnabled){
			discordBot.sendMessage(disconnectMessage);
		}

		this.pool.destroy();

		slackBot = null;
		instance = null;
	}

}
