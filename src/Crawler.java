import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.sql.*;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler
{
	Connection connection;
	int urlID;
	public Properties props;

	Crawler() {
		urlID = 0;
	}

	public void readProperties() throws IOException {
      		props = new Properties();
      		FileInputStream in = new FileInputStream("database.properties");
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
		}
		catch (Exception e) {
		}
			
		// Create the table
        	stat.executeUpdate("CREATE TABLE URLS (urlid INT, url VARCHAR(512), description VARCHAR(200))");
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

	public void insertURLInDB( String url) throws SQLException, IOException {
		Statement stat = connection.createStatement();
		String query = "INSERT INTO urls VALUES ('"+urlID+"','"+url+"','')";
		System.out.println("Executing " + query);
		stat.executeUpdate( query );
		urlID++;
	}

   	public void fetchURL(String url) {
		try {
			Document doc = Jsoup.connect(url).get();

			Elements links = doc.select("a[href]");

			for (Element link: links) {
				String absUrl = link.attr("abs:href").toString();

				if (!urlInDB(absUrl)) {
					insertURLInDB(absUrl);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

   	public static void main(String[] args)
   	{
		Crawler crawler = new Crawler();

		try {
			crawler.readProperties();
			String root = crawler.props.getProperty("crawler.root");
			crawler.createDB();
			crawler.fetchURL(root);
		} catch( Exception e) {
         		e.printStackTrace();
		}
	}
}

