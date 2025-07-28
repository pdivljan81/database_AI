package com.example.plantuml.controller;

import com.example.plantuml.service.PlantUmlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Controller
public class PlantUmlController {

    @Autowired
    private PlantUmlService plantUmlService;

    private String cachedUml;

    private static final String GEMINI_API_KEY = "AIzaSyCY64JlmXVcMNaTlyWxb2n3SKlpZkCD-Ww";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;

    @GetMapping("/")
    public String showForm() {
        return "form";
    }

    @PostMapping("/generate-script")
    public String generateScript(
            @RequestParam("text") String text,
            @RequestParam(value = "model", required = false, defaultValue = "mistral") String modelChoice,
            Model model
    ) throws Exception {
        String uml;

        if ("gemini".equalsIgnoreCase(modelChoice)) {
            // ➤ Poziv Gemini API-ja
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> part = new HashMap<>();
            part.put("text", text);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", Collections.singletonList(part));

            Map<String, Object> payload = new HashMap<>();
            payload.put("contents", Collections.singletonList(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(GEMINI_API_URL, request, Map.class);

            // ➤ Parsiranje odgovora
            Map candidate = (Map) ((List<?>) response.getBody().get("candidates")).get(0);
            Map contentMap = (Map) candidate.get("content");
            List<?> partsList = (List<?>) contentMap.get("parts");
            Map partMap = (Map) partsList.get(0);
            uml = (String) partMap.get("text");

        } else {
            // ➤ OSTAJE ISTO: Lokalni (Mistral)
            uml = plantUmlService.generateUmlFromText(text);
        }

        // ➤ Izdvoji @startuml do @enduml
        int start = uml.indexOf("@startuml");
        int end = uml.indexOf("@enduml") + "@enduml".length();
        if (start != -1 && end != -1 && end > start) {
            uml = uml.substring(start, end).trim();
        }

        cachedUml = uml;
        model.addAttribute("umlText", uml);

        if (uml.startsWith("@startuml")) {
            String encoded = plantUmlService.encodePlantUml(uml);
            model.addAttribute("encodedUml", encoded);
        }

        return "form";
    }
}
