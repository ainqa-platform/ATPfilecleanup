/** 
 * Copyright QDB Platform to Present
 * All rights Reserved.
 * 
 * Contributors
 * 11-12-2020 PerumalRaja - JSON Class
 *
 */
package com.atp.commonfiles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// TODO: Auto-generated Javadoc
/**
 * The Class JSON.
 */
public class JSON {

//	static SysLog  _log = new SysLog(JSON.class);
	 
	/**
	 * Deserialize.
	 *
	 * @param jsonstr the jsonstr
	 * @param classname the classname
	 * @return the object
	 */
	public static JsonObject getBaseRequest(String db_name, String collection,String _key,JsonObject data)
    {
    	JsonObject jobj=null;
    	try
    	{
    		jobj = new JsonObject();
    		jobj.addProperty("db_name", db_name);
    		jobj.addProperty("collection_name", collection);
    		jobj.addProperty("_key", _key);
    		jobj.add("doc", data);
    	}
    	catch (Exception exp) {
    		
//    		_log.error(exp);
		}
    	return jobj;
    }
    
    
	public static JsonObject getBaseRequest(String db_name, String collection,String _key)
    {
    	return getBaseRequest(db_name,collection,_key,null);
    }
	
	public static Object Deserialize(String jsonstr,String classname)
	{
		try
		{
			return new Gson().fromJson(jsonstr, Class.forName(classname));
		}
		catch(Exception exp)
		{
			//log.error("JSON.Deserialize" + jsonstr);
			exp.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Deserialize object.
	 *
	 * @param jsonstr the jsonstr
	 * @return the json element
	 */
	public static JsonElement DeserializeObject(String jsonstr)
	{
		try
		{	
			return (JsonElement)JsonParser.parseString(jsonstr);
		}
		catch(Exception exp)
		{
			if(jsonstr!=null)
			   System.out.println("Deserialize Error Object "+ jsonstr);
			
			exp.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * Serialize.
	 *
	 * @param obj the obj
	 * @return the string
	 */
	public static String Serialize(Object obj)
	{
		try
		{
			return new Gson().toJson(obj);
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
		return new String("");
	}
	
	/**
	 * Serialize with null.
	 *
	 * @param obj the obj
	 * @return the string
	 */
	public static String SerializeWithNull(Object obj)
	{
		try
		{
		   Gson gson = new GsonBuilder().serializeNulls().create();			   
		   return  gson.toJson(obj);
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
		return new String("");
	}
	
	/**
	 * Serialize with ignore.
	 *
	 * @param obj the obj
	 * @return the string
	 */
	public static String SerializeWithIgnore(Object obj)
	{
		try
		{
		   Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();			   
		   return  gson.toJson(obj);
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
		return new String("");
	}
	
	/**
	 * Gets the value.
	 *
	 * @param json the json
	 * @param key the key
	 * @return the string
	 */
	public static String GetValue(String json,String key)
	{
		try
		{
			return DeserializeObject(json).getAsJsonObject().get(key).getAsString();
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
		return new String("");
	}
	
	/**
	 * Gets the object.
	 *
	 * @param json the json
	 * @param key the key
	 * @return the json object
	 */
	public static JsonObject GetObject(String json,String key)
	{
		try
		{
			return DeserializeObject(json).getAsJsonObject().getAsJsonObject(key);
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Gets the json array.
	 *
	 * @param json the json
	 * @param key the key
	 * @return the json array
	 */
	public static JsonArray GetJsonArray(String json,String key)
	{
		try
		{
			return DeserializeObject(json).getAsJsonObject().getAsJsonArray(key);
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * Gets the keypair.
	 *
	 * @param json the json
	 * @return the object[]
	 */
	/*
	 * public static Object[] GetKeypair(String json) { List<String> keys = new
	 * ArrayList(); try { Set<Entry<String, JsonElement>> flst = new
	 * JsonParser().parse(json).getAsJsonObject().entrySet(); for (Entry<String,
	 * JsonElement> entry : flst) keys.add(entry.getKey() + "="+
	 * entry.getValue().getAsString()); } catch(Exception exp) { throw exp; } return
	 * keys.toArray(); }
	 */
	
	
	
	

	
}