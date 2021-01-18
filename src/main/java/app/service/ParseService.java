package app.service;

import app.dto.Model;
import app.dto.Process;
import app.dto.TaskDetail;
import app.dto.Variables;
import javafx.util.Pair;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class ParseService {

    ProbabilityService probabilityService;

    //-----------------------------------------------

    void removeRepeatedValues(
            Map<String, Set<Object>> variables,
            String variableName,
            String variableValue
    ) {
        if (variables.containsKey(variableName)) {
            Set<Object> set = new HashSet<>();
            set.addAll(variables.get(variableName));
            set.add(variableValue);
            variables.put(variableName, set);
        } else {
            variables.put(variableName, Set.of(variableValue));
        }
    }

    Pair<String, NodeList> getNodeListFromFile(String filePath) throws IOException, SAXException, ParserConfigurationException {
        File file = new File(filePath);
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(file);
        String PREFIX = (document.getDocumentElement().getNodeName().contains("bpmn")) ? "bpmn:" : "";
        NodeList nodeList = document.getElementsByTagName(PREFIX + "process");
        return new Pair<>(PREFIX, nodeList);
    }

    //-----------------------------------------------

    public String saveModelToFile(String file, String fileName) {
        String filePath = "src/main/resources/" + fileName;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(file);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filePath;
    }

    public Process setProcessParameters(Model model) {
        String processId = null;
        String processName = null;
        boolean isExecutable = false;

        try {
            Pair<String, NodeList> pair = getNodeListFromFile(model.getFilePath());
            String PREFIX = pair.getKey();
            NodeList nodeList = pair.getValue();

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                processId = node.getAttributes().getNamedItem("id").getNodeValue();
                processName = node.getAttributes().getNamedItem("name").getNodeValue();
                isExecutable = node.getAttributes().getNamedItem("isExecutable").getNodeValue().equals("true");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Process(processId, processName, isExecutable);
    }

    public Variables getVariables(Model model) {
        try {
            Pair<String, NodeList> pair = getNodeListFromFile(model.getFilePath());
            String PREFIX = pair.getKey();
            NodeList nodeList = pair.getValue();

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element process = (Element) node;

                    // find variables in model (inside <conditionExpression> tag)
                    Map<String, Set<Object>> variables = new HashMap<>();
                    String tagName = PREFIX + "conditionExpression";
                    String tagContent;
                    String variableName;
                    String variableValue;

                    for (int j = 0; j < process.getElementsByTagName(tagName).getLength(); j++) {
                        tagContent = process.getElementsByTagName(tagName).item(j).getTextContent();
                        tagContent = tagContent.substring(tagContent.indexOf('{') + 1, tagContent.indexOf('}'));

                        // TODO: Check that empty string or string which is space character should not have other implementation
                        tagContent = tagContent.replaceAll(" ", "");

                        // boolean value with NOT operator
                        if (tagContent.startsWith("!")) {
                            variableName = tagContent.substring(1);
                            variables.put(variableName, Set.of(Boolean.TRUE, Boolean.FALSE));
                        }
                        // boolean value without any operator
                        else if (!tagContent.contains("=")) {
                            variableName = tagContent;
                            variables.put(variableName, Set.of(Boolean.TRUE, Boolean.FALSE));
                        }
                        // string value with != operator
                        else if (tagContent.endsWith("\"") && tagContent.contains("!=")) {
                            variableName = tagContent.substring(0, tagContent.indexOf("!="));
                            variableValue = tagContent.substring(tagContent.indexOf("\"") + 1, tagContent.lastIndexOf("\""));
                            removeRepeatedValues(variables, variableName, variableValue);
                        }
                        // other string value
                        else if (tagContent.endsWith("\"")) {
                            variableName = tagContent.substring(0, tagContent.indexOf("="));
                            variableValue = tagContent.substring(tagContent.indexOf("\"") + 1, tagContent.lastIndexOf("\""));
                            removeRepeatedValues(variables, variableName, variableValue);
                        }
                        // boolean value equal to true or false with != operator
                        else if ((tagContent.contains("true") || tagContent.contains("false")) && tagContent.contains("!=")) {
                            variableName = tagContent.substring(0, tagContent.indexOf("!="));
                            variables.put(variableName, Set.of(Boolean.TRUE, Boolean.FALSE));
                        }
                        // boolean value equal to true or false
                        else if (tagContent.contains("true") || tagContent.contains("false")) {
                            variableName = tagContent.substring(0, tagContent.indexOf("="));
                            variables.put(variableName, Set.of(Boolean.TRUE, Boolean.FALSE));
                        }
                        // integer value
                        else if (tagContent.contains("!=")) {
                            tagContent = tagContent.concat(" ");
                            variableName = tagContent.substring(0, tagContent.indexOf("!="));
                            variableValue = tagContent.substring(tagContent.indexOf("!=") + 2, tagContent.lastIndexOf(" "));
                            removeRepeatedValues(variables, variableName, variableValue);
                        }
                        // integer value
                        else {
                            tagContent = tagContent.concat(" ");
                            variableName = tagContent.substring(0, tagContent.indexOf("=="));
                            variableValue = tagContent.substring(tagContent.indexOf("==") + 2, tagContent.lastIndexOf(" "));
                            removeRepeatedValues(variables, variableName, variableValue);
                        }
                    }

                    Map<String, Map<Object, Integer>> variablesWithProbabilities = probabilityService.calculateMockProbabilities(variables);
                    return new Variables(variables, variablesWithProbabilities);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<TaskDetail> getTasksDetails(Model model) {
        try {
            Pair<String, NodeList> pair = getNodeListFromFile(model.getFilePath());
            String PREFIX = pair.getKey();
            NodeList nodeList = pair.getValue();

            List<TaskDetail> taskDetails = new ArrayList<>();

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element process = (Element) node;

                    // TODO: Check this and maybe change "userTask" for something generic
                    String tagName = PREFIX + "userTask";

                    TaskDetail taskDetail;
                    String taskId;
                    String taskName;

                    for (int j = 0; j < process.getElementsByTagName(tagName).getLength(); j++) {
                        Element element = (Element) process.getElementsByTagName(tagName).item(j);
                        taskId = element.getAttribute("id");
                        taskName = element.getAttribute("name");
                        taskDetail = new TaskDetail(taskId, taskName);
                        taskDetails.add(taskDetail);
                    }
                }
            }
            return taskDetails;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
