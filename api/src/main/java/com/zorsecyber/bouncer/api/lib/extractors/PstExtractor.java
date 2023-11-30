package com.zorsecyber.bouncer.api.lib.extractors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.annotation.Nonnull;

import com.pff.PSTAttachment;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTMessage;
import com.zorsecyber.bouncer.api.exceptions.ArchiveExtractorException;
import com.zorsecyber.bouncer.api.lib.MimeTypeUtils;
import com.zorsecyber.bouncer.api.lib.SubmissionUtils;
import com.zorsecyber.bouncer.api.lib.WorkerUtils;
import com.zorsecyber.bouncer.api.lib.pipelines.PipelineUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PstExtractor implements ArchiveExtractor {
	private int depth = -1;
	public int numSubmittedFiles = 0;
	public float totalSize = 0;
	@Nonnull private Map<String, Object> metadata;

	public Map<String, Object> extractAndSubmit(File file, File workDir) throws ArchiveExtractorException {
		try {
			PSTFile pstFile = new PSTFile(file);
			extractAndSubmitHelper(pstFile.getRootFolder(), workDir);
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("numFiles", numSubmittedFiles);
			data.put("size", totalSize);
			return data;
		} catch (Exception ex) {
//			ex.printStackTrace();
			throw new ArchiveExtractorException("Failed to extract and submit from " + file.getAbsolutePath());
		}
	}

	private void extractAndSubmitHelper(PSTFolder folder, File outputDir) {
		try {
			depth++;
			// the root folder doesn't have a display name
			if (depth > 0) {

			}

			// go through the folders...
			if (folder.hasSubfolders()) {
				Vector<PSTFolder> childFolders = folder.getSubFolders();
				for (PSTFolder childFolder : childFolders) {
					if (childFolder.getNodeType() != 3) {
						extractAndSubmitHelper(childFolder, outputDir);
					}
				}
			}

			// and now the emails for this folder
			if (folder.getNodeType() != 3 && folder.getContentCount() > 0) {
				depth++;
				PSTMessage email = (PSTMessage) folder.getNextChild();
				while (email != null) {
					if (email.getNumberOfAttachments() > 0) {
						saveAndSubmitAttachment(email, outputDir);
					}
					email = (PSTMessage) folder.getNextChild();
				}
				depth--;
			}
			depth--;
		} catch (Exception ex) {
			 log.trace("pstException : " + ex.getMessage()+"\n"+ex.getStackTrace());
		}
	}

	private void saveAndSubmitAttachment(PSTMessage email, File outputDir) {
		int numberOfAttachments = email.getNumberOfAttachments();
		for (int x = 0; x < numberOfAttachments; x++) {
			try {
				PSTAttachment attach = email.getAttachment(x);
				InputStream attachmentStream = attach.getFileInputStream();
				// both long and short filenames can be used for attachments
				String originalFilename = attach.getLongFilename();
				// check out new worker for this file
				File file = new File(outputDir, WorkerUtils.getNewWorker().toString());
				FileOutputStream out = new FileOutputStream(file);
				// 8176 is the block size used internally and should give the best performance
				int bufferSize = 8176;
				byte[] buffer = new byte[bufferSize];
				int count = attachmentStream.read(buffer);
				while (count == bufferSize) {
					out.write(buffer);
					count = attachmentStream.read(buffer);
				}
				byte[] endBuffer = new byte[count];
				System.arraycopy(buffer, 0, endBuffer, 0, count);
				out.write(endBuffer);
				out.close();
				attachmentStream.close();
				
				log.info("extracted attachment "+file.getName()+" original name="+originalFilename);
				metadata.put("originalFileName", originalFilename);
				Map<String, Object> data = PipelineUtils.getAndRunPipeline(file, metadata);
				
				int submittedFiles = (int) data.get("numFiles");
					numSubmittedFiles+=submittedFiles;
					totalSize += (float) data.get("size");
				if (numSubmittedFiles > 0 && numSubmittedFiles % 100 == 0) {
					log.info("[" + (long) metadata.get("taskId") + "] Submitted " + numSubmittedFiles + " files");
				}
			} catch (Exception ex) {
//				ex.printStackTrace();
				log.debug("Exception: "+ex.getMessage());
			}
		}
	}

	public void printDepth() {
		for (int x = 0; x < depth - 1; x++) {
			System.out.print(" | ");
		}
		System.out.print(" |- ");
	}
}
