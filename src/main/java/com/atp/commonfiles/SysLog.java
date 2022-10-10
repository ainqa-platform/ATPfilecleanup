/** 
 * Copyright QDB Platform to Present
 * All rights Reserved.
 * 
 * Contributors
 * 10-12-2020 PerumalRaja - SysLog Class
 *
 */
package com.atp.commonfiles;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
// TODO: Auto-generated Javadoc
/**
 * The Class SysLog.
 */
public class SysLog
{
	
	/** The log. */
	private Logger _log;
	
	/**
	 * The Class LogTypes.
	 */
	
	
	/**
	 * Instantiates a new sys log.
	 *
	 * @param objclass the objclass
	 */
	public SysLog(Class<?> objclass)
	{
		_log = LogManager.getLogger(objclass);
		
	}
	public void error(Exception exp) {
		_log.error(buildmessage(exp,"error"));
	}
	public void error(String message) {
		_log.error(buildmessage(message,"info"));
		
	}
	public void info(String message) {
		_log.info(message);
		
	}
	public void info(Exception exp) {
		_log.info(buildmessage(exp,"info"));
	}
	private String buildmessage(Exception exp,String type)
	{
		Writer writer = new StringWriter();
		String result = new String();
		PrintWriter printWriter = null;
		try
		{
			//result = new SimpleDateFormat("yyyyMMdd hh:mm:ss").format(new Date()) + " " + type + " " + exp.getClass().getName() + " - [" + type + "  ]" + "\r\n";
			result = result + "Message: " + exp.getMessage() + "\r\n";
			
		    printWriter = new PrintWriter(writer);
			exp.printStackTrace(printWriter);
			result = result + "StackTrace" + "\r\n" + writer.toString() + "\r\n\r\n";
		}
		catch(Exception ex)
		{
			
		}
		finally
		{
			printWriter = null;
			writer = null;
		}
		return result;
	}
	
	private String buildmessage(String exp,String type)
	{
//		Writer writer = new StringWriter();
		String result = new String();
//		PrintWriter printWriter = null;
		try
		{
			result = new SimpleDateFormat("yyyyMMdd hh:mm:ss").format(new Date()) + " " + type + " " + exp.getClass().getName() + " - [" + type + "  ]" + "\r\n";
			result = result + "Message: " + exp + "\r\n";
//			
//		    printWriter = new PrintWriter(writer);
//			//exp.printStackTrace(printWriter);
//			result = result + "StackTrace" + "\r\n" + writer.toString() + "\r\n\r\n";
		}
		catch(Exception ex)
		{
			
		}
		finally
		{
//			printWriter = null;
//			writer = null;
		}
		return result;
	}
	
}
