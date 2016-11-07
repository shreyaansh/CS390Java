/**
 * Created by Shreyaansh on 06-Nov-16.
 */
// God Statement set CLASSPATH=C:\apache-tomcat-7.0.72\lib\servlet-api.jar;C:\Users\Shreyaansh\Downloads\jsoup-1.10.1.jar;.

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import util.HTMLFilter;
import java.util.HashMap;
import java.sql.*;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class myapp extends HttpServlet {

    private static Connection connection = null;

    public void doGet(HttpServletRequest request, HttpServletResponse
            response)
            throws IOException, ServletException
    {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Search Engine</title>");
        out.println("<meta charset=\"utf-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.println("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\">");
        out.println("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js\"></script>");
        out.println("<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script>");
        out.println("<style>.a1 { text-decoration: none; color:black; } a1:hover { color:black; cursor: pointer; } </style>");
        out.println("</head>");
        out.println("<body background=\"http://4kwallpaper.org/wp-content/uploads/2016/10/modern-wallpaper6.jpg\" style=\"background-attachment: fixed; background-repeat: no-repeat;\">");
        out.println("<br><center><a href=\"http://localhost:8080/myapp/myapp\" style=\"text-decoration: none; color: black;\"><div style=\"font-family: Impact, Charcoal, sans-serif; font-size: 60px; \"> BoilerSearch </div></a></center>");       //out.println("<h3>Enter Query</h3>");
        //out.println("Parameters in this request:<br>");
        out.println("<div style=\"text-align: center; padding-bottom: 50px;\">");
        String query = request.getParameter("query");
        //out.println("<P>");
        out.println("<br><br>");
        out.println("<div class=\"form-group\">");
        out.print("<form action=\"");
        out.print("myapp\" ");
        out.println("method=POST>");
        out.println("<div class=\"col-xs-4 col-md-offset-4\">");
        out.println("<label for=\"quer\">Query</label>");
        out.println("<input type=text size=20 class=\"form-control\" name=query id=\"quer\" style=\"padding:15px;\">");
        out.println("<button type=\"submit\" class=\"btn btn-default\" style=\"margin-top: 15px;\">Search</button>");
        out.println("</div>");
        out.println("<br>");
        out.println("</form>");
        out.println("</div>");
        //out.println("</P>");
        out.println("<br><br>");
        out.println("</div>");
        //out.println("</center>");
        out.println("<center><div style=\"background-color: rgba(0, 0, 0, .9); font-family: Century Gothic, sans-serif;\">");
        if (query != null) {
            // Connect to database
            connectDB(query, out);
        }
        out.println("</div></center>");
        out.println("</body>");
        out.println("</html>");
    }
    public void doPost(HttpServletRequest request, HttpServletResponse res)
            throws IOException, ServletException
    {
        doGet(request, res);
    }

    public void connectDB(String query, PrintWriter out) {
        try {
            if (connection == null) {
                Class.forName("com.mysql.jdbc.Driver");
                String url = "jdbc:mysql://localhost:3306/crawler";
                String username = "root";
                String password = "Chileesasi69";

                connection = DriverManager.getConnection(url, username, password);
            }
            processQuery(query, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processQuery(String query, PrintWriter out) throws SQLException, IOException {

        // Make query so that SQL injection doesn't work
        query = query.replace("'", "''");
        query = query.replace("\"", "''");

        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

        String words[] = query.toLowerCase().split(" ");
        int wordLen = words.length;

        for (String word: words) {
            if (word.length() > 0) {
                Statement stmt = connection.createStatement();
                ResultSet result = stmt.executeQuery("select distinct * from words where word like '%" + word + "%'");

                if (result.next()) {
                    //out.println("<p>It got in!</p>");
                    int id = Integer.parseInt(result.getString("urlid"));
                    getURL(id, out);
                    while (result.next()) {
                        id = Integer.parseInt(result.getString("urlid"));
                        if (map.containsKey(id)) map.put(id, map.get(id) + 1);
                        else map.put(id, 1);
                    }
                }
            }
        }

        // Gets the result with all the query terms in it first,
        // then the pages with lesser number of terms in them as long as some
        // or the other term is in that page
        out.println("<div style=\"padding: 40px;\">");
        for (int i = wordLen; i >= 1; i--) {
            for (Integer id: map.keySet()) {
                if (map.containsKey(id)) {
                    if (map.get(id) == i) {
                        getURL(id, out);
                    }
                }
            }
        }
        out.println("</div>");
    }

    public void getURL(int urlid, PrintWriter out) throws IOException {
        try {
            //out.println("<p>"+ urlid + "</p>");
            //out.println("<p>It is innnn!</p>");
            Statement stat = connection.createStatement();
            ResultSet result = stat.executeQuery("SELECT * FROM urls WHERE urlid = '" + urlid + "'");
            //out.println("<p>After query!</p>");
            if (result.next()) {
                Document doc = Jsoup.connect(result.getString("url")).get();

                String url = result.getString("url");
                String imgurl = result.getString("imgUrl");
                if (imgurl.contains(".svg")) imgurl = "https://www.cs.purdue.edu/images/brand.svg";
                out.println("<font size=\"4\"><a href=\"" + url + "\">" + "<img src=\"" + imgurl + "\" style=\"float:left; height:50px; width:50px; padding:10px\">" + doc.title() + "</a></font>");
                out.println("<br>");
                out.println("<font color=\"green\">" + url + "</font>");
                out.println("<br>");
                out.println(result.getString("description"));
                out.println("<br>");
                out.println("<br>");
                out.println("<br>");
            }
        } catch(Exception e) {}
    }
}