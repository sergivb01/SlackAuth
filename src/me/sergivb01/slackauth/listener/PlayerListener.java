package me.sergivb01.slackauth.listener;

import me.sergivb01.slackauth.Auth;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import redis.clients.jedis.Jedis;

import java.util.Random;

public class PlayerListener implements Listener{
    private Auth instance;

    public PlayerListener(){
        instance = Auth.getInstance();
        Bukkit.getPluginManager().registerEvents(this, instance);
    }

    @EventHandler
    public void onAsyncChat(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        if(player.hasPermission("auth.require")){
            if(event.getMessage().equals(getPlayerCode(player.getName()))){
                setAuthed(player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', instance.getConfig().getString("messages.authed")));
                event.setCancelled(true);
            }else{
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', instance.getConfig().getString("messages.need-auth")));
                event.setCancelled(true);
            }
        }
    }

    /*@EventHandler //Now using: "auth username" => multilobby support
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();

        if(player.hasPermission("auth.require") && !playerIsAuthed(player)){
            setCode(player.getName());
        }
    }*/

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event){
        Player player = event.getPlayer();
        if(player.hasPermission("auth.require")){
            if(!playerIsAuthed(player)){
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', instance.getConfig().getString("messages.need-auth")));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteractEvent(PlayerInteractEvent event){
        Player player = event.getPlayer();

        if(player.hasPermission("auth.require")){
            if(!playerIsAuthed(player)){
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', instance.getConfig().getString("messages.need-auth")));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInvOpen(InventoryOpenEvent event){
        Player player = (Player) event.getPlayer();

        if(player.hasPermission("auth.require")){
            if(!playerIsAuthed(player)){
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', instance.getConfig().getString("messages.need-auth")));
                player.closeInventory();
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onItemClick(InventoryClickEvent event){
        Player player = (Player) event.getWhoClicked();

        if(player.hasPermission("auth.require")){
            if(!playerIsAuthed(player)){
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', instance.getConfig().getString("messages.need-auth")));
                player.closeInventory();
                event.setCancelled(true);
            }
        }
    }

    private boolean playerIsAuthed(Player player){
        boolean toReturn;
        Jedis jedis = null;
        try {
            jedis = instance.getPool().getResource();
            String lastIP = jedis.hget("auth:players", player.getName());
            toReturn = (lastIP != null && lastIP.equals(player.getAddress().getHostString()));
            instance.getPool().returnResource(jedis);
        }finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return toReturn;
    }

    private void setAuthed(Player player){
        Jedis jedis = null;
        try {
            jedis = instance.getPool().getResource();
            jedis.hset("auth:players", player.getName(), player.getAddress().getHostString());
            if(instance.getConfig().getBoolean("slack.enabled")){
                Auth.getSlackBot().sendMessage(instance.getConfig().getString("messages.bot.player-authed")
                        .replace("%player%", player.getName()));
            }
            if(instance.getConfig().getBoolean("discord.enabled")){
                Auth.getDiscordBot().sendMessage(instance.getConfig().getString("messages.bot.player-authed")
                        .replace("%player%", player.getName()));
            }
            instance.getPool().returnResource(jedis);
        }finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public void setCode(String playerName, String service){
        new Thread(()->{
            try {
                Thread.sleep(instance.getConfig().getInt("server-num"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(getPlayerCode(playerName) != null){
                instance.getLogger().info("Code for " + playerName + " already exists. Skipping");
                return;
            }

            Random rnd = new Random();
            int n = 100000 + rnd.nextInt(900000);
            Jedis jedis = null;
            try {
                jedis = instance.getPool().getResource();
                jedis.hset("auth:codes", playerName, String.valueOf(n));
                if(service.equals("slack")){
                    Auth.getSlackBot().sendMessage(instance.getConfig().getString("messages.bot.code-set")
                            .replace("%player%", playerName)
                            .replace("%pin%", String.valueOf(n)));
                }
                if(service.equals("discord")){
                    Auth.getDiscordBot().sendMessage(instance.getConfig().getString("messages.bot.code-set")
                            .replace("%player%", playerName)
                            .replace("%pin%", String.valueOf(n)));
                }
                instance.getPool().returnResource(jedis);
            }finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }).start();
    }

    public void removeCode(String playerName){
        new Thread(()-> {
            try {
                Thread.sleep(instance.getConfig().getInt("server-num"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Jedis jedis = null;
            try {
                jedis = instance.getPool().getResource();
                jedis.hdel("auth:codes", playerName);
                instance.getPool().returnResource(jedis);
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }).start();
    }

    private String getPlayerCode(String playerName){
        String toReturn;
        Jedis jedis = null;
        try {
            jedis = instance.getPool().getResource();
            toReturn = jedis.hget("auth:codes", playerName);
            instance.getPool().returnResource(jedis);
        }finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return toReturn;
    }

}
