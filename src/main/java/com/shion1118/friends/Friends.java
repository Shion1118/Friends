package com.shion1118.friends;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.shion1118.friends.FriendEvent;

public class Friends extends JavaPlugin {

	public static File friendFile;
	public static FileConfiguration frienddata;

	HashMap<Player, HashMap<Player, Integer>> request = new HashMap<Player, HashMap<Player, Integer>>();

    @Override
    public void onEnable() {

    	registerEvents();

        try {
			setupConfig();
		} catch (IOException | InvalidConfigurationException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
    }

    @Override
    public void onDisable() {
    	getServer().getConsoleSender().sendMessage("[Friends]" + ChatColor.DARK_AQUA + " Disable");
    }

    private void registerEvents() {
    	PluginManager pm = Bukkit.getPluginManager();
    	pm.registerEvents(new FriendEvent(), this);
    }

    private void setupConfig() throws IOException, InvalidConfigurationException{
    	//FriendData Config
    	friendFile = new File(getDataFolder(), "friend.yml");
    	if(!friendFile.exists()){
    		friendFile.getParentFile().mkdirs();         // creates the /plugins/<pluginName>/ directory if not found
            copy(getResource("friend.yml"),friendFile);
    	}
    	frienddata = new YamlConfiguration();
    	frienddata.load(friendFile);
    }

    private void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	if (sender instanceof Player) {
    		Player p = (Player) sender;
    		if(cmd.getName().equalsIgnoreCase("friend")){
    			if(args.length == 2){
    				if(args[0].equalsIgnoreCase("request")){
    					requestFriend(p,args[1]);
    					return true;
    				}
    				if(args[0].equalsIgnoreCase("accept")){
    					acceptFriend(p,args[1]);
    					return true;
    				}
    				if(args[0].equalsIgnoreCase("info")){
    					infoFriend(p,args[1]);
    					return true;
    				}
    				if(args[0].equalsIgnoreCase("remove")){
    					removeFriend(p,args[1]);
    					return true;
    				}
    			}
    			if(args.length == 1){
    				if(args[0].equalsIgnoreCase("list")){
        				listFriend(p);
        				return true;
        			}
    			}

    			p.sendMessage("§6Friends Command List");
    			p.sendMessage("§6/friend request <name>");
    			p.sendMessage("§6/friend accept <name>");
    			p.sendMessage("§6/friend info <name>");
    			p.sendMessage("§6/friend remove <name>");
    			p.sendMessage("§6/friend list");
    			p.sendMessage("§6This plugin made by Shion.");

    		}
    		return true;
    	} else {
    		sender.sendMessage("This Command for Player!");
    		return false;
    	}
    }

    public void requestFriend(Player p, String name){
    	Player friend = Bukkit.getServer().getPlayer(name);
    	HashMap<Player, Integer> list = new HashMap<Player, Integer>();
    	if(friend == null){
    		p.sendMessage(name + " §adoes not online");
    		return;
    	}
    	if(frienddata.contains(p.getName() + "." + name)){
    		p.sendMessage(name + "§e is already your Friend.");
    		return;
    	}

    	if(request.containsKey(p)){
    		list = request.get(p);
    		if(list.containsKey(friend)){
    			p.sendMessage("§aYou already send Friend request to§f"+ name + "§a.");
    			return;
    		}
    	}

    	p.sendMessage("§eSend Friend request to §f" + name);

    	friend.sendMessage("§f" + p.getName() +"§e send Friend request to you.");
    	friend.sendMessage("§eIf you accept, please type §f/friend accept " + p.getName());

    	list.put(friend, 0);
    	request.put(p, list);
    	requestTime(p,friend,name);

    	return;
    }

    public void acceptFriend(Player p, String name){
    	Player friend = Bukkit.getServer().getPlayer(name);
    	HashMap<Player, Integer> list = new HashMap<Player, Integer>();
    	if(!checkOnline(name)){
    		p.sendMessage(name + " §adoes not online");
    		return;
    	}
    	if(!request.containsKey(friend)){
    		p.sendMessage(name + " §adoes not send Friend request to you.");
    		return;
    	} else if(!request.get(friend).containsKey(p)){
    		p.sendMessage(name + " §adoes not send Friend request to you.");
    		return;
    	}

    	list = request.get(friend);

    	Bukkit.getServer().getScheduler().cancelTask(list.get(p));

    	list.remove(p);
    	request.put(friend, list);

    	frienddata.set(name+ "." + p.getName(), "");
    	frienddata.set(p.getName() + "." + name, "");

    	p.sendMessage("§eAdd Friend §f" + name + "§e.");
    	friend.sendMessage(p.getName() + " §eAccept your Friend request.");

    	try {
			frienddata.save(friendFile);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

    	return;
    }

    public void requestTime(final Player p,final Player friend, final String name){
    	HashMap<Player, Integer> list = new HashMap<Player, Integer>();
    	int requestID = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
    		public void run(){
    			request.remove(p);
    			p.sendMessage(name + " §adose not accept your Friend request.");
    		}
    	}, 1200L);
    	list = request.get(p);
    	list.put(friend, requestID);
    	request.put(p, list);
    }

    public void infoFriend(Player p,String name){
    	if(!frienddata.contains(p.getName() + "." + name)){
    		p.sendMessage(name + " §ais not your Friend.");
    		return;
    	}

    	String online;
    	if(checkOnline(name)){
    		online = "§bOnline";
    	}else{
    		online = "§cOffline";
    	}

    	p.sendMessage("§e-----Friend Info-----");
    	p.sendMessage("§eID:§f" + name + " §eStatus:" + online);
    	if(checkOnline(name)){
    		p.sendMessage("§eLocationX: §f" + String.format("%.1f", checkLocationX(name)));
    		p.sendMessage("§eLocationY: §f" + String.format("%.1f", checkLocationY(name)));
    		p.sendMessage("§eLocationZ: §f" + String.format("%.1f", checkLocationZ(name)));
    	}

    	return;
    }

    public void removeFriend(Player p,String name){
    	if(!frienddata.contains(p.getName() + "." + name)){
    		p.sendMessage(name + " §ais not your Friend.");
    		return;
    	}

    	frienddata.set(p.getName() + "." + name, null);
    	frienddata.set(name + "." + p.getName(), null);
    	p.sendMessage("§eRemove your Friend §f" + name);

    	try {
			frienddata.save(friendFile);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

    	return;
    }

    public void listFriend(Player p){
    	if(frienddata.isConfigurationSection(p.getName())){
    		for (String s :frienddata.getConfigurationSection(p.getName()).getKeys(false)){
    			p.sendMessage(s);
    		}
    	}
    	return;
    }

    public boolean checkOnline(String name){
    	Player p = Bukkit.getServer().getPlayer(name);
    	if(p == null){
    		return false;
    	}else{
    		return true;
    	}

    }

    public double checkLocationX(String name){
    	Player p = Bukkit.getServer().getPlayer(name);
    	if(p == null){
    		return 0;
    	}

    	return p.getLocation().getX();
    }

    public double checkLocationY(String name){
    	Player p = Bukkit.getServer().getPlayer(name);
    	if(p == null){
    		return 0;
    	}

    	return p.getLocation().getY();
    }

    public double checkLocationZ(String name){
    	Player p = Bukkit.getServer().getPlayer(name);
    	if(p == null){
    		return 0;
    	}

    	return p.getLocation().getZ();
    }

}
