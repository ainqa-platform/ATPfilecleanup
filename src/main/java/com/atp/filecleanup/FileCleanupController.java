package com.atp.filecleanup;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atp.businesslogics.FileCleanupLogics;
import com.atp.commonfiles.JSON;
import com.atp.commonfiles.SysLog;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.google.gson.JsonObject;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class FileCleanupController {

	SysLog sysLog = new SysLog(FileCleanupController.class);

	@RequestMapping(path = "/listallfilesfromfolder", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String listallfilesfromfolder(@RequestBody Object inputObj) {
		try {
			FileCleanupLogics filecleanObj = new FileCleanupLogics();
			return filecleanObj.listallfilesfromfolder((JsonObject) JSON.DeserializeObject(JSON.Serialize(inputObj)));

		} catch (Exception e) {
			sysLog.error(e);
		}
		return "test";
	}

	@RequestMapping(path = "/deleteFilesFromFolder", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String deleteFilesFromFolder(@RequestBody Object inputObj) {
		try {
			FileCleanupLogics filecleanObj = new FileCleanupLogics();
			return filecleanObj.deleteFilesFromFolder((JsonObject) JSON.DeserializeObject(JSON.Serialize(inputObj)));

		} catch (Exception e) {
			sysLog.error(e);
		}
		return "test";
	}
	
	@RequestMapping(path = "/test_api", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String test_api(@RequestBody Object inputObj) {
		try {
			FileCleanupLogics fileclnObj=new FileCleanupLogics();
			fileclnObj.createTarFile(JSON.DeserializeObject(JSON.Serialize(inputObj)).getAsJsonObject().get("path").getAsString());
			
			return "";

		} catch (Exception e) {
			sysLog.error(e);
		}
		return "test";
	}
	
	@RequestMapping(path = "/decrypt_file", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String decrypt_file(@RequestParam("path") String path) {
		try {
			FileCleanupLogics fileclnObj=new FileCleanupLogics();
			fileclnObj.decryptFile(path);
			
			return "";

		} catch (Exception e) {
			sysLog.error(e);
		}
		return "test";
	}

}
