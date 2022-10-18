package com.atp.commonfiles;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CommonObjMessages {
	
	public static String fieldEmptyResponse(String fieldName) {
		JsonObject resultObj=new JsonObject();
		resultObj.addProperty("Code", ErrorCodes.warning);
		resultObj.addProperty("error", fieldName+": field should be empty or null!");
		return resultObj.toString();
		
	}
	
	public static String unExpectedError(String fieldName, String errorMsg) {
		JsonObject resultObj=new JsonObject();
		resultObj.addProperty("Code", ErrorCodes.warning);
		resultObj.addProperty("error", errorMsg);
		return resultObj.toString();
		
	}
	
	public static String fieldErrorWithMsg(String fieldName, String errorMsg) {
		JsonObject resultObj=new JsonObject();
		resultObj.addProperty("Code", ErrorCodes.warning);
		resultObj.addProperty("error", fieldName+": "+errorMsg+"");
		return resultObj.toString();
		
	}
	
	public static String formResultObj(JsonElement input) {
		JsonObject resultObj=new JsonObject();
		resultObj.addProperty("Code", ErrorCodes.success);
		resultObj.add("Result", input);
		return resultObj.toString();
		
	}
	public static String formSuccessResultObj(String inputMsg) {
		JsonObject resultObj=new JsonObject();
		resultObj.addProperty("Code", ErrorCodes.success);
		resultObj.addProperty("Result", inputMsg);
		return resultObj.toString();
		
	}

}
