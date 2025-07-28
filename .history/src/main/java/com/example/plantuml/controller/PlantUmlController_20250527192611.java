package com.example.plantuml.controller;

import com.example.plantuml.service.PlantUmlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PlantUmlController {

    @Autowired
    private PlantUmlService plantUmlService;

    private String cachedUml = null;

    @GetMapping("/")
    public String showForm() {
        return "form";
    }

    @PostMapping("/generate-script")
    public String generateScript(@RequestParam("text") String text, Model model) throws Exception {
        // Generiši PlantUML skriptu putem LLM API-ja
        String uml = plantUmlService.generateUmlFromText(text);

        // Trimuj sve što je van @startuml i @enduml
        int start = uml.indexOf("@startuml");
        int end = uml.indexOf("@enduml") + "@enduml".length();
        if (start != -1 && end != -1 && end > start) {
            uml = uml.substring(start, end).trim();
        }

        cachedUml = uml;
        String encoded = plantUmlService.encodePlantUml(uml);

        model.addAttribute("umlText", uml);
        model.addAttribute("encodedUml", encoded);
        return "form";
    }
}
