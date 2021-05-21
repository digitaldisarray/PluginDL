package xyz.disarray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Util {
	public static Document get(String url) {
		// The JSON request sent to cloudsolverr
		String json = "{" + "    \"cmd\": \"request.get\"," + "    \"url\": \"" + url + "\","
				+ "    \"userAgent\": \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36\","
				+ "    \"maxTimeout\": 60000" + "}";

		// The url of cloudsolverr
		String baseUrl = "http://127.0.0.1:8191/v1";

		// Create the connection and send the data to cloudsolverr
		HttpURLConnection client;
		try {
			client = (HttpURLConnection) new URL(baseUrl).openConnection();
			client.setRequestMethod("POST");
			client.addRequestProperty("Content-Type", "application/json");

			// Send the POST request body to the server (json format)
			client.setDoOutput(true);
			OutputStream outStream = client.getOutputStream();
			OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "UTF-8");
			outStreamWriter.write(json);
			outStreamWriter.flush();
			outStreamWriter.close();
			outStream.close();

			// Read the response (json format) from the server
			String response = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
					.lines().collect(Collectors.joining("\n"));

			// Parse the json response from the server
			JSONParser parser = new JSONParser();
			JSONObject responseJson = null;
			try {
				responseJson = (JSONObject) parser.parse(response);
			} catch (ParseException e) {
				e.printStackTrace();
			}

			// Extract the html code from the response
			JSONObject ree = (JSONObject) responseJson.get("solution");
			String responseBody = ree.get("response").toString();

			return Jsoup.parse(responseBody);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
