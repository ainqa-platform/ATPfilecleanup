package com.atp.commonfiles;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.directory.BasicAttributes;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.springframework.stereotype.Component;

import com.atp.businesslogics.ScheduleLogic;
import com.atp.commonfiles.CommonFields.audit_type;
import com.atp.models.IDMConfiguration;
import com.github.mervick.aes_everywhere.Aes256;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component("common")
public class CommonFunctions {
	SysLog _log = new SysLog(CommonFunctions.class);
	private static final okhttp3.MediaType MediaTypeJSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
	String arangoAdapterApi = "";
	private static final int BUFFER_SIZE = 4096;

	public CommonFunctions() {
		try {

			arangoAdapterApi = Settings.get("arangodbapi");
		} catch (Exception e) {
			_log.error(e);
		}
	}

	public String callRestAPI(String obj, String api) {
		String okHttpOutput = "";
		try {
			RequestBody body = RequestBody.create(MediaTypeJSON, obj.toString());
			Request request = new Request.Builder().url(api).post(body).build();
			OkHttpClient client = getClientConnection();
			Response response = client.newCall(request).execute();
			if (String.valueOf(response.code()).equals("504")) {
				okHttpOutput = "504 gateway timeout error";
			} else {
				okHttpOutput = response.body().string();
			}
		} catch (Exception e) {
			okHttpOutput = e.toString();
		}
		System.out.print("okHttpOutput : " + okHttpOutput);
		return okHttpOutput;
	}

	public JsonObject ReadDocuments(String dbName, String Entity, String filter, String returnflds, String api) {
		try {
			JsonObject readObj = new JsonObject();
			readObj.addProperty("db_name", dbName);
			readObj.addProperty("entity", Entity);
			readObj.addProperty("filter", filter);
			readObj.addProperty("return_fields", returnflds);
			String resp = callRestAPI(readObj.toString(), api);
			if (resp.contains("IllegalArgumentException") || resp.contains("UnknownHostException")) {
				JsonObject resultObj = new JsonObject();
				resultObj.addProperty("Code", ErrorCodes.warning);
				resultObj.addProperty("error", "not a valid url!");
				return resultObj;
			}
			JsonElement respEle = JsonParser.parseString(resp);
			JsonObject respObj = respEle.getAsJsonObject();
			return respObj;

		}

		catch (Exception e) {
			 _log.error(e);
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("Code", ErrorCodes.warning);
			resultObj.addProperty("error", "not a valid url!");
			return resultObj;
		}

	}

	public JsonObject softDelete(String dbName, String Entity, String filter, String api, String metadataDbName) {
		try {
			JsonObject resultObj = new JsonObject();
			CommonFunctions common = new CommonFunctions();
			JsonObject softDeleteObj = new JsonObject();
			softDeleteObj.addProperty("db_name", dbName);
			softDeleteObj.addProperty("entity", Entity);
			JsonObject readObj = ReadDocuments(dbName, Entity, filter, Entity,
					common.ConcatStrings(arangoAdapterApi, CommonFields.read_documents));
			JsonArray readArr = readObj.has("result") ? readObj.get("result").getAsJsonArray() : new JsonArray();
			if (readArr.size() == 0) {
				resultObj.addProperty("Code", ErrorCodes.warning);
				resultObj.addProperty("error", "Filter value does not exists!");
				return resultObj;
			} else {
				softDeleteObj.addProperty("filter", filter);
			}
			String resp = callRestAPI(softDeleteObj.toString(), api);
			if (resp.contains("IllegalArgumentException") || resp.contains("UnknownHostException")) {

				resultObj.addProperty("Code", ErrorCodes.warning);
				resultObj.addProperty("error", "not a valid url!");
				return resultObj;
			}
			JsonElement respEle = JsonParser.parseString(resp);
			JsonObject respObj = respEle.getAsJsonObject();
			return respObj;
		} catch (Exception e) {
			// _log.error(e);
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("Code", ErrorCodes.warning);
			resultObj.addProperty("error", "not a valid url!");
			return resultObj;
		}
	}

	public JsonObject ReadDocuments(JsonObject inputObj, String api) {
		try {

			String resp = callRestAPI(inputObj.toString(), api);
			if (resp.contains("IllegalArgumentException") || resp.contains("UnknownHostException")) {
				JsonObject resultObj = new JsonObject();
				resultObj.addProperty("Code", ErrorCodes.warning);
				resultObj.addProperty("error", "not a valid url!");
				return resultObj;
			}
			JsonElement respEle = JsonParser.parseString(resp);
			JsonObject respObj = respEle.getAsJsonObject();
			return respObj;

		}

		catch (Exception e) {
			// _log.error(e);
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("Code", ErrorCodes.warning);
			resultObj.addProperty("error", "not a valid url!");
			return resultObj;
		}

	}

	public String ConcatStrings(String s1, String s2) {
		return s1.concat(s2);
	}

	public JsonObject UpsertDocuments(String dbName, String Entity, String filter, JsonObject docObj, String api,
			String isMetadata, String metadataId, String metadataDbName) {
		try {
			CommonFunctions common = new CommonFunctions();
			JsonArray upsertArr = new JsonArray();
			JsonObject upsertObj = new JsonObject();
			upsertObj.addProperty("db_name", dbName);
			upsertObj.addProperty("entity", Entity);
			upsertObj.addProperty("is_metadata", isMetadata);
			if (!filter.isEmpty()) {
				JsonObject readObj = ReadDocuments(dbName, Entity, filter, Entity,
						common.ConcatStrings(arangoAdapterApi, CommonFields.read_documents));
				JsonArray readArr = readObj.has("result") ? readObj.get("result").getAsJsonArray() : new JsonArray();
				if (readArr.size() > 0) {
					JsonObject filterObj = new JsonObject();
					filterObj.addProperty("_key", readArr.get(0).getAsJsonObject().get("_key").getAsString());
					upsertObj.add("filter", filterObj);
				}
			}

			upsertObj.addProperty("is_metadata", isMetadata);
			if (!metadataId.isEmpty()) {
				upsertObj.addProperty("metadataId", metadataId);
				upsertObj.addProperty("metadata_dbname", metadataDbName);
			}
			upsertObj.add("doc", docObj);
			upsertArr.add(upsertObj);
			String resp = callRestAPI(upsertArr.toString(), api);
			if (resp.contains("IllegalArgumentException") || resp.contains("UnknownHostException")) {
				JsonObject resultObj = new JsonObject();
				resultObj.addProperty("Code", ErrorCodes.warning);
				resultObj.addProperty("error", "not a valid url!");
				return resultObj;
			}
			JsonElement respEle = JsonParser.parseString(resp);
			JsonObject respObj = respEle.getAsJsonObject();
			return respObj;

		}

		catch (Exception e) {
			 _log.error(e);
			JsonObject resultObj = new JsonObject();
			resultObj.addProperty("Code", ErrorCodes.warning);
			resultObj.addProperty("error", "not a valid url!");
			return resultObj;
		}

	}

	public JsonArray ExecuteAql(JsonObject inputObj, String api) {
		JsonArray respArr = new JsonArray();
		try {

			String resp = callRestAPI(inputObj.toString(), api);
			if (resp.contains("IllegalArgumentException") || resp.contains("UnknownHostException")) {
				return respArr;
			}
			JsonElement respEle = JsonParser.parseString(resp);
			respArr = respEle.getAsJsonArray();
			return respArr;

		}

		catch (Exception e) {
			// _log.error(e);
			return respArr;
		}

	}

	public boolean DatabaseExists(String dbName, String api) {
		String result = "";
		try {
			JsonObject inputObj = new JsonObject();
			inputObj.addProperty("db_name", dbName);
			String resp = callRestAPI(inputObj.toString(), api);
			JsonElement respEle = JsonParser.parseString(resp);
			JsonObject respObj = respEle.getAsJsonObject();
			if (resp.contains("IllegalArgumentException") || resp.contains("UnknownHostException")) {
				return false;
			}
			result = respObj.has("result") ? respObj.get("result").getAsString() : "false";
			if (result.equals("true")) {
				return true;
			} else {
				return false;
			}
		}

		catch (Exception e) {
			// _log.error(e);
			return false;
		}

	}

	public String MD5encrypt(String strtoEncrypt) {
		try {
			// Create MessageDigest instance for MD5
			MessageDigest md = MessageDigest.getInstance("MD5");
			// Add password bytes to digest
			md.update(strtoEncrypt.getBytes());
			// Get the hash's bytes
			byte[] bytes = md.digest();
			// This bytes[] has bytes in decimal format;
			// Convert it to hexadecimal format
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < bytes.length; i++) {
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			// Get complete hashed password in hex format
			return sb.toString();
		} catch (Exception e) {
			_log.error(e.getMessage());
		}

		return "";
	}

	private KeyStore readKeyStore() {
		try {
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			final String SSL_TRUSTSTORE = "/keyclkkpj.truststore";

			final String SSL_TRUSTSTORE_PASSWORD = Settings.get("cluster_truststore_password");
			// get user password and file input stream
			// char[] password = Settings.get("cluster_truststore_password");

			try {
				ks.load(this.getClass().getResourceAsStream(SSL_TRUSTSTORE), SSL_TRUSTSTORE_PASSWORD.toCharArray());
			} finally {

			}
			return ks;
		} catch (Exception e) {
			_log.error(e);
		}
		return null;
	}

	public OkHttpClient getClientConnection() {
		OkHttpClient client = null;
		try {
			String enableSsl = Settings.get("enablessl");

			if (enableSsl != null && enableSsl.equals("true")) {
				TrustManagerFactory trustManagerFactory = TrustManagerFactory
						.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				trustManagerFactory.init(readKeyStore());
				TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
				if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
					throw new IllegalStateException(
							"Unexpected default trust managers:" + Arrays.toString(trustManagers));
				}
				X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, new TrustManager[] { trustManager }, null);
				SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
				client = new OkHttpClient.Builder().sslSocketFactory(sslSocketFactory, trustManager)
						.connectTimeout(60, TimeUnit.MINUTES).writeTimeout(60, TimeUnit.MINUTES)
						.readTimeout(60, TimeUnit.MINUTES).build();

			} else {
				client = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.MINUTES)
						.writeTimeout(60, TimeUnit.MINUTES).readTimeout(60, TimeUnit.MINUTES)

						.build();
			}
		} catch (Exception e) {
			_log.error(e);
		}
		return client;
	}

	public Object removeNullValue(Object objVal, Class<Object> clstype) {
		clstype.getClass();
		return "";

	}

	public Object removeNullValue(Object collections) {
		if (collections == null) {

			return List.of();
		}
		return collections;
	}

	public void zip(List<File> listFiles, String destZipFile, IDMConfiguration idmConfigurationObj)
			throws FileNotFoundException, IOException {
		
		String osType=idmConfigurationObj.getOsType();
		String compressionType=idmConfigurationObj.getCompressionType();
		String fileFormat=".zip";
		
		if(osType.equals("windows") && compressionType.equals("rar")) {
			fileFormat=".rar";
		}
		else if(osType.equals("linux") && compressionType.equals("tar")) {
			fileFormat=".tar.gz";
		}
		else if(osType.equals("linux") && compressionType.equals("7z")) {
			fileFormat=".7z";
		}
			
		
		
		LocalDate localDate = LocalDate.now();
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
				destZipFile + "/" + idmConfigurationObj.getOutputBackupFileName() + "_" + localDate +fileFormat));
		for (File file : listFiles) {
			if (file.isDirectory()) {
				zipDirectory(file, file.getName(), zos);
			} else {
				zipFile(file, zos);
			}
		}
		ScheduleLogic schlogObj=new ScheduleLogic();
		File file=new File(destZipFile + "/" + idmConfigurationObj.getOutputBackupFileName() + "_" + localDate +fileFormat);
		
		BasicFileAttributes file_att = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
		schlogObj.AuditInsert(file, file_att.creationTime(),audit_type.BACKUP.toString());
		zos.flush();
		zos.close();
	}

	private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos)
			throws FileNotFoundException, IOException {
		for (File file : folder.listFiles()) {
			if (file.isDirectory()) {
				zipDirectory(file, parentFolder + "/" + file.getName(), zos);
				continue;
			}
			
			zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
			long bytesRead = 0;
			byte[] bytesIn = new byte[BUFFER_SIZE];
			int read = 0;
			while ((read = bis.read(bytesIn)) != -1) {
				zos.write(bytesIn, 0, read);
				bytesRead += read;
			}
			zos.closeEntry();
		}
	}

	public void zip(File[] files, String destZipFile, List<String> fileFormatLst, List<String> fileNameLst,
			IDMConfiguration idmConfigurationObj) throws FileNotFoundException, IOException {
		List<File> listFiles = new ArrayList<File>();
		CommonFunctions common = new CommonFunctions();

		if (fileFormatLst.size() == 0 && fileNameLst.size() == 0) {
			for (int i = 0; i < files.length; i++) {
				listFiles.add(files[i]);
			}
		} else if (fileNameLst.size() > 0) {

			listFiles = common.getFilesSelectedFileNames(files, fileNameLst);

		} else if (fileFormatLst.size() > 0) {

			listFiles = common.getFilesSelectedFormat(files, fileFormatLst);
		}
		if (listFiles.size() > 0) {
			zip(listFiles, destZipFile, idmConfigurationObj);
		}
	}

	public List<File> getFilesSelectedFormat(File[] files, List<String> fileFormatLst)
			throws FileNotFoundException, IOException {
		List<File> listFiles = new ArrayList<File>();
		for (int i = 0; i < files.length; i++) {

			File file = files[i];
			String fileName = file.toString();
			int index = fileName.lastIndexOf('.');
			if (index > 0) {
				String extension = "." + fileName.substring(index + 1);

				for (String fileExtension : fileFormatLst) {
					String fileExtwithDot = "." + fileExtension;
					if (fileExtwithDot.toUpperCase().equals(extension.toUpperCase())) {
						listFiles.add(files[i]);
					}
				}

			}

		}
		return listFiles;
	}

	public List<File> getFilesSelectedFileNames(File[] files, List<String> fileNameLst)
			throws FileNotFoundException, IOException {
		List<File> listFiles = new ArrayList<File>();
		for (int i = 0; i < files.length; i++) {

			File file = files[i];

			String fileNameWithExtension = file.getName();

			String fileNmae = fileNameWithExtension.split("\\.")[0];

			if (fileNameLst.contains(fileNmae)) {
				listFiles.add(file);
			}

		}
		return listFiles;
	}

	private void zipFile(File file, ZipOutputStream zos) throws FileNotFoundException, IOException {
		zos.putNextEntry(new ZipEntry(file.getName()));
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		long bytesRead = 0;
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = bis.read(bytesIn)) != -1) {
			zos.write(bytesIn, 0, read);
			bytesRead += read;
		}
		zos.closeEntry();
	}

	public String getDateFromMillisec(long milliSec) {
		// Creating date format
		DateFormat simple = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

		// Creating date from milliseconds
		// using Date() constructor
		Date result = new Date(milliSec);

		// Formatting Date according to the
		// given format
		return simple.format(result);
	}

	public long getDateFromMillisec(long milliSec, String type) {

		// Creating date format
		DateFormat simple = new SimpleDateFormat(type);

		// Creating date from milliseconds
		// using Date() constructor
		Date result = new Date(milliSec);

		// Formatting Date according to the
		// given format
		return Long.parseLong(simple.format(result));
	}
	
	public long getDateDiffFromMilliSec(long from, long to) {

		long diff = from - to;

		long diffdays = TimeUnit.MILLISECONDS.toDays(diff);

		return diffdays;

	}
	
	public long getMillSecFromDate(LocalDate date) {
		
		return date.toEpochSecond(LocalTime.now(), ZoneOffset.UTC) * 1000;
	}
	
	public String convertFiletoMD5Format(File file) {
		try {
		MessageDigest md5Digest = MessageDigest.getInstance("MD5");
		String checksum = checksum(md5Digest, file);
		return checksum;
		}catch (Exception e) {
			
		}

	return "";	
	}
	
	
	
	private static String checksum(MessageDigest digest, File file) throws IOException {

		FileInputStream fis = new FileInputStream(file);

		byte[] byteArray = new byte[1024];
		int bytesCount = 0;

		while ((bytesCount = fis.read(byteArray)) != -1) {
			digest.update(byteArray, 0, bytesCount);
		};

		fis.close();

		byte[] bytes = digest.digest();

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < bytes.length; i++) {

			sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}
	
	
	public String aescbcDecrypt(String encData) {
		try {
			String secret = Settings.get("aescbckey");
			String decrypted = Aes256.decrypt(encData, secret);
			return decrypted;

		} catch (Exception e) {
			_log.error(e);
		}
		return "";

	}

	public String aescbcEncrypt(String data) {
		try {
			String secret = Settings.get("aescbckey");
			String encrypted_bytes = Aes256.encrypt(data, secret);
			return encrypted_bytes;

		} catch (Exception e) {
			_log.error(e);
		}
		return "";
	}
	
	public void closeFiles(String path) {
		try {
		File file=new File(path);
		 if (file.exists()) {
	            BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(file, true));
	            bufferWriter.write("New Text");
	            bufferWriter.newLine();
	            bufferWriter.close();
	        }
		}catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public String AESencrypt(File file) {

		try {
			String key = Settings.get("AES_Salt_key");
			String initVector =Settings.get("AES_Vector_key");
			byte[] bytesIV = initVector.getBytes(StandardCharsets.UTF_8);
			IvParameterSpec iv = new IvParameterSpec(bytesIV);
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
			byte[] fileBytes = Files.readAllBytes(file.toPath());
			byte[] encrypted = cipher.doFinal(fileBytes);

			return Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception ex) {
			_log.error(ex.getMessage());
		}
		return null;
	}

	public String AESdecryptFile(String encrypted) {
		try {
			String key =  Settings.get("AES_Salt_key");
			String initVector =  Settings.get("AES_Vector_key");
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(StandardCharsets.UTF_8));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
			writeByte(original);
			return new String(original);
		} catch (Exception ex) {
			_log.error(ex.getMessage());
		}

		return null;
	}
	
	 static void writeByte(byte[] bytes)
	    {
		 String FILEPATH = "C:\\Users\\Admin\\Documents\\decryptfile\\test.rar";
		 File file = new File(FILEPATH);
	 
	        // Try block to check for exceptions
	        try {
	 
	            // Initialize a pointer in file
	            // using OutputStream
	            OutputStream os = new FileOutputStream(file);
	 
	            // Starting writing the bytes in it
	            os.write(bytes);
	 
	            // Display message onconsole for successful
	            // execution
	            System.out.println("Successfully"
	                               + " byte inserted");
	 
	            // Close the file connections
	            os.close();
	        }
	 
	        // Catch block to handle the exceptions
	        catch (Exception e) {
	 
	            // Display exception on console
	            System.out.println("Exception: " + e);
	        }
	    }



}