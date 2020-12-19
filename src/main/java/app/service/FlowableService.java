package app.service;

import app.dto.Model;
import app.dto.SimulationActivities;
import app.dto.SimulationActivity;
import app.dto.TaskDetail;
import app.repository.Repository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.flowable.engine.*;
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

    //    ProcessEngine processEngine;
    RuntimeService runtimeService;
    RepositoryService repositoryService;
    TaskService taskService;
    HistoryService historyService;


    public void deployProcessDefinition() {
        Model model = Repository.getModel();
        System.out.println(model);
        String filePath = model.getFilePath().substring(model.getFilePath().lastIndexOf("/") + 1);
        System.out.println(filePath);
        Deployment deployment =
                repositoryService
                        .createDeployment()
                        .addClasspathResource(filePath)
                        .deploy();

    }


    public void simulateProcessDefinition() {
        Map<String, Object> variables = probabilityService.chooseVariableValues(Repository.getVariables().getVariablesWithProbabilities());
        Repository.setInputVariables(variables);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(Repository.getProcess().getProcessId(), variables);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();

        // TODO: create or update exit from infinite loops
        /* Exit from ininite loop - calculate new probabilities */
        Set<String> taskNames = new HashSet<>();
        do {
            System.out.println(tasks);
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

        for (HistoricActivityInstance activity : activities) {
            System.out.println(
                    "ID: " + activity.getActivityId() + "\n" +
                            "Type: " + activity.getActivityType() + "\n" +
                            "Name: " + activity.getActivityName() + "\n" +
                            "Time: " + activity.getDurationInMillis() + " milliseconds \n"
            );
        }

        List<SimulationActivity> simulationActivities = new ArrayList<>();
        int sumDuration = 0;
        int sumCost = 0;
        for (HistoricActivityInstance activity : activities.stream().filter(x -> x.getActivityType().equals("userTask")).collect(Collectors.toList())) {
            int index = 0;
            for (TaskDetail taskDetail : Repository.getTaskDetails()) {
                if (taskDetail.getTaskId().equals(activity.getActivityId())) {
                    index = Repository.getTaskDetails().indexOf(taskDetail);
                    break;
                }
            }

            int duration = Repository.getTaskDetails().get(index).getDuration();
            int cost = Repository.getTaskDetails().get(index).getCost();
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
            System.out.println("TASK: " + activity.getActivityName() + " took " + duration + " minutes and cost " + cost + "$");
        }

        Repository.setSimulationActivities(new SimulationActivities(simulationActivities, sumDuration, sumCost));

        for (HistoricActivityInstance activity : activities.stream().filter(x -> x.getActivityType().equals("endEvent")).collect(Collectors.toList())) {
            System.out.println("END: " + activity.getActivityName());
        }

        System.out.println("All process took " + sumDuration + " minutes and cost " + sumCost + "$");

    }

}
