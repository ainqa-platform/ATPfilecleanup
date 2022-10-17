package com.atp.businesslogics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.atp.commonfiles.CommonFields;
import com.atp.commonfiles.CommonFields.QDMConfTypes;
import com.atp.commonfiles.CommonFields.audit_type;
import com.atp.commonfiles.CommonFunctions;
import com.atp.commonfiles.Settings;
import com.atp.commonfiles.SysLog;
import com.atp.models.IDMConfiguration;
import com.atp.models.QDMObjectConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ScheduleLogic {

	SysLog sysLog = new SysLog(ScheduleLogic.class);
	final ObjectMapper objectMapper = new ObjectMapper();

	String gbarangoApi = "";
	String gbmetadataDbName = "";

	public ScheduleLogic() {
		try {
			gbarangoApi = Settings.get("arangodbapi");
			gbmetadataDbName = Settings.get("metadatadbname");

		} catch (Exception e) {
			sysLog.error(e);
		}

	}

	public void ScheduleTask() {

		Runnable task = () -> readQdmConfig();
		Runnable task1 = () -> fileCleanup();
		ScheduledExecutorService executor = Executors
				.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

		executor.scheduleAtFixedRate(task, 0, 15, TimeUnit.SECONDS);
		//executor.scheduleAtFixedRate(task1, 0, 15, TimeUnit.SECONDS);
	}

	public void fileCleanup() {
		CommonFunctions common = new CommonFunctions();
		try {
			System.out.println("task1");

			String readApi = common.ConcatStrings(gbarangoApi, CommonFields.read_documents);

			String filter = CommonFields.IDM_CONFIGURATION + ".active=='Y'";

			JsonObject readconfObj = common.ReadDocuments(gbmetadataDbName, CommonFields.IDM_CONFIGURATION, filter,
					CommonFields.IDM_CONFIGURATION, readApi);

			String json = readconfObj.get("result").getAsJsonArray().toString();

			List<IDMConfiguration> idmconfList = objectMapper.readValue(json,
					new TypeReference<List<IDMConfiguration>>() {
					});

			for (IDMConfiguration idmConfigurationObj : idmconfList) {

				String sourcePath = idmConfigurationObj.getSourcePath();

				int retentionPeriod = Integer.parseInt(idmConfigurationObj.getRetentionPeriodInDays());

				if (retentionPeriod > 0 && sourcePath != null) {

					File[] listFiles = new File(sourcePath).listFiles();

					for (File file : listFiles) {

						Path path = file.toPath();
						BasicFileAttributes file_att = Files.readAttributes(path, BasicFileAttributes.class);
						FileTime filecreationTime = file_att.creationTime();
						long filecreationmillSec = filecreationTime.toMillis();

						LocalDate currentDate = LocalDate.now();

						long currentDateMilliSec = common.getMillSecFromDate(currentDate);

						long diffMillSec = common.getDateDiffFromMilliSec(currentDateMilliSec, filecreationmillSec);

						if (diffMillSec >= retentionPeriod) {
							// fileclnLogObj.deleteFilesFromFolder(inputObj);
							try {
								AuditInsert(file, filecreationTime,audit_type.RETENTION.toString());
								Files.deleteIfExists(Paths.get(path.toUri()));
							} catch (Exception e) {
								System.out.println("Invalid permissions.");
							}

							System.out.println("Deletion successful.");
						}

					}

				}
			}

		} catch (Exception e) {
			sysLog.error(e);
		}
	}

	public void AuditInsert(File file, FileTime filecreationTime,String processType) {
		CommonFunctions common = new CommonFunctions();
		try {
			String metadataDbName = Settings.get("metadatadbname");
			String upsertApi = gbarangoApi + "" + CommonFields.upsert_document;
			JsonObject docObj = new JsonObject();
			docObj.addProperty("fileName", file.getName());
			docObj.addProperty("fileCreationTime", filecreationTime.toMillis());
			docObj.addProperty("fileSize", (file.length() / 1024) + "kb");
//			FileInputStream fileStream = new FileInputStream(file);
//			docObj.addProperty("filebyte", common.AESencrypt(file));
			docObj.addProperty("fileCheckSum", common.convertFiletoMD5Format(file));
			docObj.addProperty("appication", "fileCleanupService");
			docObj.addProperty("processType", processType);
			common.UpsertDocuments(metadataDbName, CommonFields.FILE_AUDIT_LOGS, "", docObj, upsertApi, "false", "",
					metadataDbName);

		} catch (Exception e) {
			sysLog.error(e);
		}

	}

	public void readQdmConfig() {
		System.out.println("task");
		CommonFunctions common = new CommonFunctions();
		try {

			String readApi = common.ConcatStrings(gbarangoApi, CommonFields.read_documents);

			String filter = CommonFields.IDM_CONFIGURATION + ".active=='Y'";

			JsonObject readconfObj = common.ReadDocuments(gbmetadataDbName, CommonFields.IDM_CONFIGURATION, filter,
					CommonFields.IDM_CONFIGURATION, readApi);

			String json = readconfObj.get("result").getAsJsonArray().toString();

			List<IDMConfiguration> idmconfList = objectMapper.readValue(json,
					new TypeReference<List<IDMConfiguration>>() {
					});

			for (IDMConfiguration idmConfigurationObj : idmconfList) {

				String dbName = idmConfigurationObj.getDbName();

				if (!dbName.isEmpty()) {

					String confType = idmConfigurationObj.getConfType();

					if (confType.equals(QDMConfTypes.qdmfileupload.toString())) {

						String qdmobjfilter = CommonFields.QDM_OBJECT_CONFIG + ".tenantId=='"
								+ idmConfigurationObj.getTenantId() + "'";

						JsonObject readqdmobjconfObj = common.ReadDocuments(dbName, CommonFields.QDM_OBJECT_CONFIG,
								qdmobjfilter, CommonFields.QDM_OBJECT_CONFIG, readApi);

						JsonArray readqdmobjconfrsltArr = readqdmobjconfObj.get("result").getAsJsonArray();

						List<QDMObjectConfig> qdmconfList = objectMapper.readValue(readqdmobjconfrsltArr.toString(),
								new TypeReference<List<QDMObjectConfig>>() {
								});
						for (QDMObjectConfig qdmconfObj : qdmconfList) {

							String fullPath = qdmconfObj.path + "/" + qdmconfObj.getDirectoryname();
							String archievePath = idmConfigurationObj.getArchievePath();
							String archievePeriodInDays = idmConfigurationObj.getArchievePeriodInDays();
							List<String> fileFormatLst = idmConfigurationObj.getFileFormat();
							List<String> fileNameLst = idmConfigurationObj.getFileName();

							fileBackupLogic(fullPath, archievePath, archievePeriodInDays, fileFormatLst, fileNameLst,
									idmConfigurationObj);

						}
					}

					else if (confType.equals(QDMConfTypes.sourcefileupload.toString())) {
						String fullPath = idmConfigurationObj.getSourcePath();
						String archievePath = idmConfigurationObj.getArchievePath();
						String archievePeriodInDays = idmConfigurationObj.getArchievePeriodInDays();
						List<String> fileFormatLst = idmConfigurationObj.getFileFormat();
						List<String> fileNameLst = idmConfigurationObj.getFileName();

						fileBackupLogic(fullPath, archievePath, archievePeriodInDays, fileFormatLst, fileNameLst,
								idmConfigurationObj);
					}

					else if (confType.equals(QDMConfTypes.collections.toString())) {
						String fullPath = "src/main/resources/jsonfiles";
						String archievePath = idmConfigurationObj.getArchievePath();
						String archievePeriodInDays = idmConfigurationObj.getArchievePeriodInDays();

						List<String> collections = idmConfigurationObj.getCollections();
                       List<JsonObject> filesContentLst=new ArrayList<>();
						for (String collectionName : collections) {

							JsonObject readcollectObj = common.ReadDocuments(dbName, collectionName, "", collectionName,
									readApi);
							JsonArray readcollectrsltArr = readcollectObj.has("result")
									? readcollectObj.get("result").getAsJsonArray()
									: null;
							

							JsonObject filecontObj = new JsonObject();

							filecontObj.addProperty("fileName", collectionName + ".json");
							filecontObj.addProperty("fileContent", readcollectrsltArr.toString());
							filesContentLst.add(filecontObj);

							

						}

						List<String> empttList = Collections.emptyList();

						fileBackupLogicForCollections(fullPath, archievePath, archievePeriodInDays, empttList,
								empttList, idmConfigurationObj,filesContentLst);

					}

				}

				System.out.print("test");
			}

			System.out.print("test");

		} catch (Exception e) {
			sysLog.error(e);
		}

	}

	public boolean isBackupExists(String archievePath) {
		try {

		File archieveFolder = new File(archievePath);

		LocalDate localDate = LocalDate.now();

		String[] list = archieveFolder.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {

				return name.contains("backup_" + localDate + "");
			}
		});

		if (list.length > 0) {
			return true;
		}
		}
		catch (Exception e) {
			sysLog.error(e);
		}
		return false;

	}

	public long getLastCreationFileinPath(String path, String fileName) {

		List<Long> timeLst = new ArrayList<Long>();
		try {
			File files = new File(path);

			// File[] fileArr = files.listFiles();

			FilenameFilter filter = new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					// TODO Auto-generated method stub
					return name.contains(fileName);
				}
			};

			File[] fileArr = files.listFiles(filter);

			for (int i = 0; i < fileArr.length; i++) {
				File file = fileArr[i];

				Path filepath = file.toPath();
				BasicFileAttributes file_att = Files.readAttributes(filepath, BasicFileAttributes.class);
				FileTime creationTime = file_att.creationTime();
				timeLst.add(creationTime.toMillis());
			}

			long max = timeLst.stream().mapToLong(v -> v).max().orElseThrow(NoSuchElementException::new);

			return max;

		} catch (Exception e) {
			sysLog.error(e);
		}

		return 0;
	}

	public void fileBackupLogic(String filePath, String backupPath, String archievePeriodInDays,
			List<String> fileFormatLst, List<String> fileNameLst, IDMConfiguration idmConfigurationObj) {

		CommonFunctions common = new CommonFunctions();
		FileCleanupLogics fileclnObj=new FileCleanupLogics();

		try {
			LocalDate localDate = LocalDate.now();
			File qdmfileFolder = new File(filePath);
			Path path = qdmfileFolder.toPath();
			BasicFileAttributes file_att = Files.readAttributes(path, BasicFileAttributes.class);
			FileTime creationTime = file_att.creationTime();
			long filecreatemilliSec = creationTime.toMillis();
			long filecreationDay = common.getDateFromMillisec(filecreatemilliSec, "dd");

			int currentDate = localDate.getDayOfMonth();
			long currentDateInMillSec = localDate.toEpochSecond(LocalTime.now(), ZoneOffset.UTC) * 1000;
			if (currentDate > filecreationDay && common.getDateDiffFromMilliSec(currentDateInMillSec,
					filecreatemilliSec) >= Long.parseLong(archievePeriodInDays)) {
				if (common.getDateDiffFromMilliSec(currentDateInMillSec, getLastCreationFileinPath(backupPath,
						idmConfigurationObj.getOutputBackupFileName())) >= Long.parseLong(archievePeriodInDays)) {
					if (!isBackupExists(backupPath)) {
						File[] a = qdmfileFolder.listFiles();
						if(idmConfigurationObj.getArchieveType().equals("remote")) {
						String remotePath="src/main/resources/compressfiles";
						common.zip(a, remotePath, fileFormatLst, fileNameLst, idmConfigurationObj);	
						readAndUploadToRemotePath(remotePath);
						JsonObject clnObj=new JsonObject();
						clnObj.addProperty("path", remotePath);
						fileclnObj.deleteFilesFromFolder(clnObj);
						
						}
						else {
						common.zip(a, backupPath, fileFormatLst, fileNameLst, idmConfigurationObj);
						}
					}
				}
			}

		} catch (Exception e) {
			sysLog.error(e);
		}
	}
	
	public void readAndUploadToRemotePath(String remotePath) {
		
		try {
			
			File [] fileLst=new File(remotePath).listFiles();
			
		} catch (Exception e) {
			sysLog.error(e);
		}
		
	}

	public void fileBackupLogicForCollections(String filePath, String backupPath, String archievePeriodInDays,
			List<String> fileFormatLst, List<String> fileNameLst, IDMConfiguration idmConfigurationObj,
			List<JsonObject> filesContentLst) {

		CommonFunctions common = new CommonFunctions();
		FileCleanupLogics fileclnObj=new FileCleanupLogics(); 

		try {

			LocalDate localDate = LocalDate.now();

			long currentDateInMillSec = localDate.toEpochSecond(LocalTime.now(), ZoneOffset.UTC) * 1000;

			if (common.getDateDiffFromMilliSec(currentDateInMillSec, getLastCreationFileinPath(backupPath,
					idmConfigurationObj.getOutputBackupFileName())) >= Long.parseLong(archievePeriodInDays)) {
				if (!isBackupExists(backupPath)) {

					if (filesContentLst != null && filesContentLst.size() > 0) {
						for (JsonObject fileObj : filesContentLst) {

							String fileName = fileObj.get("fileName").getAsString();

							String fileContent = fileObj.get("fileContent").getAsString();
							FileWriter file = new FileWriter(filePath + "/" + fileName);
							file.write(fileContent);
							file.flush();
							file.close();
						}
					}

					File qdmfileFolder = new File(filePath);
					File[] a = qdmfileFolder.listFiles();
					common.zip(a, backupPath, fileFormatLst, fileNameLst, idmConfigurationObj);
					FileCleanupLogics filecleanLog = new FileCleanupLogics();
					JsonObject delfileObj = new JsonObject();
					delfileObj.addProperty("path", qdmfileFolder.getAbsolutePath());
					filecleanLog.deleteFilesFromFolder(delfileObj);
				}
			}
			else {
				common.closeFiles("src/main/resources/jsonfiles");
				fileclnObj.deleteFilesFromFolder("src/main/resources/jsonfiles");
			}

		} catch (Exception e) {
			sysLog.error(e);
		}
	}

}
