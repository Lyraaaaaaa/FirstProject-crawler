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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {
        Set<String> processedLinks = new HashSet<>();
        List<String> linkPool = new ArrayList<>();
        linkPool.add("https://sina.cn/");

        while (!linkPool.isEmpty()) {
            String link = linkPool.remove(linkPool.size() - 1);
            if (processedLinks.contains(link) || !isInterestingLink(link)) {
                continue;
            }
            Document doc = httpGetAndParseHtml(link);
            storeIntoDatabaseIfItIsNewsPage(doc);
            selectlinks(doc, linkPool);
            processedLinks.add(link);
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


