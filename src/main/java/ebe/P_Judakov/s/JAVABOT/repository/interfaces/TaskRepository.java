package ebe.P_Judakov.s.JAVABOT.repository.interfaces;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.config.Task;

public interface TaskRepository extends JpaRepository<Task, Integer> {
    }


