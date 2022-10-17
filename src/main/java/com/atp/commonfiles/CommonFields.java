package com.atp.commonfiles;

public class CommonFields {
	
	public static String idm_repo_name="IDM_Repository"; 
	public static String read_documents="read_documents";
	public static String idm_permission_management="IDM_PermissionManagement"; 
	public static String idm_permission_rolemapping="IDM_PermissionRoleMapping"; 
	public static String upsert_document="upsert_document"; 
	public static String person="Person"; 
	public static String client="client"; 
	public static String personPayload="{\"name\":[{\"use\":\"\",\"text\":\"RaviTest\",\"family\":\"\",\"given\":\"\",\"prefix\":\"\",\"suffix\":\"\",\"period\":[],\"id\":0}],\"telecom\":[{\"system\":\"email\",\"value\":\"rk@mail.com\",\"use\":\"\",\"rank\":\"\",\"period\":[],\"id\":0,\"valueprefix\":\"\"}],\"Id\":0}";
    public static String execute_aql="execute_aql";
    public static String casbin_policy="casbin_policy";
    public static String database_exists="database_exists";
    public static String soft_delete="soft_delete";
    public static String IDM_CONFIGURATION="idm_configuration";
    public static String QDM_OBJECT_CONFIG="QDMObjectConfig";
    public static String QDM_OBJECT="QDMObject";
    public static String CONFTYPE_QDMFILEUPLOAD="qdmfileupload";
    public static String FILE_AUDIT_LOGS="file_audit_logs";
    
    public enum QDMConfTypes{
    	qdmfileupload,
    	sourcefileupload,
    	collections
    }
    
    public enum QDMConffileFormat {
    	PNG,
    	JPG,
    	JPEG
    }
    
    public enum audit_type {
    	BACKUP,
    	RETENTION
    }
    
  }
