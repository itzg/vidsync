package me.itzgeoff.vidsync.common;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.mdat.MediaDataBox;

@Component
public class MdatSignatureParser {
	
	private static final String MD_ALGO = "MD5";
	private static final int MD_BUFFER_SIZE = 5000;

	public static void main(String[] args) throws IOException, VidSyncException {
		if (args.length < 1) {
			System.err.println("Missing filename arg");
			System.exit(1);
		}
		MdatSignatureParser parser = new MdatSignatureParser();
		long start = System.nanoTime();
		String signature = parser.parse(new File(args[0]), new PercentilePrinterProgressListener("Processed"));
		long duration = System.nanoTime() - start;
		
		System.out.printf("Signature of %s is %s (took %f seconds)%n", args[0], signature, duration / (double)1e9);
	}
	
	@Async("signature")
	public void parseAsync(File file, ResultConsumer<File, String> consumer) {
		try {
			String signature = parse(file, null);
			consumer.consumeResult(file, signature);
		} catch (IOException | VidSyncException e) {
			consumer.failedToReachResult(file, e);
		}
	}

	public String parse(File file, ProgressListener listener) throws IOException, VidSyncException {
		if (listener == null) {
			listener = SuppressedProgressListener.getInstance();
		}
		
		try (FileChannel channel = FileChannel.open(file.toPath())) {
			try (IsoFile isoFile = new IsoFile(channel)) {
			
				List<MediaDataBox> mdatList = isoFile.getBoxes(MediaDataBox.class);
				if (mdatList.isEmpty()) {
					throw new VidSyncException("Missing mdat box");
				}
				
				MediaDataBox mdat = mdatList.get(0);
				
				try {
					MessageDigest md5 = MessageDigest.getInstance(MD_ALGO);
					
					// Needed to compute content size since the box didn't give us that directly
				final long contentSize = mdat.getSize() - mdat.getHeader().limit();
					
					listener.expectedTotal(contentSize);
					for (long offset = 0; offset < contentSize; offset += MD_BUFFER_SIZE) {
						listener.update(offset);
						int length = Math.min(MD_BUFFER_SIZE, (int)(contentSize - offset));
						ByteBuffer buffer = mdat.getContent(offset, length);
						md5.update(buffer);
					}
					
					byte[] signatureBytes = md5.digest();
					return Base64.encodeBase64String(signatureBytes);
				} catch (NoSuchAlgorithmException e) {
					throw new VidSyncException(e);
				}
			}
			finally {
				// Since mp4parser does a whole lot of NIO caching and we're doing this
				// iteratively, we'll force a gc to working set memory bounded
				System.gc();
			}
		}
	}
}
