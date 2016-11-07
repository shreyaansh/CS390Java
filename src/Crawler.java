import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.sql.*;
import java.util.*;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler {

	static Queue queue = new LinkedList<String>();

	Connection connection;
	int urlID;
	public Properties props;

	Crawler() {
		urlID = 0;
	}

	public void readProperties() throws IOException {
      		props = new Properties();
      		FileInputStream in = new FileInputStream("src/database.properties");
      		props.load(in);
      		in.close();
	}

	public void openConnection() throws SQLException, IOException
	{
		String drivers = props.getProperty("jdbc.drivers");
      		if (drivers != null) System.setProperty("jdbc.drivers", drivers);

      		String url = props.getProperty("jdbc.url");
      		String username = props.getProperty("jdbc.username");
      		String password = props.getProperty("jdbc.password");

		connection = DriverManager.getConnection( url, username, password);
   	}

	public void createDB() throws SQLException, IOException {
		openConnection();

         	Statement stat = connection.createStatement();
		
		// Delete the table first if any
		try {
			stat.executeUpdate("DROP TABLE URLS");
			stat.executeUpdate("DROP TABLE WORDS");
		} catch (Exception e) {
		}
			
		// Create the table
        	stat.executeUpdate("CREATE TABLE URLS (urlid INT, url VARCHAR(512), imgUrl VARCHAR(250), description VARCHAR(200))");
		// Create word table
			stat.executeUpdate("CREATE TABLE WORDS (word VARCHAR(100), urlid INT)");
	}

	public boolean urlInDB(String urlFound) throws SQLException, IOException {
        Statement stat = connection.createStatement();
		ResultSet result = stat.executeQuery( "SELECT * FROM urls WHERE url LIKE '"+urlFound+"'");

		if (result.next()) {
			System.out.println("URL "+urlFound+" already in DB");
			return true;
		}
	       System.out.println("URL "+urlFound+" not yet in DB");
		return false;
	}

	public void insertURLInDB(String url) throws SQLException, IOException {
		// Check if url returns an HTML file or not
		String imgUrl = "";
		try {
			Document doc = Jsoup.connect(url).get();
			Elements el = doc.getElementsByTag("img");
			for (Element e: el) {
				imgUrl = e.absUrl("src");
				break;
			}
		} catch (UnsupportedMimeTypeException m) {
			return;
		}

		Statement stat = connection.createStatement();
		String query = "INSERT INTO urls VALUES ('"+urlID+"','"+url+"','"+imgUrl+"','')";
		System.out.println("Executing " + query);
		stat.executeUpdate( query );
		fetchDescription(url, urlID);
		urlID++;
	}

	public void insertDescInDB(String desc, int urlID) throws SQLException, IOException {
		Statement stat = connection.createStatement();
		String query = "UPDATE urls SET description=\"" + desc + "\" WHERE urlid="+ urlID;
		System.out.println("Executing " + query);
		stat.executeUpdate(query);
	}

	public String getMetaContent(Document doc, String attribute) {
		Elements elements = doc.select("meta[name="+attribute+"]");
		for (Element element: elements) {
			String s = element.attr("content");
			if (s != null) return s;
		}
		elements = doc.select("meta[property=" + attribute + "]");
		for (Element element : elements) {
			final String s = element.attr("content");
			if (s != null) return s;
		}
		return null;
	}

	public void insertWordsInDB(String Words[], int urlid) throws SQLException, IOException {
		for (String word: Words) {
			Statement stat = connection.createStatement();
			String query = "INSERT INTO words VALUES(\"" + word + "\", " + urlid + ")";
			//System.out.println("Executing " + query);
			stat.executeUpdate(query);
		}
	}

	public void fetchDescription (String url, int urlID) throws SQLException, IOException {
		Document doc = Jsoup.connect(url).get();

		// Get the description and store it
		String title = doc.title();

		String description = getMetaContent(doc, "description");
		if (description == null) description = getMetaContent(doc, "og:description");

		if (description == null) description = "";

		String text = description + " " + title;

		text = text.toLowerCase();
		String words[] = text.split("\\P{Alpha}+");

		String desc = "";
		String tempDesc = "";
		for (String word: words) {
			tempDesc = tempDesc + word + " ";
			if (tempDesc.length() < 200) desc = tempDesc;
			else break;
		}

		System.out.println("Words: " + desc);

		desc.replace("\"", "");
		desc.replace("'", "");

		insertDescInDB(desc, urlID);

		text = doc.body().text();
		text = text.toLowerCase();
		String Words[] = text.split("\\P{Alpha}+");
		insertWordsInDB(Words, urlID);
	}

	public void crawlBFS(String root) throws SQLException, IOException {
		queue.add(root);
		insertURLInDB(root);
		//int counter = 0;

		while (!queue.isEmpty()) {
			String u = (String) queue.remove();
			System.out.println("Removed String is: " + u);
			//System.out.println("Current count is: " + counter);
			if (!fetchURL(u)) return;
			//counter++;
		}
	}

   	public Boolean fetchURL(String url) {
		try {
			//System.out.println("Before opening");
			Document doc = Jsoup.connect(url).timeout(1000).get();

			// Selecting the links on the page
			Elements links = doc.select("a[href]");

			// Checking and storing the links in the database
			for (Element link: links) {
				String absUrl = link.attr("abs:href").toString();

				// How to check if URL is not http or https
				if (!absUrl.substring(0, 4).equals("http") || !absUrl.substring(0, 5).equals("https")) continue;

				// If URL doesn't exist in DB then add it
				if (!urlInDB(absUrl)) {
					if (urlID >= 10000) return false;
					queue.add(absUrl);
					insertURLInDB(absUrl);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

   	public static void main(String[] args) {
		Crawler crawler = new Crawler();

		try {
			crawler.readProperties();
			String root = crawler.props.getProperty("crawler.root");
			crawler.createDB();
			//crawler.fetchURL(root);
			crawler.crawlBFS(root);
		} catch( Exception e) {
         		e.printStackTrace();
		}
	}
}

