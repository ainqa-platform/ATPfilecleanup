package com.atp.filecleanup;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.atp.commonfiles.SysLog;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class FileCleanupController {
	
	SysLog sysLog=new SysLog(FileCleanupController.class);
	
	@RequestMapping(path = "/GetValues", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String GetValues() {
		try {
			
			
			
		} catch (Exception e) {
			sysLog.error(e);
		}
		return "test";
	}
	
	public void doSomething() {
		System.out.println("test");
	}

}
