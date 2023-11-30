package com.zorsecyber.bouncer.webapp.controller;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.options.BlobBeginCopyOptions;
import com.azure.storage.blob.specialized.AppendBlobClient;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import com.zorsecyber.bouncer.webapp.WebPageController;
import com.zorsecyber.bouncer.webapp.dao.User;
import com.zorsecyber.bouncer.webapp.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class FileUploadController extends WebPageController {
	
	@Value("${storage.userdata.container}")
	private String userDataBlobContainer;

	@Value("${storage.userdata.connection-string}")
	private String connectionString;
	
	@Value("${storage.upload.chunk-staging-directory}")
	private String blobTempDirectory;

	@Value("${storage.upload.chunk-size}")
	private long chunkSize;
	
	@Value("${storage.upload.read-timeout}")
	private long blobReadTimeout;
	
	@Value("${storage.upload.prune-timeout}")
	private long pruneTimeout;

	@Autowired
	private UserRepository userRepository;

	/*
	 * Upload web page mapping
	 */
	@GetMapping("/upload")
	public String listUploadedFiles(HttpServletRequest request, Model model) throws IOException {
		model.addAllAttributes(loadPageAttributes(request));
		return "upload";
	}

	/**
	 * Upload a file chunk to azure blob container
	 * @param chunkNum
	 * @param filename
	 * @param lastChunk
	 * @param chunk
	 * @param redirectAttributes
	 * @param model
	 * @param request
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@PostMapping("/upload/chunk")
	@ResponseBody
	public String handleFileUpload(@RequestParam long chunkNum, @RequestParam String filename, @RequestParam boolean lastChunk, @RequestPart MultipartFile chunk, RedirectAttributes redirectAttributes,
			Model model, HttpServletRequest request) throws IOException, InterruptedException {
		log.info("uploading chunk for " + filename);

		if (chunk.getOriginalFilename().equals(StringUtils.EMPTY)) {
			redirectAttributes.addFlashAttribute("message", "Please select a file");
			return "redirect:/upload";
		}

		model.addAllAttributes(loadPageAttributes(request));
		User user = userRepository.findByEmail((String) model.getAttribute("userEmail"));
		String tempBlobName = user.getUserId() + "/" + blobTempDirectory + "/" + filename;
		
		AppendBlobClient tempBlob = new BlobClientBuilder().connectionString(connectionString)
				.containerName(userDataBlobContainer).blobName(tempBlobName)
				.buildClient().getAppendBlobClient();
		
		try (BufferedInputStream is = new BufferedInputStream(chunk.getInputStream())) {
			if(chunkNum == 0)	{
				cleanStagingDirectory(user);
				tempBlob.deleteIfExists();
				tempBlob.create();
			}
			tempBlob.appendBlock(is, chunk.getSize());
			log.info("uploaded chunk "+chunkNum);
		}
		
		if(lastChunk) {
			String blobName = user.getUserId() + "/" + filename;
			AppendBlobClient blob = new BlobClientBuilder().connectionString(connectionString)
					.containerName(userDataBlobContainer).blobName(blobName)
					.buildClient().getAppendBlobClient();
			BlobBeginCopyOptions o = new BlobBeginCopyOptions(tempBlob.getBlobUrl());
			blob.beginCopy(o);
			tempBlob.deleteIfExists();
		}

//		redirectAttributes.addFlashAttribute("message",
//				"successfully uploaded " + filename + "!");

		return new JSONObject()
				.put("data", new JSONObject()
				.put("status", "success")
				.put("chunk", chunkNum)).toString();
	}
	
	// deletes any unfinished uploads from user's staging directory
	public void cleanStagingDirectory(User user) {
		List<String> blobs = listFilesAndDirectories(Long.toString(user.getUserId())+"/"+blobTempDirectory+"/");
		for(String blobName : blobs) {
			AppendBlobClient blob = new BlobClientBuilder().connectionString(connectionString)
					.containerName(userDataBlobContainer).blobName(blobName)
					.buildClient().getAppendBlobClient();
			BlobProperties properties = blob.getProperties();
			log.debug("blob "+blobName+" last access "+properties.getLastModified().toString());
			if(properties.getLastModified().isBefore(OffsetDateTime.now().minusSeconds(pruneTimeout))) {
				log.debug("deleting unfinished upload "+blobName);
				blob.deleteIfExists();
			}
		}
	}
	
	// lists blobs and virtual folders with a given prefix
	public List<String> listFilesAndDirectories(String prefix) {
		List<String> list = new ArrayList<String>();
		BlobContainerClient clientBcClient = getUserDataBlobContainerClient();
		ListBlobsOptions options = new ListBlobsOptions()
			     .setPrefix(prefix)
			     .setDetails(new BlobListDetails()
			         .setRetrieveDeletedBlobs(false)
			         .setRetrieveSnapshots(false));
		clientBcClient.listBlobsByHierarchy("/", options, Duration.ofSeconds(blobReadTimeout))
				.forEach(blob -> 
				list.add(blob.getName())
				);
		return list;
	}
	
	  /**
	   * This method returns a blobcontainerclient pertaining to a client's 
	   * private blob container.
	   * @return BlobContainerClient
	   * 			The blobcontainerclient pertaining to the client's blobcontainer
	   */
	public BlobContainerClient getUserDataBlobContainerClient()
	{
		return new BlobContainerClientBuilder().connectionString(connectionString)
				.containerName(userDataBlobContainer).buildClient();
	}
	
//	// async
//	@PostMapping("/upload/chunk")
//	@ResponseBody
//	public String handleFileUpload(@RequestParam long chunkNum, @RequestParam String filename, @RequestParam String ref,
//			@RequestParam boolean lastChunk, @RequestPart MultipartFile chunk, RedirectAttributes redirectAttributes,
//			Model model, HttpServletRequest request) throws IOException, InterruptedException {
//		log.info("uploading chunk for " + filename);
//
//		if (chunk.getOriginalFilename().equals(StringUtils.EMPTY)) {
//			redirectAttributes.addFlashAttribute("message", "Please select a file");
//			return "redirect:/upload";
//		}
//
//		model.addAllAttributes(loadPageAttributes(request));
//		User user = userRepository.findByEmail((String) model.getAttribute("userEmail"));
//		String blobName = user.getUserId() + "/" + filename;
//		String chunkName = chunkStagingDirectory +"/"+ref + "." + chunkNum;
//		
//		BlobClient chunkBlob = new BlobClientBuilder().connectionString(connectionString)
//				.containerName(userDataBlobContainer).blobName(chunkName)
//				.buildClient();
//
//		try (BufferedInputStream is = new BufferedInputStream(chunk.getInputStream())) {
//				chunkBlob.deleteIfExists();
//				chunkBlob.upload(is);
//		}
//		log.info("wrote chunk "+chunkNum +" to "+chunkName);
//		
//		if(lastChunk) reassembleChunks(blobName, ref, chunkNum);
//
//		redirectAttributes.addFlashAttribute("message",
//				"successfully uploaded " + filename + "!");
//
//		return new JSONObject()
//				.put("data", new JSONObject()
//				.put("status", "success")
//				.put("chunk", chunkNum)).toString();
//	}
	
//	private void reassembleChunks(String blobName, String ref, long numChunks) throws IOException, InterruptedException {
//		log.info("assembling "+blobName+" from "+numChunks+ " chunks");
//		AppendBlobClient blob = new BlobClientBuilder().connectionString(connectionString)
//				.containerName(userDataBlobContainer).blobName(blobName)
//				.buildClient().getAppendBlobClient();
//		// create blob
//		blob.deleteIfExists();
//		blob.create();
//		log.info("created blob "+blobName);
//		
//		// append each chunk to blob
//		String chunkName;
//		AppendBlobClient chunk = null;
//		BlobOutputStream os = blob.getBlobOutputStream(true);
//		for(long i=0; i<numChunks+1; i++) {
//			try {
//			chunkName = chunkStagingDirectory + "/" + ref + "." + i;
//			chunk = new BlobClientBuilder()
//					.connectionString(connectionString)
//					.containerName(userDataBlobContainer)
//					.blobName(chunkName)
//					.buildClient().getAppendBlobClient();
//			log.info("got client for chunk "+chunkName);
//			while(!chunk.exists()) {
//				log.warn("chunk "+i+" doesn't exist. waiting 0.05 seconds");
//				Thread.sleep(50);
//			}
//			log.info("merging chunk "+i);
//			IOUtils.copy(chunk.openInputStream(), os);
//			log.info("merged chunk "+i);
//			} catch (Exception e) {
//				e.printStackTrace();
//				log.error("exception "+e);
//			} finally {
//				if(chunk!=null) chunk.deleteIfExists();
//				log.info("deleted chunk "+i);
//			}
//		}
//	}
}