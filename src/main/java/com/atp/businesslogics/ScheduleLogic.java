package com.atp.businesslogics;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
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
		ScheduledExecutorService executor = Executors
				.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

		executor.scheduleAtFixedRate(task, 1, 15, TimeUnit.SECONDS);
	}

	public void readQdmConfig() {
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
					} else if (confType.equals(QDMConfTypes.collections.toString())) {
						String fullPath = "src/main/resources/jsonfiles";
						String archievePath = idmConfigurationObj.getArchievePath();
						String archievePeriodInDays = idmConfigurationObj.getArchievePeriodInDays();

						List<String> collections = idmConfigurationObj.getCollections();

						for (String collectionName : collections) {

							JsonObject readcollectObj = common.ReadDocuments(dbName, collectionName, "", collectionName,
									readApi);
							JsonArray readcollectrsltArr = readcollectObj.has("result")
									? readcollectObj.get("result").getAsJsonArray()
									: null;

							FileWriter file = new FileWriter(fullPath + "/" + collectionName + ".json");
							file.write(readcollectrsltArr.toString());
							file.close();

						}

						List<String> empttList = Collections.emptyList();

						fileBackupLogicForCollections(fullPath, archievePath, archievePeriodInDays, empttList,
								empttList, idmConfigurationObj);

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
			// TODO: handle exception
		}

		return 0;
	}

	public void fileBackupLogic(String filePath, String backupPath, String archievePeriodInDays,
			List<String> fileFormatLst, List<String> fileNameLst, IDMConfiguration idmConfigurationObj) {

		CommonFunctions common = new CommonFunctions();

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
			if (currentDate > filecreationDay && getDateDiffFromMilliSec(currentDateInMillSec,
					filecreatemilliSec) >= Long.parseLong(archievePeriodInDays)) {
				if (getDateDiffFromMilliSec(currentDateInMillSec, getLastCreationFileinPath(backupPath,
						idmConfigurationObj.getOutputBackupFileName())) >= Long.parseLong(archievePeriodInDays)) {
					if (!isBackupExists(backupPath)) {
						File[] a = qdmfileFolder.listFiles();
						common.zip(a, backupPath, fileFormatLst, fileNameLst, idmConfigurationObj);
					}
				}
			}

		} catch (Exception e) {
			sysLog.error(e);
		}
	}

	public void fileBackupLogicForCollections(String filePath, String backupPath, String archievePeriodInDays,
			List<String> fileFormatLst, List<String> fileNameLst, IDMConfiguration idmConfigurationObj) {

		CommonFunctions common = new CommonFunctions();

		try {
			LocalDate localDate = LocalDate.now();

			File qdmfileFolder = new File(filePath);
			long currentDateInMillSec = localDate.toEpochSecond(LocalTime.now(), ZoneOffset.UTC) * 1000;

			if (getDateDiffFromMilliSec(currentDateInMillSec, getLastCreationFileinPath(backupPath,
					idmConfigurationObj.getOutputBackupFileName())) >= Long.parseLong(archievePeriodInDays)) {
				if (!isBackupExists(backupPath)) {
					File[] a = qdmfileFolder.listFiles();
					common.zip(a, backupPath, fileFormatLst, fileNameLst, idmConfigurationObj);
					for (File file : a) {
						File abfile=new File(file.getAbsolutePath());
						boolean as=abfile.delete();
						System.out.print("test");
						
					}
				}
			}

		} catch (Exception e) {
			sysLog.error(e);
		}
	}

	public long getDateDiffFromMilliSec(long from, long to) {

		long diff = from - to;

		long diffdays = TimeUnit.MILLISECONDS.toDays(diff);

		return diffdays;

	}

}
