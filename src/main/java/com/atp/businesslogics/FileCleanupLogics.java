package com.atp.businesslogics;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import com.atp.commonfiles.CommonFunctions;
import com.atp.commonfiles.CommonObjMessages;
import com.atp.commonfiles.JSON;
import com.atp.commonfiles.SysLog;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class FileCleanupLogics {

	SysLog sysLog = new SysLog(FileCleanupLogics.class);

	public String deleteFilesFromFolder(JsonObject inputObj) {
		CommonFunctions common = new CommonFunctions();
		try {
			String path = JSON.GetValue(inputObj, "path");

			if (path.isEmpty()) {
				return CommonObjMessages.fieldEmptyResponse("path");
			}

			File[] lstFiles = new File(path).listFiles();

			for (File file : lstFiles) {
				common.closeFiles(Paths.get(file.toURI()).toString());
				Files.deleteIfExists(Paths.get(file.toURI()));
				boolean a = file.delete();
				System.out.print("test");
			}
			return CommonObjMessages.formSuccessResultObj("Files deleted successfully!");

		} catch (Exception e) {
			sysLog.error(e);
		}
		return "";
	}

	public String deleteFilesFromFolder(String path) {
		CommonFunctions common = new CommonFunctions();
		try {
			

			if (path.isEmpty()) {
				return CommonObjMessages.fieldEmptyResponse("path");
			}

			File[] lstFiles = new File(path).listFiles();

			for (File file : lstFiles) {
				common.closeFiles(Paths.get(file.toURI()).toString());
				Files.deleteIfExists(Paths.get(file.toURI()));
				boolean a = file.delete();
				System.out.print("test");
			}
			return CommonObjMessages.formSuccessResultObj("Files deleted successfully!");

		} catch (Exception e) {
			sysLog.error(e);
		}
		return "";
	}

	public String listallfilesfromfolder(JsonObject inputObj) {
		JsonArray resultArr = new JsonArray();
		try {
			String path = JSON.GetValue(inputObj, "path");

			if (path.isEmpty()) {
				return CommonObjMessages.fieldEmptyResponse("path");
			}

			File[] lstFiles = new File(path).listFiles();

			for (File file : lstFiles) {
				resultArr.add(file.getName());

			}

			return CommonObjMessages.formResultObj(resultArr);

		} catch (Exception e) {
			sysLog.error(e);
		}
		return "";
	}

	public void createTarFile(String sourceDir) {
		TarArchiveOutputStream tarOs = null;
		try {
			File source = new File(sourceDir);
			// Using input name to create output name
			FileOutputStream fos = new FileOutputStream(source.getAbsolutePath().concat(".tar.gz"));
			GzipCompressorOutputStream gos = new GzipCompressorOutputStream(fos);
			tarOs = new TarArchiveOutputStream(gos);
			addFilesToTarGZ(sourceDir, "", tarOs);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				tarOs.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void addFilesToTarGZ(String filePath, String parent, TarArchiveOutputStream tarArchive) throws IOException {
		File file = new File(filePath);
		// Create entry name relative to parent file path
		String entryName = parent + file.getName();
		// add tar ArchiveEntry
		tarArchive.putArchiveEntry(new TarArchiveEntry(file, entryName));
		if (file.isFile()) {
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			// Write file content to archive
			IOUtils.copy(bis, tarArchive);
			tarArchive.closeArchiveEntry();
			bis.close();
		} else if (file.isDirectory()) {
			// no need to copy any content since it is
			// a directory, just close the outputstream
			tarArchive.closeArchiveEntry();
			// for files in the directories
			for (File f : file.listFiles()) {
				// recursively call the method for all the subdirectories
				addFilesToTarGZ(f.getAbsolutePath(), entryName + File.separator, tarArchive);
			}
		}
	}
	
	public void decryptFile(String encrypted) {
		CommonFunctions common=new CommonFunctions();
		try {
			encrypted="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";
			common.AESdecryptFile(encrypted);
			
		} catch (Exception e) {
			sysLog.error(e);
		}
	}

}
