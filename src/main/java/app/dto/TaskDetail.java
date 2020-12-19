package app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class TaskDetail {

    String taskId;
    String taskName;
    Integer duration;
    Integer cost;

    public TaskDetail(String taskId, String taskName) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.duration = 0;
        this.cost = 0;
    }


}
