package com.zorsecyber.bouncer.api.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MimeTypeUtils {

	public static final ArrayList<String> pdfMimeTypes = new ArrayList<String>
	(Arrays.asList(new String[] {
			"application/pdf"
			}));
	
	public static final ArrayList<String> pstMimeTypes = new ArrayList<String>
	(Arrays.asList(new String[] {
			"application/vnd.ms-outlook",
			"application/vnd.ms-outlook-pst",
			}));
	
	public static final ArrayList<String> docMimeTypes = new ArrayList<String>
	(Arrays.asList(new String[] {
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/msword",
			"application/vnd.ms-word.document.macroenabled.12"
			}));
	
	public static final ArrayList<String> excelMimeTypes = new ArrayList<String>
	(Arrays.asList(new String[] {
			"application/vnd.ms-excel",
			"application/vnd.ms-excel.sheet.binary.macroenabled.12",
			"application/vnd.ms-excel.sheet.macroenabled.12",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
			}));
	
	public static final ArrayList<String> pptMimeTypes = new ArrayList<String>
	(Arrays.asList(new String[] {
			"application/vnd.ms-powerpoint", 
			"application/vnd.ms-powerpoint.presentation.macroenabled.12",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation"
			}));
	
	public static final ArrayList<String> genericMimeTypes = new ArrayList<String>
	(Arrays.asList(new String[] {
			"application/x-tika-msoffice",
			"application/x-tika-ooxml",
			"application/octet-stream"
			}));
	
	public static final ArrayList<String> zipMimeTypes = new ArrayList<String>
	(Arrays.asList(new String[] {
			"application/zip",
			"application/x-zip-compressed"
			}));
	
	public static final ArrayList<String> rarMimeTypes = new ArrayList<String>
	(Arrays.asList(new String[] {
//			"application/x-rar-compressed"
			}));
	
	public static final ArrayList<String> archiveMimeTypes = (ArrayList<String>) Stream
			.of(pstMimeTypes, zipMimeTypes, rarMimeTypes)
			.flatMap(x -> x.stream())
			.collect(Collectors.toList());
	
	public static final ArrayList<String> officeFileMimeTypes = (ArrayList<String>) Stream
			.of(docMimeTypes, excelMimeTypes, pptMimeTypes)
			.flatMap(x -> x.stream())
			.collect(Collectors.toList());
	
	public static final ArrayList<String> singularFileMimeTypes = (ArrayList<String>) Stream
			.of(officeFileMimeTypes, genericMimeTypes, pdfMimeTypes)
			.flatMap(x -> x.stream())
			.collect(Collectors.toList());
	
	public static final List<String> coreAllowedMimeTypes = Stream
			.of(pstMimeTypes, singularFileMimeTypes, zipMimeTypes)
	        .flatMap(x -> x.stream())
	        .collect(Collectors.toList());
	
	public static String getMimeType(File file) throws Exception {
		return getMimeType(file, null);
	}
	
	public static String getMimeType(File file, String filename) throws Exception {
		final Metadata tikaMetadata = new Metadata();
		if(filename != null)
			tikaMetadata.add(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
		try (FileInputStream fis = new FileInputStream(file)) {
		return new Tika().detect(fis, tikaMetadata);
		} catch (Exception e) {
			String errorMsg = "Could not get mime type for file "+file.getName();
			if(filename != null)
				errorMsg+=" filename "+filename;
			throw new Exception(errorMsg);
		}
	}
	
	public static String stripMimeTypeVersion(String mimeType) {
		if(mimeType != null && mimeType.contains(";")) {
			return mimeType.split(";")[0];
		}
		return mimeType;
	}
	
	public static Boolean allowedMimeType(File file) throws IOException {
		Tika tika = new Tika();
		final Metadata tikaMetadata = new Metadata();
		tikaMetadata.add(TikaCoreProperties.RESOURCE_NAME_KEY, file.getName());
		String mimeType = MimeTypeUtils.stripMimeTypeVersion(tika.detect(file));
		if (MimeTypeUtils.coreAllowedMimeTypes.contains(mimeType)) {
			return true;
		}
		log.trace("unsupported mime type : " + mimeType);
		return false;
	}
	
	public static ArrayList<String> getMimeTypeClass(String mimeType, ArrayList<ArrayList<String>> mimeTypeClasses) {
		for(ArrayList<String> mimeTypeClass : mimeTypeClasses) {
			if(mimeTypeClass.contains(mimeType)) {
				return mimeTypeClass;
			}
		}
		return null;
	}
}
