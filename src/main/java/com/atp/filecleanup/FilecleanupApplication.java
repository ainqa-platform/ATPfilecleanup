package com.atp.filecleanup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.atp.businesslogics.ScheduleLogic;

@SpringBootApplication
public class FilecleanupApplication {

	public static void main(String[] args) {
		ScheduleLogic schLog=new ScheduleLogic();
		schLog.ScheduleTask();
		SpringApplication.run(FilecleanupApplication.class, args);
	}

}
