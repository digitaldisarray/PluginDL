package xyz.disarray;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PluginDL extends Thread {

	private boolean running = true;
	List<Plugin> plugins = new ArrayList<>();

	public PluginDL() {

	}

	public void run() {

		// Read the json and init plugin objects that we have previously downloaded
		JSONParser jsonParser = new JSONParser();
		try (FileReader reader = new FileReader(System.getProperty("user.dir") + "/downloads/plugins.json")) {
			Object obj = jsonParser.parse(reader);
			JSONArray pluginList = (JSONArray) obj;

			// Iterate over employee array
			for(Object o : pluginList) {
				plugins.add(parsePluginObj((JSONObject) o));
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// Get the first page
		Document firstPage = Util.get("https://dev.bukkit.org/bukkit-plugins");

		// Get the number of pages
		int pages = 1;
		// Get <a> that has href that contains "?page="
		for (Element e : firstPage.select("a[href*=?page=]")) {
			// Find the first page # jump
			if (Integer.parseInt(e.attr("href").split("=")[1]) - pages > 1) {
				pages = Integer.parseInt(e.attr("href").split("=")[1]);
				break;
			} else {
				pages = Integer.parseInt(e.attr("href").split("=")[1]);
			}
		}

		System.out.println(pages + " page(s) detected");

		// DEBUG PURPOSES:
		pages = 1;

		// Loop through each page & create plugin objects out of every link
		for (int i = 1; i <= pages; i++) {
			Document doc = Util.get("https://dev.bukkit.org/bukkit-plugins?page=" + i);

			Elements pluginElements = doc.getElementsByClass("project-list-item");
			for (Element e : pluginElements) {
				for (Element link : e.select("a[href]")) {
					if (link.attr("href").startsWith("https://dev.bukkit.org/projects/")) {
						plugins.add(new Plugin(link.attr("href")));
						System.out.println("Added new plugin");
						break;
					}
				}
			}
		}

		// Start each plugin thread
		plugins.get(0).start();

		/*
		 * for(Plugin p : plugins) { p.start(); }
		 */

		// Check if all downloads are finished
		boolean allFinished = false;
		while (!allFinished) {
			
			// debug
			if(plugins.get(0).isDone()) {
				allFinished = true;
			}
			
			
			//boolean foundUnfinished = false;
			//for (Plugin p : plugins) {
			//	if (!p.isDone()) {
			//		foundUnfinished = true;
			//		break;
			//	}
			//}

			//if (!foundUnfinished) {
			//	allFinished = true;
			//}
		}
		
		System.out.println("All plugins completed");
		
		// All plugins are done downloading, save the json entries to a file
		JSONArray pluginEntries = new JSONArray();
		for (Plugin p : plugins) {
			pluginEntries.add(p.toJson());
		}

		try (FileWriter file = new FileWriter(System.getProperty("user.dir") + "/downloads/plugins.json")) {
			file.write(pluginEntries.toJSONString().replace("\\/", "/"));
			file.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private Plugin parsePluginObj(JSONObject plugin) {
		String url = (String) plugin.get("url");
		String name = (String) plugin.get("name");
		int id = (int) plugin.get(name);
		String created = (String) plugin.get("created");
		String updated = (String) plugin.get("updated");
		String totalDownloads = (String) plugin.get("totalDownloads");
		String categories = (String) plugin.get("categories");
		return new Plugin(url, name, id, created, updated, totalDownloads, categories); 
	}

	
	public void exit() {
		running = false;
	}

	public static void main(String[] args) {
		PluginDL pluginDL = new PluginDL();
		pluginDL.start();

		// System.out.print("Press enter to close...");
		// Scanner in = new Scanner(System.in);
		// in.nextLine();
		// in.close();
		// pluginDL.exit();

	}
}
