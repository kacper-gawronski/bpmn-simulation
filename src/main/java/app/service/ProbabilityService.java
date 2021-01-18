package app.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class ProbabilityService {


    public Map<String, Object> chooseVariableValues(Map<String, Map<Object, Integer>> variablesWithProbabilities) {
        Map<String, Object> inputVariables = new HashMap<>();

        for (String name : variablesWithProbabilities.keySet()) {
            int probabilityPercentage = 0;
            int randomPercentage = new Random().nextInt(100);
//            System.out.println(randomPercentage);
            for (int i = 0; i < variablesWithProbabilities.get(name).size(); i++) {
//                System.out.println(variablesWithProbabilities.get(name).values().toArray()[i]);
                probabilityPercentage += (int) variablesWithProbabilities.get(name).values().toArray()[i];
                if (randomPercentage <= probabilityPercentage) {
                    Object value = variablesWithProbabilities.get(name).keySet().toArray()[i];
//                    System.out.println(value);
//                    System.out.println(value.getClass());

                    // TODO: check this because this may cause errors (when string value is "true" or "false") - but for now is unnecessary
                    if (value.equals("true")) {
                        inputVariables.put(name, true);
                    } else if (value.equals("false")) {
                        inputVariables.put(name, false);
                    } else inputVariables.put(name, value);
                    break;
                }

            }
        }

        return inputVariables;
    }


    public Map<String, Map<Object, Integer>> calculateMockProbabilities(Map<String, Set<Object>> variableMap) {

        // probability of using a given variable
        Map<String, Map<Object, Integer>> variablesProbability = new HashMap<>();
        int percentage;
        int numberOfValues;
        int setIndex = 0;
        String setName;
        for (Set<Object> set : variableMap.values()) {
            Map<Object, Integer> valuesProbability = new HashMap<>();
            percentage = 100;
            numberOfValues = set.size();
            int index = 0;
            for (Object object : set) {
                if (index + 1 == numberOfValues) {
                    valuesProbability.put(object, percentage);
                } else {
                    Integer probability = 100 / numberOfValues;
                    percentage -= probability;
                    valuesProbability.put(object, probability);
                    index++;
                }
            }

            setName = variableMap.keySet().toArray()[setIndex].toString();
            setIndex++;
            variablesProbability.put(setName, valuesProbability);

        }

//        System.out.println(variablesProbability);
        return variablesProbability;
    }

}
