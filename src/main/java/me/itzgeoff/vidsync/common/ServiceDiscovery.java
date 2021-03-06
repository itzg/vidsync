/**
 * Copyright 2013 Geoff Bourne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.itzgeoff.vidsync.common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Geoff
 *
 */
@Component
public class ServiceDiscovery {
    

    /**
     * 
     */
    private static final int OP_ADVERTISEMENT = 1;

    /**
     * 
     */
    private static final int OP_REMOVAL = 2;
    
    /**
     * Sent out in place of advertisements, if there would have been none
     */
    private static final int OP_HEARTBEAT = 3;

    public static class Service {
        protected int port;
        protected String name;
        
        private Service() {
        }
        
        protected Service(int port, String name) {
            this.port = port;
            this.name = name;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            Service rhs = (Service) obj;
            return rhs.port == this.port && rhs.name.equals(this.name);
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return port + name.hashCode();
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return String.format("%s on port %d", name, port);
        }
        
        /**
         * @return the port
         */
        public int getPort() {
            return port;
        }
        
        /**
         * @return the name
         */
        public String getName() {
            return name;
        }
    }
    
    public class ServiceInstance extends Service {
        private InetAddress remoteAddress;
        
        private long timeLastSeen;

        private short ttl;
        
        protected ServiceInstance(int port, String name, InetAddress remoteAddress, short ttl) {
            super(port, name);
            this.remoteAddress = remoteAddress;
            touch(ttl);
        }

        @Override
        public boolean equals(Object obj) {
            ServiceInstance rhs = (ServiceInstance) obj;
            return super.equals(obj) && this.remoteAddress.equals(rhs.remoteAddress);
        }
        
        /* (non-Javadoc)
         * @see me.itzgeoff.vidsync.common.ServiceDiscovery.Service#hashCode()
         */
        @Override
        public int hashCode() {
            return super.hashCode() + remoteAddress.hashCode();
        }
        
        /* (non-Javadoc)
         * @see me.itzgeoff.vidsync.common.ServiceDiscovery.Service#toString()
         */
        @Override
        public String toString() {
            return super.toString() + " at " + remoteAddress;
        }
        
        public void touch(short newTtl) {
            this.ttl = newTtl;
            timeLastSeen = System.currentTimeMillis();
        }
        
        /**
         * @return the remoteAddress
         */
        public InetAddress getRemoteAddress() {
            return remoteAddress;
        }

        public long getTimeLastSeen() {
            return timeLastSeen;
        }
    }


    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);

    private static final int SD_PORT = 5454;
    
    public static final int MAX_SERVICE_NAME_LENGTH = 50;

    // Size : Description
    // 8    : Our ID (to avoid feedback loops)
    // 2    : Operation, 1 = advertise, 2 = removal, ...
    // 2    : TTL (in seconds)
    // 4    : port of the service
    // 4    : port^port for protocol validation
    // 4    : length(N) of the service name string
    // N    : service name string, no more than 50 bytes
    private static final int MAX_DATAGRAM_SIZE = 21+MAX_SERVICE_NAME_LENGTH;

    protected static final long NAP_MULTIPLIER_AFTER_BAD_ADVERT = 2;

    
    @Autowired(required=false)
    private List<ServiceListener> listeners;
    
    @Value("${serviceDiscovery.heartbeatPeriod:60000}")
    private long advertisePeriod = 60000;
    
    @Value("${serviceDiscovery.ttlMultiplier:3}")
    private int ttlMultiplier = 3;
    
    /**
     * Multiplier of advertisePeriod that is used to determine if a sleep/suspend state might
     * have occurred in between invocations of the advertise timer. If so, we'll reset the
     * multicast membership to overcome OS peculiarities related to waking up.
     */
    @Value("${serviceDiscovery.sleepDetectionMultiplier:3}")
    private int sleepDetectionMultiplier = 3;

    @Value("${serviceDiscovery.failedInitSleepPeriod:360000}")
    private long failedInitSleepPeriod;

    private final long ourId = new Random().nextLong();

    private Thread thread;
    
    private boolean running;
    
    private Exception lastException;

    private DatagramChannel channel;

    private MembershipKey membershipKey;

    private InetSocketAddress multicastSdAddress;

    private Timer sdTimer;
    
    private List<Service> servicesToAdvertise = new ArrayList<>();
    
    private List<ServiceInstance> servicesObserved = new ArrayList<>();

    private TimerTask sdTimerTask;
    
    private long lastSdRunTimestamp;

    protected boolean needsReset;
    
    /**
     * 
     */
    public ServiceDiscovery() {
        sdTimer = new Timer("ServiceDiscoveryTimer", true);
    }

    @PostConstruct
    private void start() {
        thread = new Thread(new Runnable() {
            
            @Override
            public void run() {
                running = true;
                
                runLoop();
            }
        }, "ServiceDiscoveryRecv");
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    private void stop() {
        sdTimer.cancel();
        deregisterAll();
        running = false;
        thread.interrupt();
    }

    /**
     * Set up the discovery listening, etc
     * @throws IOException 
     */
    protected void init() throws IOException {
        
        NetworkInterface multicastInterface = locateMulticastInterface();
        if (multicastInterface == null) {
            throw new IOException("Unable to locate suitable multicast interface");
        }
        
        channel = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE)
                .bind(new InetSocketAddress(SD_PORT))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, multicastInterface);
        
        InetAddress multicastGroup = InetAddress.getByName("224.0.0.151");
        multicastSdAddress = new InetSocketAddress(multicastGroup, SD_PORT);
        membershipKey = channel.join(multicastGroup, multicastInterface);
        
        logger.info("Joined service discovery on {}", multicastInterface);
        
        lastSdRunTimestamp = 0;
        sdTimerTask = new TimerTask() {
            
            @Override
            public void run() {
                final long now = System.currentTimeMillis();
                
                if (lastSdRunTimestamp != 0 && (now - lastSdRunTimestamp) > advertisePeriod*sleepDetectionMultiplier) {
                    logger.debug("Detected sleep/suspend. Restarting multicast.");
                    restartMulticastMembership();
                    return;
                }
                lastSdRunTimestamp = now;
                
                try {
                    sendAdvertisements();
                } catch (Exception e) {
                    logger.error("An error occurred while sending advertisements.", e);
                    restartMulticastMembership();
                    return;
                }
                
                checkForExpirations();
            }
            

            private void restartMulticastMembership() {
                cancel(); // ...this timer task
                needsReset = true;
                thread.interrupt();
            }

        };
        
        sdTimer.schedule(sdTimerTask, 0, advertisePeriod);
    }
    
    /**
     * @throws IOException 
     * 
     */
    protected void sendAdvertisements() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(MAX_DATAGRAM_SIZE);
        if (!servicesToAdvertise.isEmpty()) {
            synchronized (servicesToAdvertise) {
                for (Service service : servicesToAdvertise) {
                    sendServiceAdvertisement(service, buffer, false);
                }
            }
        }
        else {
            sendHeartbeat(buffer);
        }
    }

    /**
     * 
     */
    private void checkForExpirations() {
        final long now = System.currentTimeMillis();
        
        Iterator<ServiceInstance> it = servicesObserved.iterator();
        while (it.hasNext()) {
            ServiceInstance instance = it.next();

            if (now - instance.ttl*1000 > instance.timeLastSeen) {
                it.remove();
                handleRemovedService(instance, true);
            }
        }
    }

    private void sendServiceAdvertisement(Service service, ByteBuffer buffer, boolean isRemoval) throws IOException {
        if (channel != null) {
            logger.trace("Advertising {}, removal={}", service, isRemoval);
            if (buffer == null) {
                buffer = ByteBuffer.allocate(MAX_DATAGRAM_SIZE);
            }
            
            buffer.clear();
            buffer.putLong(ourId);
            buffer.putShort((short) (isRemoval ? OP_REMOVAL : OP_ADVERTISEMENT));
            buffer.putShort(getTtlToSend());
            buffer.putInt(service.port);
            buffer.putInt(service.port^service.port);
            buffer.putInt(service.name.length());
            buffer.put(service.name.getBytes());
            
            buffer.flip();
            channel.send(buffer, multicastSdAddress);
        }
    }

    /**
     * @param buffer
     * @throws IOException 
     */
    private void sendHeartbeat(ByteBuffer buffer) throws IOException {
        if (channel != null) {
            logger.trace("Sending heartbeat");
            if (buffer == null) {
                buffer = ByteBuffer.allocate(MAX_DATAGRAM_SIZE);
            }
            
            buffer.clear();
            buffer.putLong(ourId);
            buffer.putShort((short) OP_HEARTBEAT);
            
            buffer.flip();
            channel.send(buffer, multicastSdAddress);
        }
    }

    /**
     * @return
     */
    private short getTtlToSend() {
        return (short) ((advertisePeriod/1000)*ttlMultiplier);
    }

    public class RegisterServiceBuilder {
        private Service service;

        private RegisterServiceBuilder(Service service) {
            this.service = service;
        }
        
        public RegisterServiceBuilder named(String name) {
            if (name.length() > MAX_SERVICE_NAME_LENGTH) {
                throw new IllegalArgumentException("Service names can only be "+MAX_SERVICE_NAME_LENGTH+" bytes long");
            }

            this.service.name = name;
            return this;
        }
        
        public RegisterServiceBuilder onPort(int port) {
            this.service.port = port;
            return this;
        }
        
        public Service done() {
            synchronized (servicesToAdvertise) {
                if (!servicesToAdvertise.contains(service)) {
                    servicesToAdvertise.add(service);
                    try {
                        sendServiceAdvertisement(service, null, false);
                    } catch (IOException e) {
                        logger.warn("Immediate advertisement of {} failed", e, service);
                    }
                }
            }

            return service;
        }
    }
    
    public RegisterServiceBuilder registerService() {
        return new RegisterServiceBuilder(new Service());
    }
    
    public void deregisterService(Service service) {
        boolean needsRemoval;
        synchronized (servicesToAdvertise) {
            needsRemoval = servicesToAdvertise.remove(service);
        }
        
        if (needsRemoval) {
            try {
                sendServiceAdvertisement(service, null, true);
            } catch (IOException e) {
                logger.error("Failed to send single service removal advertisement", e);
            }
        }
    }

    private NetworkInterface locateMulticastInterface() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            if (ni.supportsMulticast() && ni.isUp() && !ni.isLoopback()) {
                return ni;
            }
        }
        
        return null;
    }

    /**
     * Blocks in here waiting for something to happen
     * @throws IOException 
     */
    protected void receiveUpdates() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(MAX_DATAGRAM_SIZE);
        
        while (running) {
            buffer.clear();
            InetSocketAddress origin = (InetSocketAddress) channel.receive(buffer);
            buffer.flip();
            
            logger.trace("Received packet from {}", origin);
            
            byte[] content = new byte[buffer.limit()];
            int before = buffer.position();
            buffer.get(content);
            buffer.position(before);
            
            long srcId = buffer.getLong();
            if (srcId != ourId) {
                short op = buffer.getShort();
                
                short ttl = buffer.getShort();
                logger.trace("Op is {} with TTL of {}", op, ttl);
                
                int port = buffer.getInt();
                int portCheck = buffer.getInt();
                
                if ((port^port) == portCheck) {
                    logger.trace("Valid foreign message");
                    
                    int strLen = buffer.getInt();
                    byte[] strBytes = new byte[strLen];
                    buffer.get(strBytes);
                    String name = new String(strBytes);
                    
                    ServiceInstance instance = new ServiceInstance(port, name, origin.getAddress(), ttl);
                    handleObservedService(op, instance);
                }
            }
            else {
                logger.trace("It's from us, just ignoring it");
            }
        }
        
    }
    
    /**
     * @param op 
     * @param instance
     */
    private void handleObservedService(short op, ServiceInstance instance) {
        switch (op) {
            case OP_ADVERTISEMENT: {
                synchronized (servicesObserved) {
                    for (ServiceInstance previousInstance : servicesObserved) {
                        if (instance.equals(previousInstance)) {
                            previousInstance.touch(instance.ttl);
                            return;
                        }
                    }
                    
                    servicesObserved.add(instance);
                }
                
                handleNewService(instance);
                break;
            }
            
            case OP_REMOVAL: {
                boolean removed = false;

                synchronized (servicesObserved) {
                    Iterator<ServiceInstance> it = servicesObserved.iterator();
                    while (it.hasNext()) {
                        ServiceInstance previousInstance = it.next();
                        if (instance.equals(previousInstance)) {
                            it.remove();
                            removed = true;
                            break;
                        }
                    }
                }
                
                if (removed) {
                    handleRemovedService(instance, false);
                }
                break;
            }
        }
    }

    /**
     * @param instance
     */
    private void handleNewService(ServiceInstance instance) {
        if (listeners == null) {
            return;
        }
        
        for (ServiceListener listener : listeners) {
            ServiceDescription description = listener.getClass().getAnnotation(ServiceDescription.class);

            boolean matches = false;
            if (description != null) {
                for (String desiredName : description.names()) {
                    if (Pattern.matches(desiredName, instance.name)) {
                        matches = true;
                    }
                }
            }
            else {
                matches = true;
            }
            
            if (matches) {
                listener.serviceAdded(instance);
            }
        }
    }

    /**
     * @param instance
     * @param isTimeout TODO
     */
    private void handleRemovedService(ServiceInstance instance, boolean isTimeout) {
        if (listeners == null) {
            return;
        }
        
        for (ServiceListener listener : listeners) {
            ServiceDescription description = listener.getClass().getAnnotation(ServiceDescription.class);

            boolean matches = false;
            if (description != null) {
                for (String desiredName : description.names()) {
                    if (Pattern.matches(desiredName, instance.name)) {
                        matches = true;
                    }
                }
            }
            else {
                matches = true;
            }
            
            if (matches) {
                if (!isTimeout) {
                    listener.serviceRemoved(instance);
                }
                else {
                    listener.serviceTimedOut(instance);
                }
            }
        }
    }

    /**
     * Close sockets and things
     */
    protected void reset() {
        logger.debug("Reseting");
        
        if (sdTimerTask != null) {
            sdTimerTask.cancel();
            sdTimerTask = null;
        }
        
        if (membershipKey != null) {
            membershipKey.drop();
            membershipKey = null;
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                logger.debug("Unable to close ServiceDiscovery channel", e);
            }
            finally {
                channel = null;
            }
        }
    }

    private void deregisterAll() {
        logger.debug("De-registering all");
        
        synchronized (servicesToAdvertise) {
            ByteBuffer buffer = ByteBuffer.allocate(MAX_DATAGRAM_SIZE);
            for (Service service : servicesToAdvertise) {
                try {
                    sendServiceAdvertisement(service, buffer, true);
                } catch (IOException e) {
                    logger.error("Failed to advertise removal of {}", e, service);
                }
            }
            servicesToAdvertise.clear();
        }
    }
    
    /**
     * @return the running
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * @return the lastException
     */
    public Exception getLastException() {
        return lastException;
    }

    private void runLoop() {
        while (running) {
            try {
                init();
            } catch (IOException e) {
                logger.warn("Failed to initialize ServiceDiscovery. Will try again later.", e);
                lastException = e;
                
                try {
                    Thread.sleep(failedInitSleepPeriod);
                } catch (InterruptedException e1) {}
                
                continue;
            }
            
            try {
                receiveUpdates();
            } catch (ClosedByInterruptException e) {
                if (needsReset) {
                    logger.debug("Interruption requested reset");
                    needsReset = false;
                    reset();
                }
                else {
                    logger.debug("Normal closure by interrupt");
                }
                
                // clear interrupted status
                Thread.interrupted();
            } catch (Exception e) {
                logger.error("Unexpected error from worker. Resetting and trying again.", e);
                reset();
            }
        }
    }
}
