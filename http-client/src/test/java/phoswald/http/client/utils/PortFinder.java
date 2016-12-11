package phoswald.http.client.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class PortFinder {

    private static final Logger logger = Logger.getLogger(PortFinder.class.getName());
    private static final AtomicInteger nextCandidate = new AtomicInteger(10000);

    private int port;

    public PortFinder() {
        int reties = 50;
        while(reties-- > 0) {
            int candidate = nextCandidate.incrementAndGet();
            try(ServerSocket socket = new ServerSocket(candidate)) {
                port = candidate;
                logger.info("Using port " + port);
                return;
            } catch(IOException e) {
                logger.warning("Probing port " + candidate + " failed: " + e);
            }
        }
        throw new IllegalStateException("Failed to find an available port.");
    }

    public int port() {
        return port;
    }
}
