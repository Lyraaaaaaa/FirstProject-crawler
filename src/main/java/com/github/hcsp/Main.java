package com.github.hcsp;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Main {

   // private static final String UserName = "root";
   // private static final String Password = "82468aliran";
    private static List<String>  loadUrlsFromDatabase(Connection connection,String sql) throws SQLException {
        List<String> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
        }
        return results;
    }
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:tcp:/Users/aliran/FirstProject-crawler/news", "root", "82468aliran");
        while (true){
            List<String> linkPool = loadUrlsFromDatabase(connection, "select link from LINKS_TO_BE_PROCESSED");
            if (linkPool.isEmpty()) {
                break;
            }
            String link = linkPool.remove(linkPool.size() - 1);
            insertLinkIntoDatabase(connection, "delete from LINKS_TO_BE_PROCESSED where link = ? ", link);
            if (isLinkProcessed(connection,link)) {
                continue;
            }
            if( isInterestingLink(link)){
                Document doc = httpGetAndParseHtml(link);
                parseUrlFromPageAndStoreIntoDatabase(doc, connection);
                storeIntoDatabaseIfItIsNewsPage(doc);
                insertLinkIntoDatabase(connection, "insert into LINKS_ALREADY_PROCESSED (link)values (?)", link);
            }
        }
    }
    private static void parseUrlFromPageAndStoreIntoDatabase(Document doc, Connection connection) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            insertLinkIntoDatabase(connection, "insert into LINKS_TO_BE_PROCESSED (link)values (?)", href);
        }
    }
    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select link from LINKS_ALREADY_PROCESSED where link =?")) {
            statement.setString(1, link);
            try(ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void insertLinkIntoDatabase(Connection connection, String sql, String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }
    private static Document httpGetAndParseHtml (String link) throws IOException{
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            if (link.startsWith("//")) {
                link = "https:" + link;
            }
            HttpGet httpGet = new HttpGet(link);
            httpGet.addHeader("User-Agent", "Macintosh;Intel Mac os X 10_14_6) AppleWebKit/537.36 (KHTML,like Gecko) Chrome/76.0.3809.100 Safari/537.36");
            try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                System.out.println(response.getStatusLine());
                HttpEntity entity1 = response.getEntity();
                String html = EntityUtils.toString(entity1);
                return Jsoup.parse(html);
            }
        }
    }
            private static void selectlinks(Document doc, List<String> linkPool) {
                doc.select("a").stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);
            }
            private static void storeIntoDatabaseIfItIsNewsPage (Document doc){
                ArrayList<Element> articleTags = doc.select("article");
                if (!articleTags.isEmpty()) {
                    for (Element articleTag : articleTags) {
                        String title = articleTag.child(0).text();
                        System.out.println(title);
                    }
                }
            }
        private static boolean isInterestingLink (String link){
            return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link);
        }
    private static boolean isNotLoginPage(String link){
        return !link.contains("passport.sina.cn");
    }

    private static boolean isNewsPage (String link){
            return link.contains("news.sina.cn");
        }
        private static boolean isIndexPage (String link){
            return "https://sina.cn".equals(link);
        }
    }


