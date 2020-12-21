package app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SimulationActivities {

    List<SimulationActivity> simulationActivities;
    Integer totalDuration;
    Double totalCost;

}
