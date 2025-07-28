package com.example.plantuml.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

@Service
public class PlantUmlService {

    private static final String MISTRAL_API_URL = "https://api.mistral.ai/v1/chat/completions";
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=";

    @Value("${mistral.api.key}")
    private String mistralApiKey;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    /**
     * Poziva Mistral API i vraća PlantUML skriptu
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

        HttpURLConnection connection = (HttpURLConnection) new URL(MISTRAL_API_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + mistralApiKey);
        connection.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        InputStream is;
        try {
            is = connection.getInputStream();
        } catch (IOException e) {
            is = connection.getErrorStream(); // fallback ako je greška
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
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

        return extractUmlBlock(fullContent);
    }

    /**
     * Poziva Gemini API i vraća PlantUML skriptu
     */
    public String generateUmlWithGemini(String inputText) throws IOException {
        String prompt = "Pretvori sljedeći opis u ispravnu PlantUML skriptu koja počinje sa @startuml i završava sa @enduml:\n\n" + inputText;

        JSONObject part = new JSONObject().put("text", prompt);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject requestBody = new JSONObject().put("contents", new JSONArray().put(content));

        String fullUrl = GEMINI_BASE_URL + geminiApiKey;
        HttpURLConnection connection = (HttpURLConnection) new URL(fullUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        InputStream is;
        try {
            is = connection.getInputStream();
        } catch (IOException e) {
            is = connection.getErrorStream(); // fallback za greške
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }

        JSONObject jsonResponse = new JSONObject(response.toString());
        String fullContent = jsonResponse
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        return extractUmlBlock(fullContent);
    }

    /**
     * Izdvaja @startuml blok iz teksta
     */
    private String extractUmlBlock(String text) {
        int startIdx = text.indexOf("@startuml");
        int endIdx = text.indexOf("@enduml");
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return text.substring(startIdx, endIdx + "@enduml".length()).trim();
        }
        return text.trim(); // fallback
    }

    /**
     * Encoduje PlantUML skriptu za prikaz na plantuml.com
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
