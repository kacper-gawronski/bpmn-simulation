package app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class Process {

    String processId;
    String processName;
    boolean processIsExecutable;


}
