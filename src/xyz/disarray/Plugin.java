package xyz.disarray;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Plugin extends Thread {
	private String url;
	private String name;
	private int id;
	private String created;
	private String updated;
	private String totalDownloads;
	private List<String> categories;

	private List<PluginFile> downloaded;

	public Plugin(String url) {
		this.url = url;
		downloaded = new ArrayList<>();
	}

	public void start() {
		// Fetch data
		Document d = Util.get(url);
		
		// NAME
		this.name = d.select("span[class=overflow-tip]").first().text();
		
		// INFO/PROPERTIES
		List<String> properties = new ArrayList<>();
		for(Element e : d.select("div[class=info-data]")) 
			properties.add(e.text());
		
		this.id = Integer.parseInt(properties.get(0));
		this.created = properties.get(1);
		this.updated = properties.get(2);
		this.totalDownloads = properties.get(3);
		
		// CATEGORIES
		categories = new ArrayList<>();
		for(Element e : d.select("a[class='e-avatar32 tip']"))
			categories.add(e.attr("title"));
		
		// Get plugin files/versions etc
		d = Util.get(url + "/files");
		// Dl file page while next is available
		Element next = d.selectFirst("a[rel=next]");
		if(next == null) {
			dlPluginsFromPage(d);
		} else {
			while(next != null) {
				dlPluginsFromPage(d);
				d = Util.get("https://dev.bukkit.org" + d.selectFirst("a[rel=next]").attr("href"));
				next = d.selectFirst("a[rel=next]");
			}
		}
		
		// Create the json that stores the list of jar files
		JSONArray pluginFiles = new JSONArray();
		for(PluginFile f : downloaded) {
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
		String current = System.getProperty("user.dir");
		System.out.println(pluginFiles.toJSONString());
		try (FileWriter file = new FileWriter(System.getProperty("user.dir") + "/downloads/" + url.split("/")[4] + "/plugins.json")) {
            file.write(pluginFiles.toJSONString().replace("\\/", "/")); 
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		// Create json object
		JSONObject plugin = new JSONObject();
		//plugin.put("url", url);
		// ...
	}

	private void dlPluginsFromPage(Document d) {
		for (Element entry : d.select("tr[class=project-file-list-item]")) {
			Element downloadCell = entry.select("a[class='overflow-tip twitch-link']").first();
			String name = downloadCell.text();
			String dlPageLink = url.substring(0, url.indexOf(".org/") + 4) + downloadCell.attr("href");
			String fileSize = entry.select("td[class=project-file-size]").first().text();
			String uploadDate = entry.select("abbr").first().text();
			String gameVersions = entry.select("span[class=version-label]").first().text() + "";

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
		System.out.println("Completed downloading plugin: " + name);
	}

}
