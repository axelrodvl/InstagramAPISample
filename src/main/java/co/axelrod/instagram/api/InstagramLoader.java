package co.axelrod.instagram.api;

import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class InstagramLoader {
    private final String USER_AGENT = "Mozilla/5.0";
    private final String ACCESS_TOKEN = "sampleToken";
    private String userId;
    private int photoCount = 0;
    
    public InstagramLoader(String userName) {
        try {
            userId = getUserId(userName);
        }
        catch (Exception ex) {
        }
    }

    private String getUserId(String userName) throws Exception {
        String requestURL = "https://api.instagram.com/v1/users/search?q=" + userName + "&access_token=" + ACCESS_TOKEN;

        URL url = new URL(requestURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = connection.getResponseCode();
        
        if (responseCode != 200) 
            throw new Exception();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        JSONParser parser = new JSONParser();
        
        JSONObject responseJSON = (JSONObject) parser.parse(response.toString());
        
        JSONArray dataJSON = (JSONArray) responseJSON.get("data");
        
        String userId = "not found";
        
        for (Object obj : dataJSON) {
            JSONObject userInfoJSON = (JSONObject) obj;
            
            if (userInfoJSON.get("username").toString().equals(userName))
                userId = userInfoJSON.get("id").toString();
        }
        
        return userId;
    }
    
    private Integer getUserPhotoCount() throws Exception {
        String requestURL = "https://api.instagram.com/v1/users/" + userId + "/?access_token=" + ACCESS_TOKEN;

        URL url = new URL(requestURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = connection.getResponseCode();
        
        if (responseCode != 200) 
            throw new Exception();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        JSONParser parser = new JSONParser();
        
        JSONObject responseJSON = (JSONObject) parser.parse(response.toString());
        
        responseJSON = (JSONObject) responseJSON.get("data");
        responseJSON = (JSONObject) responseJSON.get("counts");
        
        return Integer.parseInt(responseJSON.get("media").toString());
    }
    
    private JSONArray getPhotoRequestData(String userId, String MAX_ID) throws Exception {
        String requestURL;
        if (MAX_ID == null) 
            requestURL = "https://api.instagram.com/v1/users/" + userId + "/media/recent/?access_token=" + ACCESS_TOKEN + "&count=20";
        else
            requestURL = "https://api.instagram.com/v1/users/" + userId + "/media/recent/?access_token=" + ACCESS_TOKEN + "&max_id=" + MAX_ID;
        
        System.out.println(requestURL);
        
        URL url = new URL(requestURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = connection.getResponseCode();
        
        if (responseCode != 200) 
            throw new Exception();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        JSONParser parser = new JSONParser();
        JSONObject responseJSON = (JSONObject) parser.parse(response.toString());
        JSONArray dataJSON = (JSONArray) responseJSON.get("data");
        
        return dataJSON;
    }
    
    private void downloadPhoto(JSONArray dataJSON) throws Exception {
        for (Object obj : dataJSON) {
            JSONObject photoJSON = (JSONObject) obj;
            
            photoJSON = (JSONObject) photoJSON.get("images");
            photoJSON = (JSONObject) photoJSON.get("standard_resolution");
            
            System.out.println(photoJSON.get("url").toString());
            
            URL website = new URL(photoJSON.get("url").toString());
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            String fileName = "photo/" + Integer.toString(photoCount++) + ".jpg";
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }
    
    public void downloadAll() throws Exception {
        JSONArray dataJSON = getPhotoRequestData(userId, null);
        
        downloadPhoto(dataJSON);
        
        String lastId = null;
        
        if (getUserPhotoCount() > 20) {
            JSONObject photoJSON = (JSONObject) dataJSON.get(dataJSON.size() - 1);
            lastId = photoJSON.get("id").toString();
            System.out.println(lastId);
        }
        
        for (int i = 0; i < (getUserPhotoCount() / 20) + 1; ++i)
        {
            dataJSON = getPhotoRequestData(userId, lastId);
            
            if (dataJSON == null)
                dataJSON = getPhotoRequestData(userId, lastId);
            
            JSONObject photoJSON = (JSONObject) dataJSON.get(dataJSON.size() - 1);
            downloadPhoto(dataJSON);
            lastId = photoJSON.get("id").toString();
            System.out.println(lastId);
        }
    }

    public static void main(String[] args) throws Exception {
        InstagramLoader instagramLoader = new InstagramLoader("axelrodvl");
        instagramLoader.downloadAll();
    }
}
