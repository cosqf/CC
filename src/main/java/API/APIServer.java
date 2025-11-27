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
import java.util.ArrayList;
import java.util.Collection;

public class APIServer implements Runnable {

    private String ip;
    private int port;
    private final Mothership mothership;

    public APIServer(String ip, int port, Mothership ms){
        this.ip = ip;
        this.port = port;
        this.mothership = ms;
    }

    @Override
    public void run() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // REST endpoints
            server.createContext("/rovers", this::handleRoverInfo);
            server.createContext("/missions/active", this::handleActiveMissions);
            server.createContext("/missions/past", this::handlePastMissions);
            server.createContext("/telemetry", this::handleLastTelemetry);

            server.start();
            System.out.println("API reachable at: http://" + this.ip + ":" + this.port);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendText(HttpExchange ex, String text) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "text/plain");
        ex.sendResponseHeaders(200, text.length()); // 200 -> HTTP Status Code for OK

        OutputStream os = ex.getResponseBody();
        os.write(text.getBytes());
        os.close();
    }

    private void handleRoverInfo(HttpExchange ex) throws IOException {
        try {
            Collection<RoverInfo> info = mothership.getRoverInfo();

            StringBuilder textBuilder = new StringBuilder();

            if(info.isEmpty()) {
                textBuilder.append("No rovers connected, check again later.\n");
            } else {
                for (RoverInfo i : info) {
                    textBuilder.append(i.toStringForAPI());
                }
            }

            String text = textBuilder.toString();

            sendText(ex, text);

        } catch (Exception e) {
            e.printStackTrace();
            ex.sendResponseHeaders(500, -1);
            ex.close();
        }
    }

    private void handleActiveMissions(HttpExchange ex) throws IOException {
        try {
            Collection<Mission> info = mothership.getActiveMissions();

            StringBuilder textBuilder = new StringBuilder();

            if(info.isEmpty()) {
                textBuilder.append("No active missions, check again later.\n");
            } else {
                for (Mission m : info) {
                    textBuilder.append(m.toStringForAPI());
                }
            }

            String text = textBuilder.toString();

            sendText(ex, text);
        } catch (Exception e) {
            e.printStackTrace();
            ex.sendResponseHeaders(500, -1);
            ex.close();
        }
    }

    private void handlePastMissions(HttpExchange ex) throws IOException {
        try {
            Collection<Mission> info = mothership.getPastMissions();

            StringBuilder textBuilder = new StringBuilder();

            if(info.isEmpty()) {
                textBuilder.append("No finished missions yet, check again later.\n");
            } else {
                for (Mission m : info) {
                    textBuilder.append(m.toStringForAPI());
                }
            }

            String text = textBuilder.toString();

            sendText(ex, text);
        } catch (Exception e) {
            e.printStackTrace();
            ex.sendResponseHeaders(500, -1);
            ex.close();
        }
    }

    private void handleLastTelemetry(HttpExchange ex) throws IOException {
        try {
            ArrayList<RoverTelemetryMessage> info = mothership.getLastTelemetry();

            StringBuilder textBuilder = new StringBuilder();

            if(info.isEmpty()){
                textBuilder.append("No telemetry messages have been sent yet, check again later.\n");
            } else {
                for (RoverTelemetryMessage i : info) {
                    if (i != null) textBuilder.append(i.toStringForAPI());
                }
            }

            String text = textBuilder.toString();

            sendText(ex, text);
        } catch (Exception e) {
            e.printStackTrace();
            ex.sendResponseHeaders(500, -1);
            ex.close();
        }
    }
}
