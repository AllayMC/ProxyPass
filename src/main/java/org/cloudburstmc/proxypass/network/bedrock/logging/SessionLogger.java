package org.cloudburstmc.proxypass.network.bedrock.logging;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.proxypass.ProxyPass;
import org.jose4j.json.internal.json_simple.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


@Log4j2
public class SessionLogger {

    private static final String PATTERN_FORMAT = "HH:mm:ss:SSS";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN_FORMAT)
            .withZone(ZoneId.systemDefault());
    private static final String LOG_FORMAT = "[%s] [%s] - %s";

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final ProxyPass proxy;

    private final Path dataPath;

    private final Path logPath;

    private final Deque<String> logBuffer = new ArrayDeque<>();

    public SessionLogger(ProxyPass proxy, Path sessionsDir, String displayName, long timestamp) {
        this.proxy = proxy;
        this.dataPath = sessionsDir.resolve(displayName + '-' + timestamp);
        this.logPath = dataPath.resolve("packets.log");
    }

    public void start() {
        if (proxy.getConfiguration().isLoggingPackets()) {
            if (proxy.getConfiguration().getLogTo().logToFile) {
                log.debug("Packets will be logged under " + logPath.toString());
                try {
                    Files.createDirectories(dataPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            executor.scheduleAtFixedRate(this::flushLogBuffer, 5, 5, TimeUnit.SECONDS);
        }
    }

    public void saveImage(String name, BufferedImage image) {
        Path path = dataPath.resolve(name + ".png");
        try (OutputStream stream = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ImageIO.write(image, "png", stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveJson(String name, JSONObject object) throws IOException {
        ObjectWriter jsonout = ProxyPass.JSON_MAPPER.writer(new DefaultPrettyPrinter());
        jsonout.writeValue(new FileOutputStream(logPath.getParent().resolve(name + ".json").toFile()), object);
    }

    public void saveJson(String name, JsonNode node) throws IOException {
        ObjectWriter jsonout = ProxyPass.JSON_MAPPER.writer(new DefaultPrettyPrinter());
        jsonout.writeValue(new FileOutputStream(logPath.getParent().resolve(name + ".json").toFile()), node);
    }

    public void saveJson(String name, byte[] encodedJsonString) {
        Path geometryPath = dataPath.resolve(name + ".json");
        try {
            Files.write(geometryPath, encodedJsonString, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Vector3i.class, new JsonSerializer<Vector3i>() {
            @Override
            public void serialize(Vector3i vector3i, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
                gen.writeStartObject();
                gen.writeNumberField("x", vector3i.getX());
                gen.writeNumberField("y", vector3i.getY());
                gen.writeNumberField("z", vector3i.getZ());
                gen.writeEndObject();
            }
        });
        MAPPER.registerModule(simpleModule);
    }

    private static final ObjectWriter WRITER = MAPPER.writer(new DefaultPrettyPrinter());

    public void logPacket(BedrockSession session, BedrockPacket packet, boolean upstream) {
        String logPrefix = getLogPrefix(upstream);
        if (!proxy.isIgnoredPacket(packet.getClass())) {
            if (session.isLogging() && log.isTraceEnabled()) {
                log.trace("{} {}: {}", logPrefix, session.getSocketAddress(), packet);
            }
            String logMessage;
            if (proxy.getConfiguration().isLogToJson()) {
                try {
                    String s = WRITER.writeValueAsString(packet);
                    logMessage = String.format(LOG_FORMAT, FORMATTER.format(Instant.now()), logPrefix, s);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            } else {
                logMessage = String.format(LOG_FORMAT, FORMATTER.format(Instant.now()), logPrefix, packet);
            }
            if (proxy.getConfiguration().isLoggingPackets()) {
                logToBuffer(() -> logMessage);
            }

            if (proxy.getConfiguration().isLoggingPackets() && proxy.getConfiguration().getLogTo().logToConsole) {
                System.out.println(logMessage);
            }
        }
    }

    private String getLogPrefix(boolean upstream) {
        return upstream ? "SERVER BOUND" : "CLIENT BOUND";
    }

    private void logToBuffer(Supplier<String> supplier) {
        synchronized (logBuffer) {
            logBuffer.addLast(supplier.get());
        }
    }

    private void flushLogBuffer() {
        synchronized (logBuffer) {
            try {
                if (proxy.getConfiguration().getLogTo().logToFile) {
                    Files.write(logPath, logBuffer, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                }
                logBuffer.clear();
            } catch (IOException e) {
                log.error("Unable to flush packet log", e);
            }
        }
    }

}
