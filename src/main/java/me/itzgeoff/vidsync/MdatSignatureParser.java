package me.itzgeoff.vidsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import me.itzgeoff.vidsync.common.PercentilePrinterProgressListener;
import me.itzgeoff.vidsync.common.ProgressListener;
import me.itzgeoff.vidsync.common.SuppressedProgressListener;
import me.itzgeoff.vidsync.common.VidSyncException;

import org.apache.commons.codec.binary.Base64;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.mdat.MediaDataBox;

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

	public String parse(File file, ProgressListener listener) throws IOException, VidSyncException {
		if (listener == null) {
			listener = SuppressedProgressListener.getInstance();
		}
		
		try (IsoFile isoFile = new IsoFile(file)) {
		
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
					if (length < MD_BUFFER_SIZE) {
						System.out.println("Last one");
					}
					ByteBuffer buffer = mdat.getContent(offset, length);
					md5.update(buffer);
				}
				
				byte[] signatureBytes = md5.digest();
				return Base64.encodeBase64String(signatureBytes);
			} catch (NoSuchAlgorithmException e) {
				throw new VidSyncException(e);
			}
		}
	}
}
