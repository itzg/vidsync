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
import me.itzgeoff.vidsync.domain.common.WatchedFile;
import me.itzgeoff.vidsync.domain.common.WatchedFilesRepository;

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
			logger.error("Unable to parse signature from "+videoFile, e);
			out(videoFile);
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
		
		if (incomingFiles.add(videoFile)) {
    		try {
    			final String canonicalPath = videoFile.getCanonicalPath();
                WatchedFile entry = repository.findByPath(canonicalPath);
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
		else {
            logger.debug("Already tracking incoming {}", videoFile);
		}
	}
	
	protected void nextPotentiallyKnownVideoFile(WatchedFile entry, File videoFile) throws IOException {
		logger.debug("Validating known file {} against {}", entry, videoFile);
		
		if (videoFile.lastModified() == entry.getLastModified() && 
				videoFile.length() == entry.getFileSize()) {
			logger.debug("Validated {}", videoFile);
			
			entry.setTheFile(videoFile);
			
			distributor.distributeExistingFile(entry, createCompletionConsumer());
			return;
		}
		else {
		    logger.debug("Modified {} != {} or Size {} != {}", videoFile.lastModified(), entry.getLastModified(), 
                videoFile.length(), entry.getFileSize());
			nextUnknownVideoFile(videoFile);
		}
	}

	protected void nextFoundSignature(final File videoFile, String signature) {
	    
	    // Look it up by signature to see if only metadata changed
		WatchedFile watchedFile = repository.findByContentSignature(signature);
		
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
				
				distributor.distributeNewFile(watchedFile, createCompletionConsumer());
			} catch (IOException | VidSyncException e) {
			    out(videoFile);
			}
		}
	}
	
	private ResultConsumer<WatchedFile, Boolean> createCompletionConsumer() {
	    return new ResultConsumer<WatchedFile, Boolean>() {
            
            @Override
            public void failedToReachResult(WatchedFile in, Exception e) {
                out(in.getTheFile());
            }
            
            @Override
            public void consumeResult(WatchedFile in, Boolean result) {
                out(in.getTheFile());
            }
        };
	}

	public void out(File videoFile) {
	    logger.trace("Outgoing file {}", videoFile);
        incomingFiles.remove(videoFile);
    }

    protected void nextPotentiallyChangedFile(File videoFile,
			WatchedFile watchedFile) {
		logger.debug("Checking for potential changes of {}", videoFile);
		
		watchedFile.setTheFile(videoFile);
		
		MovieInfo movieInfo;
		try {
            movieInfo = movieInfoParser.parse(videoFile);
        } catch (IOException | VidSyncException e) {
            logger.error("Unable to parse movie info from {}", videoFile);
            out(videoFile);
            return;
        }
		
		if (!movieInfo.getTitle().equals(watchedFile.getTitle())) {
		    // Update our title info
		    watchedFile.setTitle(movieInfo.getTitle());
		    
		    distributor.distributeChangedFile(watchedFile, createCompletionConsumer());
		}
		else {
		    // Just a courtesy refresh
		    distributor.distributeExistingFile(watchedFile, createCompletionConsumer());
		}
		
		// Save off any changes to the non-important, but tracked metadata
        try {
            watchedFile.setPath(videoFile.getCanonicalPath());
        } catch (IOException e) {
            watchedFile.setPath(videoFile.getAbsolutePath());
        }
        watchedFile.setFileSize(videoFile.length());
        watchedFile.setLastModified(videoFile.lastModified());
        watchedFile.setTheFile(videoFile);
		
		// Save it!
		WatchedFile saved = repository.save(watchedFile);
		logger.debug("Saved updated {}", saved);
	}

	protected void nextUnknownVideoFile(File videoFile) throws IOException {
		logger.debug("Processing unknown file {}", videoFile);
		
		try {
			movieInfoParser.validate(videoFile);

			logger.debug("Will parse signature of {}", videoFile);
			
			mdatSignatureParser.parseAsync(videoFile, signatureResultConsumer);
		} catch (VidSyncException e) {
		    // Only a debug level since this can happen while the file is still being encoded
			logger.debug("Validating file {}", e, videoFile);
			out(videoFile);
		}
	}

}
