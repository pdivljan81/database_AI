package com.example.plantuml.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

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

    /**
     * Encoduje PlantUML skriptu za prikaz preko plantuml.com servera
     */
    public String encodePlantUml(String umlSource) {
        byte[] data = umlSource.getBytes(StandardCharsets.UTF_8);
        Deflater deflater = new Deflater(9, true);
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            baos.write(buffer, 0, count);
        }
        deflater.end();

        return encode64(baos.toByteArray());
    }

    private String encode64(byte[] data) {
        final StringBuilder res = new StringBuilder();
        final String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";
        int current = 0;
        int bits = 0;
        for (byte b : data) {
            current = (current << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 6) {
                bits -= 6;
                res.append(alphabet.charAt((current >> bits) & 0x3F));
            }
        }
        if (bits > 0) {
            res.append(alphabet.charAt((current << (6 - bits)) & 0x3F));
        }
        return res.toString();
    }
}
