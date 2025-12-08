package API;

import Message.RoverTelemetryMessage;
import Mission.Mission;
import Mothership.RoverInfo;
import Mothership.Mothership;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class APIServer implements Runnable {

    private String ip;
    private int port;
    private final Mothership mothership;

    public APIServer(String ip, int port, Mothership ms) {
        this.ip = ip;
        this.port = port;
        this.mothership = ms;
    }

    @Override
    public void run() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(ip, port), 0);

            // REST endpoints
            server.createContext("/rovers", this::handleRoverInfo);
            server.createContext("/missions/active", this::handleActiveMissions);
            server.createContext("/missions/past", this::handlePastMissions);
            server.createContext("/missions/future", this::handleFutureMissions);
            server.createContext("/telemetry", this::handleLastTelemetry);

            server.start();
            System.out.println("API reachable at: http://" + this.ip + ":" + this.port);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendText(HttpExchange ex, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);

        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private <T> void handleCollection(HttpExchange ex, Collection<T> data, String emptyMessage,
                                      java.util.function.Function<T, String> formatter) throws IOException {
        try {
            StringBuilder sb = new StringBuilder();

            if (data.isEmpty()) {
                sb.append(emptyMessage);
            } else {
                for (T item : data) {
                    sb.append(formatter.apply(item));
                }
            }
            sendText(ex, sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            ex.sendResponseHeaders(500, -1);
            ex.close();
        }
    }


    private void handleRoverInfo(HttpExchange ex) throws IOException {
        handleCollection(ex,
                mothership.getRoverInfo(),
                "No rovers connected, check again later.\n",
                RoverInfo::toStringForAPI
        );
    }

    private void handleActiveMissions(HttpExchange ex) throws IOException {
        handleCollection(ex,
                mothership.getActiveMissions(),
                "No active missions, check again later.\n",
                Mission::toStringForAPI
        );
    }

    private void handlePastMissions(HttpExchange ex) throws IOException {
        handleCollection(ex,
                mothership.getPastMissions(),
                "No finished missions yet, check again later.\n",
                Mission::toStringForAPI
        );
    }

    private void handleFutureMissions(HttpExchange ex) throws IOException {
        handleCollection(ex,
                mothership.getFutureMissions(),
                "No future missions queued, check again later.\n",
                Mission::toStringForAPI
        );
    }

    private void handleLastTelemetry(HttpExchange ex) throws IOException {
        handleCollection(ex,
                mothership.getLastTelemetry(),
                "No telemetry messages have been sent yet, check again later.\n",
                RoverTelemetryMessage::toStringForAPI
        );
    }
}