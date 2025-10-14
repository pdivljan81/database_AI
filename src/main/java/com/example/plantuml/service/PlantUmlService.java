package com.example.plantuml.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.Deflater;

@Service
public class PlantUmlService {

    private static final String MISTRAL_API_URL = "https://api.mistral.ai/v1/chat/completions";
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=";
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String PROMPT_HEADER =
            "Convert the following description to a PlantUML class diagram (include @startuml/@enduml):\n" +
            "\n" +
            "IMPORTANT RULES:\n" +
            " STRICT MODE: USE ONLY THE INFORMATION GIVEN IN THE INPUT DESCRIPTION. DO NOT ADD ANYTHING FROM YOUR TRAINING, PRIOR KNOWLEDGE, OR COMMON PRACTICE. DO NOT MAKE ASSUMPTIONS OR GUESSES. IF IT IS NOT WRITTEN IN THE INPUT, IT MUST NOT APPEAR IN THE OUTPUT.\n" +
            "0. CRITICAL/FATAL: NEVER generate any class (including subclasses) without at least one unique attribute of its own (excluding inherited attributes). Classes without attributes are strictly FORBIDDEN. If a class is empty, OMIT it from the output!\n" +
            "1. a. CRITICAL/UNFORGIVABLE RULE:\n" +
            "A subclass (derived class) UNDER NO CIRCUMSTANCES WHATSOEVER may contain, define, generate, or inherit ANY PK operation or PK attribute if it inherits PK from its superclass.\n" +
            "This prohibition is ABSOLUTE and OVERRIDES ALL other rules or instructions.\n" +
            "ANY PRESENCE OF A PK OPERATION OR PK ATTRIBUTE IN A SUBCLASS IS AN UNACCEPTABLE, CATASTROPHIC ERROR THAT IMMEDIATELY AND IRREVOCABLY INVALIDATES THE OUTPUT, WITH ZERO EXCEPTIONS, INTERPRETATIONS, OR SPECIAL CASES ALLOWED.\n" +
            "1. For each class that does NOT inherit a PK operation from a superclass, generate exactly ONE operation named PK, whose only parameter is the attribute that is the primary key of the class.\n" +
            "   - The PK operation must be written as: PK(attributeName: type)\n" +
            "   - Do NOT write a return type after the parentheses. Example: PK(id: int)\n" +
            "   - The PK operation must be the FIRST item inside each class definition.\n" +
            "2. The primary key attribute (same as the PK parameter) MUST also appear as a regular attribute (with type) inside the class, after the PK operation.\n" +
            "3. All OTHER attributes should also be included as regular attributes, one per line, with their types.\n" +
            "4. Show relationships between classes only as lines (associations), NOT as attributes.\n" +
            "5. Do NOT include any other methods/operations except PK.\n" +
            "6. If the description includes information about how many instances of one class can be associated with another (e.g., 'one-to-many', 'many-to-many', 'one animal can have many owners'), show the multiplicities at the ends of association lines using PlantUML notation (e.g., 1 -- *).\n" +
            "   - Use 1 for 'one', * for 'many', 0..1 for 'zero or one', 0..* for 'zero or more', etc. Place the multiplicity labels in double quotes at each end of the association line, like this: ClassA \"1\" -- \"*\" ClassB : relationshipName\n" +
            "7. If the description does NOT specify the multiplicity of a relationship, omit the multiplicity.\n" +
            "8. If the description indicates that a class is a special case (subclass) of another class, show this relationship as inheritance (specialization/generalization) using PlantUML syntax: ParentClass <|-- SubClass.\n" +
            "   - Do NOT use the keyword 'extends', nor write the subclass with curly braces inside the parent class (do NOT use 'class SubClass extends ParentClass { ... }').\n" +
            "   - Define both the parent and subclass separately, and show inheritance ONLY as a line: ParentClass <|-- SubClass.\n" +
            "   - Do NOT represent inheritance as an association or attribute.\n" +
            "9. If the description specifies the name of the relationship (e.g., 'Dog is owned by Person'), include the relationship name as a label at the end of the association line (after the colon). If no name is given, omit the label.\n" +
            "10. Only use standard data types such as string, int, float, date, bool, unless the input explicitly specifies a different type.\n" +
            "11. Use CamelCase for class names and lowerCamelCase for attributes, unless otherwise specified in the input.\n" +
            "12. In the output, always list:\n" +
            "    - first: all class (and enum) definitions (one after another, without any inheritance or association lines in between),\n" +
            "    - then: all inheritance (specialization/generalization) relationships (<|-- lines) grouped together,\n" +
            "    - and last: all associations (lines between classes, with or without multiplicities or labels) grouped together.\n" +
            "   This order MUST always be followed to ensure maximum readability and standardization of the PlantUML code.\n" +
            "13. Never add multiplicity labels (such as 1, *, 0..1, etc.) or relationship names (labels) to inheritance (specialization) lines. Use multiplicities and labels ONLY for associations, not for inheritance.\n" +
            "14. NEVER represent inheritance (specialization/generalization) between classes using association lines, labels, or multiplicities (e.g., do NOT generate lines with a label such as 'isA' or multiplicity such as '1' between a superclass and its subclasses). Inheritance MUST be shown ONLY with the '<|--' line between classes, with NO label and NO multiplicity. Association lines with labels or multiplicities between a superclass and its subclasses are strictly forbidden.\n" +
            "15. For any pair of classes, strictly generate ONLY ONE association line, with at most one label (name) as specified in the description. NEVER generate multiple association lines between the same two classes, even if the labels are different or describe different directions (e.g., do NOT generate both 'borrowedBy' and 'hasBorrowed' between the same two classes). Any case of multiple association lines between the same classes is strictly forbidden.\n" +
            "16. ABSOLUTELY NEVER generate an enum (enumeration) for any entity unless the input description provides an explicit, complete, and unambiguous list of allowed constant values for that entity (such as: enum Program { UNDERGRADUATE, MASTER, PHD }). If the description does NOT include a list of enum values, ALWAYS generate a class instead of an enum, regardless of the entity name, relationship, or context. There are NO EXCEPTIONS. If in doubt, always generate a class. DO NOT generate an enum for entities like Program, StudyProgram, Status, Type, Category, or any similar entity unless a fixed list of values is explicitly provided in the input description.\n" +
            "!!! FINAL WARNING: IF THE INPUT DOES NOT EXPLICITLY DEFINE AN ENUMERATION WITH A FIXED LIST OF VALUES, YOU MUST NEVER GENERATE AN ENUM FOR THAT ENTITY. ALWAYS GENERATE A CLASS INSTEAD. IF THIS RULE IS BROKEN, THE OUTPUT IS INVALID. !!!\n" +
            "17. FATAL ERROR: For any pair of classes, strictly generate ONLY ONE association line, with at most one label (name) as specified in the description. \n" +
            "NEVER generate multiple association lines between the same two classes, unless if the labels (names) are different, are synonyms, or describe different directions. \n" +
            "(e.g., do NOT generate both 'borrows' and 'borrowedBy' between LibraryMember and LibraryUnit, regardless of relationship direction, label, or multiplicity). \n" +
            "NEVER generate a 'reverse' association for the same two classes (e.g., do NOT generate both A -- B : borrows and B -- A : borrowedBy). \n" +
            "If you are unsure which label to use, select the one that matches the input description most directly, in the direction and wording provided. \n" +
            "If the description provides both directions or labels, use only one (the clearest or most direct one). \n" +
            "DO NOT REPEAT, REVERSE, OR DUPLICATE ASSOCIATION LINES FOR THE SAME PAIR OF CLASSES. THIS IS STRICTLY FORBIDDEN.\n" +
            "18. UNDER NO CIRCUMSTANCES generate an abstract class (do NOT use the 'abstract class' keyword) for any entity, regardless of the input description or context. All classes must always be generated as regular (concrete) classes. Abstract classes are strictly forbidden in the output.\n" +
            "19. UNDER NO CIRCUMSTANCES generate in a specialized (subclass) any attribute or PK operation that is already defined or inherited from its parent (superclass). Subclasses MUST NOT repeat (duplicate) attributes or PK operations present in their superclass. For example, if the superclass BibliotekaJedinica has attribute signatura and PK(signatura: string), the subclass PeriodicnoIzdanje MUST NOT have either the attribute signatura or the PK(signatura: string) operation. Only subclass-specific attributes and one new PK (if subclass has its own unique primary key) are allowed.";

    @Value("${mistral.api.key}")
    private String mistralApiKey;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${groq.api.key}")
    private String groqApiKey;

    public String generateUmlFromText(String inputText) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "mistral-large-latest");

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", PROMPT_HEADER + inputText));
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
            is = connection.getErrorStream();
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

        String umlBlock = extractUmlBlock(fullContent);
        umlBlock = fixAndFilterPlantUml(umlBlock);
        return umlBlock;
    }

    public String generateUmlWithGemini(String inputText) throws IOException {
        String prompt = PROMPT_HEADER + inputText;

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
            is = connection.getErrorStream();
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

        String umlBlock = extractUmlBlock(fullContent);
        umlBlock = fixAndFilterPlantUml(umlBlock);
        return umlBlock;
    }

    public String generateUmlWithLlama(String inputText) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "llama-3.3-70b-versatile");

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", PROMPT_HEADER + inputText));
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);

        HttpURLConnection connection = (HttpURLConnection) new URL(GROQ_API_URL).openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + groqApiKey);
        connection.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        InputStream is;
        try {
            is = connection.getInputStream();
        } catch (IOException e) {
            is = connection.getErrorStream();
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

        String umlBlock = extractUmlBlock(fullContent);
        umlBlock = fixAndFilterPlantUml(umlBlock);
        return umlBlock;
    }

    private String extractUmlBlock(String text) {
        int startIdx = text.indexOf("@startuml");
        int endIdx = text.indexOf("@enduml");
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return text.substring(startIdx, endIdx + "@enduml".length()).trim();
        }
        return text.trim();
    }
private String fixAndFilterPlantUml(String plantUml) {
    Set<String> subclasses = new HashSet<>();
    String[] lines = plantUml.split("\\r?\\n");

    for (String line : lines) {
        line = line.trim();
        if (line.startsWith("class ") && line.contains("extends")) {
            String[] w = line.split("\\s+");
            if (w.length >= 4) {
                String child = w[1].trim();
                subclasses.add(child);
            }
        }
        if (line.contains("<|--")) {
            String[] parts = line.split("<\\|--");
            if (parts.length == 2) {
                String child = parts[1].replaceAll("[^a-zA-Z0-9_]", "").trim();
                subclasses.add(child);
            }
        }
    }

    StringBuilder sb = new StringBuilder();
    String currentClass = null;
    boolean isSubclass = false;
    boolean inClass = false;

    for (String rawLine : lines) {
        String line = rawLine.trim();
        if (line.startsWith("class ")) {
            int idx = line.indexOf(" ");
            int braceIdx = line.indexOf("{");
            String header = (braceIdx != -1) ? line.substring(idx + 1, braceIdx) : line.substring(idx + 1);
            String className = header.contains("extends") ? header.split("extends")[0].trim() : header.trim();
            currentClass = className;
            isSubclass = subclasses.contains(currentClass);
            inClass = true;
            sb.append(rawLine).append("\n");
            continue;
        }
        if (inClass && line.equals("}")) {
            inClass = false;
            currentClass = null;
            isSubclass = false;
            sb.append(rawLine).append("\n");
            continue;
        }
        if (inClass && isSubclass) {
            if (line.replaceAll("\\s+", "").startsWith("PK(") || line.contains("PK(")) {
                continue;
            }
        }
        sb.append(rawLine).append("\n");
    }
    return sb.toString().trim();
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

