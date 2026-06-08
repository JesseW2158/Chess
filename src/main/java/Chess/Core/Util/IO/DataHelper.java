package Chess.Core.Util.IO;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.scene.image.Image;

public class DataHelper {
    public static Image loadImage(String resource, int width, int height) {
        return new Image(DataHelper.class.getClassLoader().getResourceAsStream(resource), width, height, true, true);
    }

    public static byte[] loadDataFromFile(File file) {
        try {
            return Files.readAllBytes(Paths.get(file.toURI()));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static byte[] loadDataFromResource(String resource) {
        URI uri = null;
        try {
            URL url = ChessIO.class.getClassLoader().getResource(resource);
            if (url != null) {
                uri = url.toURI();
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if (uri == null) {
            return null;
        }
        try {
            return Files.readAllBytes(Paths.get(uri));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void saveDataToFile(byte[] data, File file) {
        try {
            Files.write(Paths.get(file.toURI()), data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
