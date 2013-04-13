package me.itzgeoff.vidsync.server;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.StandardOpenOption;

import me.itzgeoff.vidsync.common.ResultConsumer;
import me.itzgeoff.vidsync.domain.common.WatchedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Async("sender")
public class ClientSender {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientSender.class);
    
    @Value("${senderBufferSize:5000}")
    private int senderBufferSize;
    
    public void send(WatchedFile file, ServerViewOfClientInstance clientView, ResultConsumer<WatchedFile, Boolean> resultConsumer) {
        logger.debug("Sending {} to {}", file, clientView);
        
        try {
            int receiverPort = clientView.getProxy().prepareForTransfer(file);
            
            logger.debug("Got receiver port {} for transfer of {}", receiverPort, file);
            
            
            try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(clientView.getServiceInfo().getInet4Addresses()[0], receiverPort))) {
                logger.debug("Opened socket {}", socketChannel);
                
                ByteBuffer sendBuffer = ByteBuffer.allocate(senderBufferSize);
                
                try (FileChannel fileChannel = FileChannel.open(file.getTheFile().toPath(), StandardOpenOption.READ)) {
                    
                    sendBuffer.clear();
                    while (fileChannel.read(sendBuffer) > 0 || sendBuffer.position() > 0) {
                        sendBuffer.flip();
                        socketChannel.write(sendBuffer);
                        sendBuffer.compact();
                    }
                    
                    logger.debug("Finished sending");
                    resultConsumer.consumeResult(file, true);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to prepare or send file", e);
            resultConsumer.failedToReachResult(file, e);
        }
    }
}
