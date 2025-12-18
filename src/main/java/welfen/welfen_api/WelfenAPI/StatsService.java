package welfen.welfen_api.WelfenAPI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class StatsService {

    private static final File FILE = new File("stats.json");

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private long gefragt;
    private long beantwortet;
    private long messages;

    @PostConstruct
    private void init() {
        load();
    }

    public synchronized void addGefragt() {
        gefragt++;
        save();
    }

    public synchronized void addBeantwortet() {
        beantwortet++;
        save();
    }

    public synchronized void addMessage() {
        messages++;
        save();
    }

    public long getGefragt() {
        return gefragt;
    }

    public long getBeantwortet() {
        return beantwortet;
    }

    public long getMessages() {
        return messages;
    }

    private void load() {
        if (!FILE.exists()) {
            save();
            return;
        }
        try {
            StatsService loaded = mapper.readValue(FILE, StatsService.class);
            this.gefragt = loaded.gefragt;
            this.beantwortet = loaded.beantwortet;
            this.messages = loaded.messages;
        } catch (IOException e) {
            System.err.println("⚠️ Stats konnten nicht geladen werden");
            e.printStackTrace();
        }
    }

    private synchronized void save() {
        try {
            mapper.writeValue(FILE, this);
        } catch (IOException e) {
            System.err.println("⚠️ Stats konnten nicht gespeichert werden");
            e.printStackTrace();
        }
    }
}
