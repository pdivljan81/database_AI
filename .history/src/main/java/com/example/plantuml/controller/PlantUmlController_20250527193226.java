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

    @GetMapping("/")
    private String cachedUml;
    public String showForm() {
        return "form";
    }

   @PostMapping("/generate-script")
    public String generateScript(@RequestParam("text") String text, Model model) throws Exception {
    String uml = plantUmlService.generateUmlFromText(text);
    cachedUml = uml;
    model.addAttribute("umlText", uml);

    // automatski generiši sliku iz skripte
    if (uml.startsWith("@startuml")) {
        String encoded = plantUmlService.encodePlantUml(uml);
        model.addAttribute("encodedUml", encoded);
    }

    return "form";
}

}
