package app.service;

import app.dto.Model;
import app.dto.SimulationActivities;
import app.dto.SimulationActivity;
import app.dto.TaskDetail;
import app.repository.Repository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class FlowableService {

    ProbabilityService probabilityService;
    CostService costService;

    RuntimeService runtimeService;
    RepositoryService repositoryService;
    TaskService taskService;
    HistoryService historyService;


    public String deployProcessDefinition() {

        Model model = Repository.getModel();
        String filePath = model.getFilePath().substring(model.getFilePath().lastIndexOf("/") + 1);
        Deployment deployment =
                repositoryService
                        .createDeployment()
                        .addString(filePath, Repository.getModel().getFileContent())
//                        .addClasspathResource(filePath)
                        .deploy();

        return deployment.getName();
    }


    public SimulationActivities simulateProcessDefinition() {
        Map<String, Object> variables = probabilityService.chooseVariableValues(Repository.getVariables().getVariablesWithProbabilities());
        Repository.setInputVariables(variables);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(Repository.getProcess().getProcessId(), variables);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();

        /* Exit from infinite loop - calculate new variables */
        Set<String> taskNames = new HashSet<>();
        do {
            for (Task task : tasks) {
                if (taskNames.contains(task.getName())) {
                    taskService.complete(task.getId(), probabilityService.chooseVariableValues(Repository.getVariables().getVariablesWithProbabilities()));
                } else {
                    taskService.complete(task.getId());
                    taskNames.add(task.getName());
                }
            }
            tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        }
        while (!tasks.isEmpty());

        List<HistoricActivityInstance> activities =
                historyService
                        .createHistoricActivityInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .finished()
                        .orderByHistoricActivityInstanceEndTime()
                        .asc()
                        .list();

        /*
        for (HistoricActivityInstance activity : activities) {
            System.out.println(
                    "ID: " + activity.getActivityId() + "\n" +
                            "Type: " + activity.getActivityType() + "\n" +
                            "Name: " + activity.getActivityName() + "\n");
        }
         */


        List<SimulationActivity> simulationActivities = new ArrayList<>();
        int sumDuration = 0;
        double sumCost = 0;
        for (HistoricActivityInstance activity : activities.stream().filter(x -> x.getActivityType().equals("userTask")).collect(Collectors.toList())) {
            int index = 0;
            for (TaskDetail taskDetail : Repository.getTaskDetails()) {
                if (taskDetail.getTaskId().equals(activity.getActivityId())) {
                    index = Repository.getTaskDetails().indexOf(taskDetail);
                    break;
                }
            }

            int duration = Repository.getTaskDetails().get(index).getDuration();
            double cost = costService.calculateCost(Repository.getTaskDetails().get(index).getCost(), duration);
            SimulationActivity simulationActivity = new SimulationActivity(
                    activity.getActivityId(),
                    activity.getActivityName(),
                    activity.getActivityType(),
                    duration,
                    cost
            );
            simulationActivities.add(simulationActivity);

            sumDuration += duration;
            sumCost += cost;
        }

        HistoricActivityInstance endEvent = activities.stream().filter(activity -> activity.getActivityType().equals("endEvent")).findAny().orElse(null);
//        System.out.println("END EVENT: " + endEvent.getActivityName());

        SimulationActivities simulationResult = new SimulationActivities(simulationActivities, endEvent.getActivityName(), sumDuration, sumCost);
        Repository.setSimulationActivities(simulationResult);

        Repository.getAllSimulations().add(simulationResult);
        return simulationResult;
    }

}
