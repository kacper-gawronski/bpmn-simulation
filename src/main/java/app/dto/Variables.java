package app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class Variables {

    Map<String, Set<Object>> possibleVariables;
    Map<String, Map<Object, Integer>> variablesWithProbabilities;

}
