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

    public enum ModelType {
        MISTRAL, GEMINI, GPT
    }

    private static final String API_URL_MISTRAL = "https://api.mistral.ai/v1/chat/completions";
    private static final String API_URL_GEMINI = "key=GEMINI_KEY";
    private static final String API_URL_GPT = "https://api.openai.com/v1/chat/completions";

    private static final String MISTRAL_KEY = "Uxg63tTXlpS9eVYlwsSXIGJnFjPeRZTN";
    private static final String GEMINI_KEY = "AIzaSyCY64JlmXVcMNaTlyWxb2n3SKlpZkCD-Ww";
    private static final String GPT_KEY = "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";

    public String generateUmlFromText(String inputText, ModelType modelType) throws IOException {
        return switch (modelType) {
            case MISTRAL -> callMistral(inputText);
            case GEMINI -> callGemini(inputText);
            case GPT -> callGpt(inputText);
        };
    }

    private String callMistral(String inputText) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "mistral-large-latest");

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", "Convert the following description to a PlantUML class diagram (include @startuml/@enduml):\n" + inputText));
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);

        return callApi(API_URL_MISTRAL, requestBody.toString(), MISTRAL_KEY, "Authorization");
    }

    private String callGemini(String inputText) throws IOException {
        JSONObject requestBody = new JSONObject()
            .put("contents", new JSONArray()
                .put(new JSONObject()
                    .put("parts", new JSONArray()
                        .put(new JSONObject()
                            .put("text", "Convert the following description to a PlantUML class diagram (include @startuml/@enduml):\n" + inputText)))
                    .put("role", "user")));

        return callApi(API_URL_GEMINI, requestBody.toString(), null, null);
    }

    private String callGpt(String inputText) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-3.5-turbo");

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", "Convert the following description to a PlantUML class diagram (include @startuml/@enduml):\n" + inputText));
        requestBody.put("messages", messages);

        return callApi(API_URL_GPT, requestBody.toString(), GPT_KEY, "Authorization");
    }

    private String callApi(String urlStr, String body, String key, String authHeader) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        if (key != null && authHeader != null) {
            connection.setRequestProperty(authHeader, "Bearer " + key);
        }
        connection.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
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

        String fullContent = response.toString();
        int startIdx = fullContent.indexOf("@startuml");
        int endIdx = fullContent.indexOf("@enduml");

        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return fullContent.substring(startIdx, endIdx + "@enduml".length()).trim();
        } else {
            return fullContent.trim();
        }
    }

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
