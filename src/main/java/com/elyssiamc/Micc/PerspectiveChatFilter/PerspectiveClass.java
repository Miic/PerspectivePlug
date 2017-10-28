package com.elyssiamc.Micc.PerspectiveChatFilter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.md_5.bungee.api.ChatColor;

public class PerspectiveClass extends JavaPlugin implements Listener {
	
	private static String apiKey;
	private JavaPlugin plugin = this;
	private FileConfiguration config = getConfig();
	
    @Override
    public void onEnable() {
        getLogger().info("Perspective API registered!");
        config.addDefault("apiKey", "");
        config.addDefault("minimumTrigger", 0.9);
        config.addDefault("command", "ping %player%");
        config.addDefault("alterinatinCommand", "ping %player%");
        config.addDefault("debug", false);
        config.options().copyDefaults(true);
        saveConfig();
        apiKey = config.getString("apiKey");
        this.getServer().getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Perspective API unregistered!");
    }
    
    @EventHandler
    public void onChat(final AsyncPlayerChatEvent e) {
    	if (!e.getPlayer().hasPermission("PerspectiveAPI.exempt")) {
	    	new Thread(new Runnable() {
	    		public void run() {
	    			
	    			if (Pattern.compile("([a-z]{1,3}[A-Z]{1,3}){2}( |_|$)").matcher(e.getMessage()).find()) {
						String[] cmds = config.getString("alterinatinCommand").split("[|]");
						for(int i = 0; i < cmds.length; i++) {
							cmds[i] = cmds[i].replaceAll("%player%", e.getPlayer().getName());
							cmds[i] = cmds[i].replaceAll("%message%", e.getMessage());
							String cmd = cmds[i];
							Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd));
						}
	    			}
	    			
			    	float toxicityLevel = getToxicity(e.getMessage());
			    	if (config.getBoolean("debug")) {
			    		getLogger().info(e.getPlayer().getName() + " : " + e.getMessage() + " - " + toxicityLevel);
			    	}
			    	if (toxicityLevel >= config.getDouble("minimumTrigger")) {
			    		getLogger().info(ChatColor.RED + "[PerspectiveAPI] " + ChatColor.RESET + e.getPlayer().getName() + " : " + e.getMessage());
			    		getLogger().info("    " + ChatColor.RED + "Toxicity Rating: " + toxicityLevel);
						String[] cmds = config.getString("command").split("[|]");
						for(int i = 0; i < cmds.length; i++) {
							cmds[i] = cmds[i].replaceAll("%player%", e.getPlayer().getName());
							cmds[i] = cmds[i].replaceAll("%message%", e.getMessage());
							cmds[i] = cmds[i].replaceAll("%rating%", (toxicityLevel * 100) + "%");
							String cmd = cmds[i];
							Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd));
						}
			    	}
	    		}
	    	}).start();
    	}
    }
    
    private float getToxicity(String query) {
    	try {
	       	HttpURLConnection httpcon = (HttpURLConnection) ((new URL("https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=" + apiKey).openConnection()));
	    	httpcon.setDoOutput(true);
	    	httpcon.setRequestProperty("Content-Type", "application/json");
	    	httpcon.setRequestProperty("Accept", "application/json");
	    	httpcon.setRequestMethod("POST");
	    	httpcon.connect();
	    	
	    	JsonParser parser = new JsonParser();
	    	JsonObject jObj = parser.parse(
	    			"{comment: {text: \"" + query + "\"},"
	    			+ "languages: [\"en\"], "
	    			+ "requestedAttributes: {TOXICITY:{}} }").getAsJsonObject();
	
	    	//Send Request
	    	OutputStream os = httpcon.getOutputStream();
	    	PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
	    	pw.write(jObj.toString());
	    	pw.close();
	    	os.close();
	    	
	    	//Read response
	    	InputStream is = httpcon.getInputStream();
	    	BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    	String line = null;
	    	StringBuffer sb = new StringBuffer();
	    	while ((line = reader.readLine()) != null) {
	    	    sb.append(line);
	    	}
	    	is.close();
	    	
	    	//Get specified data needed.
	    	JsonObject jResponse = parser.parse(sb.toString()).getAsJsonObject();
	    	return jResponse.get("attributeScores").getAsJsonObject().get("TOXICITY").getAsJsonObject().get("summaryScore").getAsJsonObject().get("value").getAsFloat();
    	} catch (Exception e) {
    		getLogger().warning(e.toString());
    		return 0;
    	}
    }
}
