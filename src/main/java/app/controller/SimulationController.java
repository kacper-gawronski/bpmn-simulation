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
@CrossOrigin
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class SimulationController {


    ParseService parseService;
    FlowableService flowableService;

    // -------------------------------------------------------------

    @PostMapping(value = "/parse", consumes = {MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Map<String, Object>> parseModel(@RequestBody String file) {
        Model model = new Model(parseService.saveModelToFile(file));

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


    @PostMapping(value = "/set-variables")
    public ResponseEntity<Variables> parseModel(@RequestBody Variables variables) {
        System.out.println(variables);
        Repository.setVariables(variables);

        return ResponseEntity.ok().body(variables);
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
        return ResponseEntity.ok().body(null);
    }

    @PostMapping("/simulation")
    public ResponseEntity<SimulationActivities> simulateProcess() {
        flowableService.simulateProcessDefinition();
        return ResponseEntity.ok().body(Repository.getSimulationActivities());
    }


    // -------------------------------------------------------------


    @GetMapping("/api")
    public Collection<String> test() {
        return List.of("answer", "from", "backend");
    }


}
