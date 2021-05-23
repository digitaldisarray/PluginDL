package xyz.disarray;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class PluginFile {
	private String name;
	private String dlPageLink;
	private String fileSize;
	private String uploadDate;
	private String gameVersions;
	private String fileName;
	private int id;

	public PluginFile(String name, String fileSize, String dlPageLink, String uploadDate, String gameVersions,
			String fileName) {
		this.name = name;
		this.fileSize = fileSize;
		this.dlPageLink = dlPageLink;
		this.uploadDate = uploadDate;
		this.gameVersions = gameVersions;
		this.fileName = fileName;
		this.id = Integer.parseInt(dlPageLink.substring(dlPageLink.lastIndexOf("/") + 1));
	}

	public void download() {
		System.out.println("DL to: " + "./" + "/" + fileName);
		System.out.println("Dalink: " + dlPageLink);
		// Ex: https://dev.bukkit.org/projects/worldedit/files/831692
		// Example
		// https://edge.forgecdn.net/files/831/692/worldedit-bukkit-6.0-beta-01.jar
		String dlNumbers = String.valueOf(id);
		String directLink;
		if (dlNumbers.length() == 6) {
			directLink = "https://edge.forgecdn.net/files/" + dlNumbers.substring(0, 3) + "/" + dlNumbers.substring(3)
					+ "/" + fileName;
		} else {
			directLink = "https://edge.forgecdn.net/files/" + dlNumbers.substring(0, 4) + "/" + dlNumbers.substring(4)
					+ "/" + fileName;
		}
		System.out.println("Direct Link: " + directLink);
		try {
			InputStream in = new URL(directLink).openStream();

			// Make sure path to file exists
			Path target = Paths.get(System.getProperty("user.dir") + "/downloads/" + dlPageLink.split("/")[4] + "/"
					+ dlNumbers + "/" + fileName);
			if (!Files.exists(target)) {
				Files.createDirectories(target);
			}

			// Put the input stream into the file
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getName() {
		return name;
	}

	public String getFileSize() {
		return fileSize;
	}

	public String getDownloadPageLink() {
		return dlPageLink;
	}

	public String getUploaded() {
		return uploadDate;
	}

	public String getGameVersions() {
		return gameVersions;
	}

	public String getFileName() {
		return fileName;
	}

	public int getID() {
		return id;
	}

}
