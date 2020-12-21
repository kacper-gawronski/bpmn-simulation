package app.controller;

import app.dto.*;
import app.dto.Process;
import app.repository.Repository;
import app.service.FlowableService;
import app.service.ParseService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class SimulationController {


    ParseService parseService;
    FlowableService flowableService;

    // -------------------------------------------------------------

    @PostMapping(value = "/file-name", consumes = {MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<String> setFileName(@RequestBody String fileName) {
        Repository.setFileName(fileName);
        return ResponseEntity.ok().body("File name: " + fileName + " is set on the server");
    }

    @PostMapping(value = "/parse", consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Map<String, Object>> parseModel(@RequestBody String file) {
        Model model = new Model(parseService.saveModelToFile(file, Repository.getFileName()), file);

        Process process = parseService.setProcessParameters(model);
        Variables variables = parseService.getVariables(model);
        List<TaskDetail> taskDetails = parseService.getTasksDetails(model);

        Repository.setModel(model);
        Repository.setProcess(process);
        Repository.setVariables(variables);
        Repository.setTaskDetails(taskDetails);

        System.out.println(Repository.getProcess());

        Map<String, Object> modelProperties = new HashMap<>();
        modelProperties.put("process", process);
        modelProperties.put("variables", variables);
        modelProperties.put("tasks", taskDetails);

        return ResponseEntity.ok().body(modelProperties);
    }


    @PostMapping(value = "/set-variables", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<Variables> parseModel(@RequestBody Map<String, Map<Object, Integer>> variablesWithProbabilities) {
        System.out.println(variablesWithProbabilities);
        Repository.setVariables(new Variables(Repository.getVariables().getPossibleVariables(), variablesWithProbabilities));

        return ResponseEntity.ok().body(Repository.getVariables());
    }

    @PostMapping(value = "/set-task-values")
    public ResponseEntity<List<TaskDetail>> parseModel(@RequestBody List<TaskDetail> taskDetails) {
        System.out.println(taskDetails);
        Repository.setTaskDetails(taskDetails);

        return ResponseEntity.ok().body(taskDetails);
    }

    @PostMapping("/deploy")
    public ResponseEntity<?> deployWorkflow() {
        flowableService.deployProcessDefinition();
        return ResponseEntity.ok().body("Process successfully deployed");
    }

    @PostMapping("/simulation")
    public ResponseEntity<SimulationActivities> simulateProcess() {
        flowableService.simulateProcessDefinition();
        return ResponseEntity.ok().body(Repository.getSimulationActivities());
    }

    @PostMapping("/deploy-simulation")
    public ResponseEntity<SimulationActivities> deployAndSimulateProcess() {
        flowableService.deployProcessDefinition();
        flowableService.simulateProcessDefinition();
        return ResponseEntity.ok().body(Repository.getSimulationActivities());
    }

    // -------------------------------------------------------------


    @GetMapping("/api")
    public Collection<String> test() {
        return List.of("answer", "from", "backend");
    }


}
