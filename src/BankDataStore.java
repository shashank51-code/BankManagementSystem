import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class BankDataStore {
    private final Path dataFile;

    public BankDataStore(Path dataFile) {
        this.dataFile = dataFile;
    }

    public BankData load() {
        if (!Files.exists(dataFile)) {
            return new BankData();
        }

        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(dataFile))) {
            Object saved = input.readObject();
            if (saved instanceof BankData bankData) {
                return bankData;
            }
            throw new IllegalStateException("Saved bank data has an unsupported format.");
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load saved bank data from " + dataFile, e);
        }
    }

    public void save(BankData bankData) {
        try {
            Files.createDirectories(dataFile.getParent());
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(dataFile))) {
                output.writeObject(bankData);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save bank data to " + dataFile, e);
        }
    }
}
