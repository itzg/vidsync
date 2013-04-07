package me.itzgeoff.vidsync.client;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.annotation.PostConstruct;

import me.itzgeoff.vidsync.domain.common.WatchedFile;
import me.itzgeoff.vidsync.domain.common.WatchedFilesRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class Receiver {
    
    private static final Logger logger = LoggerFactory.getLogger(Receiver.class);

    @Value("${client.baseDirectory}")
    private File baseDirectory;
    
    @Value("${client.videoFileSuffix}")
    private String suffix;
    
    @Value("${client.transferBufferSize}")
    private int transferBufferSize;
    
    @Autowired
    private WatchedFilesRepository repository;

    private WatchedFile watchedFile;

    private FileChannel fileChannel;

    private ByteBuffer transferBuffer;

    private Path videoFilePath;

    private ServerSocketChannel serverSocketChannel;
    
    @PostConstruct
    public void init() {
        transferBuffer = ByteBuffer.allocate(transferBufferSize);
    }


    public int createSocket(WatchedFile watchedFile) throws IOException {
        this.watchedFile = watchedFile;

        videoFilePath = baseDirectory.toPath().resolve(normalizeTitleToFilename(watchedFile.getTitle())+"."+suffix);
        
        fileChannel = FileChannel.open(videoFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        logger.debug("Opened file channel on {}", videoFilePath);
        
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(0));
        
        logger.debug("Opened server socket {}", serverSocketChannel);
        
        return ((InetSocketAddress)serverSocketChannel.getLocalAddress()).getPort();
    }
    
    private String normalizeTitleToFilename(String title) {
        return title.replaceAll(":", "_");
    }


    @Async("receiver")
    public void receive() {
        try {
            try (SocketChannel receiverChannel = serverSocketChannel.accept()) {
                logger.debug("Accepted socket {}", receiverChannel);
                
                transferBuffer.clear();
                while (receiverChannel.read(transferBuffer) > 0 || transferBuffer.position() > 0) {
                    transferBuffer.flip();
                    fileChannel.write(transferBuffer);
                    transferBuffer.compact();
                }
                
                WatchedFile ourWatchedFile = new WatchedFile();
                ourWatchedFile.setTitle(watchedFile.getTitle());
                ourWatchedFile.setPath(videoFilePath.toString());
                ourWatchedFile = repository.save(ourWatchedFile);
                
                logger.debug("Transfer complete. Saved watched file as {}", ourWatchedFile);
            }
            
        } catch (IOException e) {
            logger.error("Failed to accept receiver socket", e);
        } finally {
            try {
                fileChannel.close();
            } catch (IOException e) {
                logger.warn("Trying to close file", e);
            }
            
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                logger.warn("Trying to close server socket", e);
            }
        }
    }
}
