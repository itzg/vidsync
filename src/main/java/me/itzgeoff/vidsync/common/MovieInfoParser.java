package me.itzgeoff.vidsync.common;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.springframework.stereotype.Component;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.HandlerBox;
import com.coremedia.iso.boxes.MetaBox;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.MovieHeaderBox;
import com.coremedia.iso.boxes.UserDataBox;
import com.coremedia.iso.boxes.apple.AppleItemListBox;
import com.coremedia.iso.boxes.apple.AppleTrackTitleBox;
import com.coremedia.iso.boxes.mdat.MediaDataBox;

@Component
public class MovieInfoParser {
	
	private String[] titleRegExReplacements = new String[]{
		"\\.\\p{Alnum}+$", "",
		"_", " "
	};
	
	public static void main(String[] args) throws IOException, VidSyncException {
		if (args.length < 1) {
			System.err.println("Missing filename arg");
			System.exit(1);
		}
		MovieInfoParser parser = new MovieInfoParser();
		MovieInfo info = parser.parse(new File(args[0]));
		
		System.out.printf("Parsed %s", info);
	}

	public MovieInfoParser() {
	}
	
	public void validate(File file) throws IOException, VidSyncException {
		try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			try (IsoFile isoFile = new IsoFile(channel)) {
				if (isoFile.getBoxes(MovieBox.class).isEmpty()) {
					throw new VidSyncException("Missing moov box");
				}
	
				if (isoFile.getBoxes(MediaDataBox.class).isEmpty()) {
					throw new VidSyncException("Missing mdat box");
				}
			}
			catch (Exception e) {
			    throw new VidSyncException("Unable to parse file");
			}
		}
	}

	public MovieInfo parse(File file) throws IOException, VidSyncException {
		MovieInfo info = new MovieInfo();
		
		info.setTitle(deriveTitleFromFilename(file));
		
		try (FileChannel channel = FileChannel.open(file.toPath())) {
			try (IsoFile isoFile = new IsoFile(channel)) {
				List<MovieBox> moovList = isoFile.getBoxes(MovieBox.class);
				if (moovList.isEmpty()) {
					throw new VidSyncException("Missing moov box");
				}
				
				MovieBox moov = moovList.get(0);
				
				MovieHeaderBox movieHeaderBox = moov.getMovieHeaderBox();
				final long timescale = movieHeaderBox.getTimescale();
				
				info.setDuration(movieHeaderBox.getDuration()/timescale);
				
				List<UserDataBox> udtaBoxes = moov.getBoxes(UserDataBox.class);
				if (!udtaBoxes.isEmpty()) {
					UserDataBox udta = udtaBoxes.get(0);
					List<MetaBox> metaBoxes = udta.getBoxes(MetaBox.class);
					if (!metaBoxes.isEmpty()) {
						MetaBox meta = metaBoxes.get(0);
						List<HandlerBox> hdlrBoxes = meta.getBoxes(HandlerBox.class);
						if (!hdlrBoxes.isEmpty()) {
							HandlerBox hdlr = hdlrBoxes.get(0);
							switch (hdlr.getHandlerType()) {
								case "mdir":
									handleAppleMetadata(meta, info);
									break;
							}
						}
					}
				}
			}
		}
		
		return info;
		
	}

	private String deriveTitleFromFilename(File file) {
		String name = file.getName();
		
		for (int i = 0; i < titleRegExReplacements.length; i += 2) {
			name = name.replaceAll(titleRegExReplacements[i], titleRegExReplacements[i+1]);
		}
		
		return name;
	}

	private void handleAppleMetadata(MetaBox meta, MovieInfo info) throws VidSyncException {
		List<AppleItemListBox> ilistBoxes = meta.getBoxes(AppleItemListBox.class);
		if (ilistBoxes.isEmpty()) {
			throw new VidSyncException("Expected ilist box");
		}
		
		AppleItemListBox ilist = ilistBoxes.get(0);
		List<AppleTrackTitleBox> titleBoxes = ilist.getBoxes(AppleTrackTitleBox.class);
		
		if (!titleBoxes.isEmpty()) {
			info.setTitle(titleBoxes.get(0).getValue());
		}
	}
}
