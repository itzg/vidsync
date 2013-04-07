package me.itzgeoff.vidsync.server;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import me.itzgeoff.vidsync.common.MdatSignatureParser;
import me.itzgeoff.vidsync.common.MovieInfo;
import me.itzgeoff.vidsync.common.MovieInfoParser;
import me.itzgeoff.vidsync.common.ResultConsumer;
import me.itzgeoff.vidsync.common.VidSyncException;
import me.itzgeoff.vidsync.domain.server.WatchedFile;
import me.itzgeoff.vidsync.domain.server.WatchedFilesRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class WatchedFileRouter {
	private class SignatureResultConsumer implements ResultConsumer<File, String> {

		@Override
		public void consumeResult(File videoFile, String signature) {
			logger.debug("Got signature {} for {}", signature, videoFile);
			nextFoundSignature(videoFile, signature);
		}

		@Override
		public void failedToReachResult(File videoFile, Exception e) {
			logger.error("Unable to parse signature from {}", e, videoFile);
			incomingFiles.remove(videoFile);
		}
		
	}

	private static final Logger logger = LoggerFactory
			.getLogger(WatchedFileRouter.class);

	@Autowired
	private MdatSignatureParser mdatSignatureParser;
	
	@Autowired
	private MovieInfoParser movieInfoParser;
	
	@Autowired
	private WatchedFilesRepository repository;
	
	@Autowired
	private ClientDistributor distributor;
	
	private Set<File> incomingFiles = Collections.synchronizedSet(new LinkedHashSet<File>());
	
	private SignatureResultConsumer signatureResultConsumer = new SignatureResultConsumer();

	@Async("worker")
	public void in(File videoFile) {
        if (!videoFile.exists()) {
            logger.debug("Given a file that doesn't actually exist: {}", videoFile);
            return;
        }
        
		logger.trace("Incoming file {}", videoFile);
		
		if (incomingFiles.contains(videoFile)) {
			logger.debug("Already tracking incoming {}", videoFile);
			return;
		}
		
		try {
			WatchedFile entry = repository.findByPath(videoFile.getCanonicalPath());
			if (entry == null) {
				nextUnknownVideoFile(videoFile);
			}
			else {
				nextPotentiallyKnownVideoFile(entry, videoFile);
			}
		} catch (IOException e) {
			logger.error("Trying to get canonical form {}", e);
		}
		
	}
	
	protected void nextPotentiallyKnownVideoFile(WatchedFile entry, File videoFile) throws IOException {
		logger.debug("Validating known file {} against {}", entry, videoFile);
		
		if (videoFile.lastModified() == entry.getLastModified() && 
				videoFile.length() == entry.getFileSize()) {
			logger.debug("Validated {}", videoFile);
			return;
		}
		else {
			nextUnknownVideoFile(videoFile);
		}
	}

	protected void nextFoundSignature(final File videoFile, String signature) {
		WatchedFile watchedFile = repository.findByContentSignature(signature);
		
		try {
			
			if (watchedFile != null) {
				nextPotentiallyChangedFile(videoFile, watchedFile);
			}
			else {
				try {
					watchedFile = new WatchedFile(videoFile);
					watchedFile.setContentSignature(signature);
					
					MovieInfo movieInfo = movieInfoParser.parse(videoFile);
					watchedFile.setTitle(movieInfo.getTitle());
					
					watchedFile = repository.save(watchedFile);
					
					logger.debug("Saved newly watched file {}", watchedFile);
					
					distributor.distributeNewFile(watchedFile);
				} catch (IOException | VidSyncException e) {
					logger.error("Trying to init metadata from {}", e, videoFile);
				}
			}
		
		}
		finally {
			incomingFiles.remove(videoFile);
		}
	}

	protected void nextPotentiallyChangedFile(File videoFile,
			WatchedFile watchedFile) {
		logger.debug("Checking for potential changes of {}", videoFile);
		
		// TODO Auto-generated method stub
		
	}

	protected void nextUnknownVideoFile(File videoFile) throws IOException {
		logger.debug("Processing unknown file {}", videoFile);
		
		// Check the result to protect against race condition
		if (incomingFiles.add(videoFile)) {
			try {
				movieInfoParser.validate(videoFile);

				logger.debug("Will parse signature of {}", videoFile);
				
				mdatSignatureParser.parseAsync(videoFile, signatureResultConsumer);
			} catch (VidSyncException e) {
			    // Only a debug level since this can happen while the file is still being encoded
				logger.debug("Validating file {}", e, videoFile);
				incomingFiles.remove(videoFile);
			}
		}
	}
	
}
