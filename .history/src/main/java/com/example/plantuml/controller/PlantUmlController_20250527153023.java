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

    // Prvo dugme – generiše skriptu (@startuml...@enduml)
    @PostMapping("/generate-script")
    public String generateScript(@RequestParam("text") String text, Model model) throws Exception {
        String uml = plantUmlService.generateUmlFromText(text);
        cachedUml = uml;
        model.addAttribute("umlText", uml);
        return "form";
    }

    // Drugo dugme – enkoduje prethodno generisanu skriptu i prikazuje sliku
    @PostMapping("/generate-diagram")
    public String generateDiagram(Model model) throws Exception {
        if (cachedUml != null && cachedUml.startsWith("@startuml")) {
            String encoded = plantUmlService.encodePlantUml(cachedUml);
            model.addAttribute("umlText", cachedUml);
            model.addAttribute("encodedUml", encoded);
        }
        return "form";
    }
}
