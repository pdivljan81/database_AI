package com.example.plantuml.controller;

import com.example.plantuml.service.PlantUmlService;
import com.example.plantuml.service.PlantUmlService.ModelType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PlantUmlController {

    @Autowired
    private PlantUmlService plantUmlService;

    private String cachedUml;

    @GetMapping("/")
    public String showForm() {
        return "form";
    }

    @PostMapping("/generate-script")
    public String generateScript(@RequestParam("text") String text,
                                 @RequestParam("model") String model,
                                 Model modelAttr) throws Exception {
        ModelType modelType = ModelType.valueOf(model.toUpperCase());
        String uml = plantUmlService.generateUmlFromText(text, modelType);
        cachedUml = uml;
        modelAttr.addAttribute("umlText", uml);

        if (uml.startsWith("@startuml")) {
            String encoded = plantUmlService.encodePlantUml(uml);
            modelAttr.addAttribute("encodedUml", encoded);
        }

        return "form";
    }
}
