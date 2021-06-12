package xyz.disarray;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Plugin extends Thread {

	// Info & statistics about the plugin
	private String url;
	private String name;
	private int id;
	private String created;
	private String updated;
	private String totalDownloads;
	private String categories;
	private String iconFileName;
	private String description;

	// Json object that represents the plugin as a whole
	private JSONObject plugin;

	// List of all the releases that this plugin has
	private List<PluginFile> downloaded;

	// Is the plugin done downloading all the releases
	private boolean done = false;

	public Plugin(String url, String description) {
		this.url = url;
		this.description = description;
		downloaded = new ArrayList<>();
	}

	public Plugin(String url, String name, int id, String created, String updated, String totalDownloads,
			String categories, String iconFileName, String description) {
		this.url = url;
		this.name = name;
		this.id = id;
		this.created = created;
		this.updated = updated;
		this.totalDownloads = totalDownloads;
		this.categories = categories;
		this.iconFileName = iconFileName;
		this.description = description;
		
		downloaded = new ArrayList<>();

		// Load plugin files that we already downloaded from json
		JSONParser jsonParser = new JSONParser();
		try (FileReader reader = new FileReader(
				System.getProperty("user.dir") + "/downloads/" + url.split("/")[4] + "/files.json")) {
			Object obj = jsonParser.parse(reader);
			JSONArray pluginList = (JSONArray) obj;

			for (Object o : pluginList) {
				downloaded.add(parsePluginFileObj((JSONObject) o));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}

	private PluginFile parsePluginFileObj(JSONObject o) {
		String name = (String) o.get("name");
		String fileSize = (String) o.get("fileSize");
		String dlPageLink = (String) o.get("link");
		String uploadDate = (String) o.get("uploadDate");
		String gameVersions = (String) o.get("versions");
		String fileName = (String) o.get("fileName");
		return new PluginFile(name, fileSize, dlPageLink, uploadDate, gameVersions, fileName);
	}

	public void start() {

		Document d;

		// Even if we already have the info, we want to get latest updated date and
		// download count

		// Fetch data
		d = Util.get(url);
		// NAME
		this.name = d.select("span[class=overflow-tip]").first().text();

		// INFO/PROPERTIES
		List<String> properties = new ArrayList<>();
		for (Element e : d.select("div[class=info-data]"))
			properties.add(e.text());

		this.id = Integer.parseInt(properties.get(0));
		this.created = properties.get(1);
		this.updated = properties.get(2);
		this.totalDownloads = properties.get(3);

		// CATEGORIES
		categories = "";
		for (Element e : d.select("a[class='e-avatar32 tip']"))
			categories += e.attr("title") + ",";

		// ICON FILE
		if (iconFileName == null || iconFileName.equals("none") || iconFileName.equals("null")) {
			String iconUrl = d.select("a[class='e-avatar64 lightbox']").first().attr("href");
			if (iconUrl == null) {
				iconFileName = "none";
			} else {
				iconFileName = iconUrl.split("/")[6];
			}
			// download the icon file
			try {
				InputStream in = new URL(iconUrl).openStream();

				// Make sure path to file exists
				Path target = Paths
						.get(System.getProperty("user.dir") + "/downloads/" + url.split("/")[4] + "/" + iconUrl.split("/")[6]);
				if (!Files.exists(target)) {
					Files.createDirectories(target);
				}

				// Put the input stream into the file
				Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Get plugin files/versions etc
		d = Util.get(url + "/files");
		// Dl file page while next is available
		Element next = d.selectFirst("a[rel=next]");
		if (next == null) {
			dlPluginsFromPage(d);
		} else {
			while (next != null) {
				dlPluginsFromPage(d);
				d = Util.get("https://dev.bukkit.org" + d.selectFirst("a[rel=next]").attr("href"));
				next = d.selectFirst("a[rel=next]");
			}
		}

		// Create the json that stores the list of jar files
		JSONArray pluginFiles = new JSONArray();
		for (PluginFile f : downloaded) {
			JSONObject file = new JSONObject();

			// Add all the properties of the plugin release file to an object
			file.put("name", f.getName());
			file.put("fileName", f.getFileName());
			file.put("id", f.getID());
			file.put("size", f.getFileSize());
			file.put("link", f.getDownloadPageLink());
			file.put("versions", f.getGameVersions());
			file.put("uploadDate", f.getUploaded());

			// Add the object to the array of files
			pluginFiles.add(file);
		}

		// Write the json file to the plugins directory
		try (FileWriter file = new FileWriter(
				System.getProperty("user.dir") + "/downloads/" + url.split("/")[4] + "/files.json")) {
			file.write(pluginFiles.toJSONString().replace("\\/", "/"));
			file.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Create json object that represents the plugin as a whole
		plugin = new JSONObject();
		plugin.put("name", name);
		plugin.put("id", id);
		plugin.put("url", url);
		plugin.put("categories", categories);
		plugin.put("created", created);
		plugin.put("updated", updated);
		plugin.put("downloads", Integer.parseInt(totalDownloads.replaceAll(",", "")));
		plugin.put("iconFileName", iconFileName);
		plugin.put("description", description);
		
		done = true;
	}

	private void dlPluginsFromPage(Document d) {
		for (Element entry : d.select("tr[class=project-file-list-item]")) {
			Element downloadCell = entry.select("a[class='overflow-tip twitch-link']").first();
			String name = downloadCell.text();
			String dlPageLink = url.substring(0, url.indexOf(".org/") + 4) + downloadCell.attr("href");

			// If we already have this plugin downloaded, skip it
			boolean alreadyDownloaded = false;
			for (PluginFile p : downloaded) {
				if (dlPageLink.equals(p.getDownloadPageLink())) {
					alreadyDownloaded = true;
					break;
				}
			}
			if (alreadyDownloaded) {
				System.out.println("Already downloaded " + name + "! Skipping...");
				continue;
			}

			String fileSize = entry.selectFirst("td[class=project-file-size]").text();
			String uploadDate = entry.selectFirst("abbr").text();
			String gameVersions = entry.selectFirst("span[class=version-label]").text() + "";

			// Is there more than one game version supported
			Element additionalVersions = entry.selectFirst("span[class='additional-versions tip']");
			if (additionalVersions != null) {
				gameVersions += " " + additionalVersions.attr("title").substring(5).replaceAll("</div><div>", " ")
						.replaceAll("</div>", "");
			}
			String fileName = Util.get(dlPageLink).selectFirst("div[class^='info-data overflow-tip']").text();

			PluginFile pf = new PluginFile(name, fileSize, dlPageLink, uploadDate, gameVersions, fileName);
			pf.download();
			downloaded.add(pf);
		}
		System.out.println("Completed downloading a page for plugin: " + name);
	}

	public boolean isDone() {
		return done;
	}

	public JSONObject toJson() {
		return plugin;
	}
	
	public String getPluginURL() {
		return url;
	}

}
