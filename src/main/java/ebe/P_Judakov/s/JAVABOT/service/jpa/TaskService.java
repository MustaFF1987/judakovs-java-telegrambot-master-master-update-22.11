package ebe.P_Judakov.s.JAVABOT.service.jpa;

import ebe.P_Judakov.s.JAVABOT.repository.interfaces.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.config.Task;

public class TaskService {

    @Autowired
    private TaskRepository repository;

    public void save(Task task) {
        repository.save(task);
    }

}
