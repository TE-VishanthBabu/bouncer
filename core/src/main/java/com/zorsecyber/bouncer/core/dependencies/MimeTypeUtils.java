package com.zorsecyber.bouncer.core.dependencies;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

import com.nteligen.hq.dhs.siaft.exceptions.MimeTypeDetectionException;

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
			"application/x-rar-compressed"
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
	
	public static final List<String> metadefenderAllowedMimeTypes = Stream
			.of(pstMimeTypes, docMimeTypes, excelMimeTypes, pptMimeTypes, pdfMimeTypes, archiveMimeTypes, genericMimeTypes)
	        .flatMap(x -> x.stream())
	        .collect(Collectors.toList());
	
	public static final ArrayList<String> DeepSecureAllowedMimeTypes = new ArrayList<String>
	(Arrays.asList(new String[] {
			"application/octet-stream",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"application/vnd.ms-word.document.macroEnabled.12",
			"application/vnd.ms-word.document.macroenabled.12",
			"application/vnd.ms-powerpoint.presentation.macroEnabled.12",
			"application/vnd.ms-powerpoint.presentation.macroenabled.12",
			"application/vnd.ms-excel.sheet.macroEnabled.12",
			"application/vnd.ms-excel.sheet.macroenabled.12",
			"application/vnd.ms-word.template.macroEnabled.12",
			"application/vnd.ms-word.template.macroenabled.12",
			"application/pdf",
			"image/gif",
			"image/bmp",
			"image/jpeg",
			"image/jpg",
			"image/jp2",
			"image/png",
			"image/x-ms-bmp",
			"image/tiff",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.openxmlformats-officedocument.presentationml.slideshow",
			"application/vnd.ms-powerpoint.slideshow.macroEnabled.12",
			"application/vnd.ms-powerpoint.slideshow.macroenabled.12",
			"application/vnd.openxmlformats-officedocument.presentationml.template",
			"application/vnd.ms-powerpoint.template.macroEnabled.12",
			"application/vnd.ms-powerpoint.template.macroenabled.12",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.template",
			"application/vnd.ms-excel.addin.macroEnabled.12",
			"application/vnd.ms-excel.addin.macroenabled.12",
			"application/vnd.ms-excel.template.macroEnabled.12",
			"application/vnd.ms-excel.template.macroenabled.12",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.template",
			"application/vnd.ms-powerpoint.addin.macroEnabled.12",
			"application/vnd.ms-powerpoint.addin.macroenabled.12",
			"application/vnd.ms-visio.drawing",
			"application/vnd.ms-visio.viewer",
			"application/json",
			"application/xml",
			"text/xml",
			"application/zip",
			"application/x-zip-compressed",
			"application/x-compressed",
			"message/rfc822",
			"application/vnd.ms-outlook",
			"image/vnd.ms-photo",
			"image/jxr",
			"image/webp",
			"audio/x-wav"
			}));
	
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
//		log.info("unsupported mime type : " + mimeType);
		return false;
	}
	
	public static Boolean allowedMimeType(String mimeType) {
		if (MimeTypeUtils.coreAllowedMimeTypes.contains(mimeType)) {
			return true;
		}
		return false;
	}
}
