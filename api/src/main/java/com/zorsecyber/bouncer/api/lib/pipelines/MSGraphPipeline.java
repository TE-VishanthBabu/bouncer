package com.zorsecyber.bouncer.api.lib.pipelines;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import com.microsoft.graph.http.BaseCollectionPage;
import com.microsoft.graph.http.BaseCollectionRequest;
import com.microsoft.graph.http.BaseCollectionRequestBuilder;
import com.microsoft.graph.http.BaseRequest;
import com.microsoft.graph.http.BaseRequestBuilder;
import com.microsoft.graph.http.ICollectionResponse;
import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.Entity;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.AttachmentCollectionPage;
import com.microsoft.graph.requests.AttachmentCollectionRequestBuilder;
import com.microsoft.graph.requests.FileAttachmentStreamRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageCollectionPage;
import com.microsoft.graph.requests.MessageCollectionRequestBuilder;
import com.zorsecyber.bouncer.api.dal.oauth2.MSoAuth2TokenRefresher;
import com.zorsecyber.bouncer.api.dal.oauth2.OAuth2TokenRefresher;
import com.zorsecyber.bouncer.api.dao.oauth2.AccessToken;
import com.zorsecyber.bouncer.api.dao.oauth2.OAuth2Token;
import com.zorsecyber.bouncer.api.exceptions.OAuth2Exception;
import com.zorsecyber.bouncer.api.exceptions.PipelineException;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

@Slf4j
@RequiredArgsConstructor
public class MSGraphPipeline implements SubmitPipeline {
	private static final long LOOKAHEAD_MILLIS = 5 * 60 * 1000; // 5 mins
	private long tokenExpiresIn = OAuth2Token.DEFAULT_ACCESS_TOKEN_LIFETIME;
	private OAuth2TokenRefresher tokenRefresher;
	@Nonnull
	private Map<String, Object> metadata;

	@SuppressWarnings({ "unchecked", "null" })
	public Map<String, Object> run(File file) throws PipelineException {
		File systemTempDir = new File(System.getProperty("java.io.tmpdir"));
		File tempDir = new File(systemTempDir, file.getName() + "-work");
		// create work dir for this workerId
		if (tempDir.exists() && tempDir.isDirectory()) {
			tempDir.delete();
		}
		log.debug("creating work dir " + tempDir.getName());
		tempDir.mkdirs();

		final GraphServiceClient<Request> graphServiceClient = (GraphServiceClient<Request>) metadata
				.get("graphServiceClient");
		final String mailbox = (String) metadata.get("mailbox");
		final Date untilDate = (Date) metadata.get("until");
		final int days = (int) metadata.get("days");

		int numSubmittedFiles = 0;
		float totalSize = 0;

		Calendar since = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		Calendar until = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		since.setTime(untilDate);
		until.setTime(untilDate);
		
		since.add(Calendar.DAY_OF_MONTH, -days);
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		String isoSince = df.format(since.getTime());
		String isoUntil = df.format(untilDate.getTime());
		log.debug("Processing messages from " + since.getTime().toString() + " to " + untilDate.toString());

		AccessToken accessToken = (AccessToken) metadata.get("accessToken");
		tokenExpiresIn = accessToken.expiresIn();
		try {
			tokenRefresher = new MSoAuth2TokenRefresher();
			refreshTokenIfNecessary(accessToken);
		} catch (Exception ex) {
			throw new PipelineException(ex);
		}

		// get user
		log.debug("getting user " + mailbox);
		User user = graphServiceClient.users(mailbox).buildRequest().select("id").get();
		log.debug("got user " + mailbox + ". getting emails");
		// get messages
		MessageCollectionPage messages = graphServiceClient.users(mailbox).messages().buildRequest()
				.filter("ReceivedDateTime ge " + isoSince + " AND ReceivedDateTime le " + isoUntil)
				.select("attachments").get();
		log.debug("got emails for " + mailbox);

		// remove graphServiceClient so graphPipeline does not get selected again
		metadata.remove("graphServiceClient");
		while (messages != null) {
			for (Message message : messages.getCurrentPage()) {
				try {
					AttachmentCollectionPage attachments = graphServiceClient.users(user.id).messages(message.id)
							.attachments().buildRequest().select("id,name").get();
					while (attachments != null) {
						for (Attachment attachment : attachments.getCurrentPage()) {
							try {
								log.debug("downloading attachment " + attachment.name);
								file = downloadAttachment(graphServiceClient, user.id, message.id, attachment, tempDir);
								// in case download fails
								if (file == null) {
									continue;
								}
								log.debug("Downloaded " + file.getName() + " from MS Graph api");

								// update metadata for pipeline selection
								metadata.put("originalFileName", file.getName());

								Map<String, Object> data = PipelineUtils.getAndRunPipeline(file, metadata);
								numSubmittedFiles += (int) data.get("numFiles");
								totalSize += (float) data.get("size");

							} catch (PipelineException ex) {
//								ex.printStackTrace();
								log.debug("Pipeline exception: " + ex.getMessage());
							} finally {
								if (file != null && file.exists()) {
									file.delete();
									log.debug("Deleted file " + file.getName());
									log.trace("Delete successful for " + file.getName() + " ? " + !file.exists());
								}
							}
						}
						attachments = getNextAttachmentsPage(attachments);
					}

					refreshTokenIfNecessary(accessToken);

				} catch (Exception ex) {
					log.warn("Exception while processing message page", ex);
				}

			}
			messages = getNextMessagesPage(messages);
		}

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("numFiles", numSubmittedFiles);
		data.put("size", totalSize);
		return data;
	}
	
	private AttachmentCollectionPage getNextAttachmentsPage(AttachmentCollectionPage attachments) {
		AttachmentCollectionRequestBuilder nextPage = attachments.getNextPage();
		if (nextPage == null) {
			log.trace("next attachments page is null");
			return null;
		} else {
			return nextPage.buildRequest().get();
		}
	}
	
	private MessageCollectionPage getNextMessagesPage(MessageCollectionPage messages) {
		MessageCollectionRequestBuilder nextPage = messages.getNextPage();
		if (nextPage == null) {
			log.trace("next messages page is null");
			return null;
		} else {
			return nextPage.buildRequest().get();
		}
	}

	@SuppressWarnings("null")
	private File downloadAttachment(GraphServiceClient<Request> graphServiceClient, String userId, String messageId,
			Attachment attachment, File outputDir) {
		URL requestUrl = graphServiceClient.users(userId).messages(messageId).attachments(attachment.id).buildRequest()
				.getRequestUrl();
		try (InputStream stream = new FileAttachmentStreamRequestBuilder(requestUrl.toString() + "/$value",
				graphServiceClient, null).buildRequest().get()) {
			// both long and short filenames can be used for attachments
			File file = new File(outputDir, attachment.name);
			log.debug("Downloading file to " + file.getAbsolutePath());
			// save file to disk
			Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return file;
		} catch (Exception ex) {
//			ex.printStackTrace();
			log.warn("Unable to download attachment " + attachment.name, ex);
			return null;
		}
	}

	private AccessToken refreshTokenIfNecessary(AccessToken accessToken) throws OAuth2Exception {
		try {
			if (tokenExpiresIn < LOOKAHEAD_MILLIS) {
				log.debug("Refreshing tokens");
				return tokenRefresher.RefreshAccessToken(accessToken);
			}
			return accessToken;
		} catch (Exception ex) {
			throw new OAuth2Exception("Enable to check/refresh token for user " + accessToken.getUser(), ex);
		}
	}

}
