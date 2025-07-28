package com.example.plantuml.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class PlantUmlService {

    private static final String API_URL = "https://api.mistral.ai/v1/chat/completions";
    private static final String API_KEY = "Uxg63tTXlpS9eVYlwsSXIGJnFjPeRZTN";

    /**
     * Poziva Mistral API i izdvaja čistu PlantUML skriptu
     */
    public String generateUmlFromText(String inputText) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "mistral-large-latest");

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", "Convert the following description to a PlantUML class diagram (include @startuml/@enduml):\n" + inputText));
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);

        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
        connection.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (InputStream is = connection.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }

        JSONObject jsonResponse = new JSONObject(response.toString());
        String fullContent = jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        // Izdvoji samo PlantUML skriptu
        int startIdx = fullContent.indexOf("@startuml");
        int endIdx = fullContent.indexOf("@enduml");

        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return fullContent.substring(startIdx, endIdx + "@enduml".length()).trim();
        } else {
            return fullContent.trim(); // fallback ako tagovi nisu pronađeni
        }
    }
}
