package xyz.disarray;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PluginDL extends Thread {

	private boolean running = true;
	List<Plugin> plugins = new ArrayList<>();

	public PluginDL() {

	}

	public void run() {
		// Get the first page
		Document firstPage = Util.get("https://dev.bukkit.org/bukkit-plugins");

		// Get the number of pages
		int pages = 1;
		// Get <a> that has href that contains "?page="
		for(Element e : firstPage.select("a[href*=?page=]")) {
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
		
		// Loop through each page getting all of the links & create plugin objects out each one
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
