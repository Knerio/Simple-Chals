package de.derioo.chals.server.api.types;

import de.derioo.chals.server.api.Unsafe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;

@Getter
@AllArgsConstructor
public class Mod {

  private final String name;


  public void load() throws IOException, InvalidPluginException, InvalidDescriptionException {
    if (!isDownloaded()) download();
    File downloadDir = getDownloadDir();
    File jar = new File(downloadDir, name + ".jar");
    FileUtils.copyFile(jar, new File("./plugins"));
    Plugin plugin = Bukkit.getPluginManager().loadPlugin(new File("./plugins", name + ".jar"));

    Bukkit.getPluginManager().loadPlugin(new File("./plugins", name + ".jar"));
    Bukkit.getPluginManager().enablePlugin(Objects.requireNonNull(plugin));
  }

  @NotNull
  private static File getDownloadDir() {
    File downloadDir = new File("./plugins/SimpleChals/downloads");
    downloadDir.mkdirs();
    return downloadDir;
  }

  public boolean isDownloaded() {
    File downloadDir = getDownloadDir();
    return new File(downloadDir, name + ".jar").exists();
  }

  public void download() {
    File downloadDir = getDownloadDir();
    try {
      String serverUrl = "https://127.0.0.1:8000/mod/" + this.name + ".jar";

      URL url = new URL(serverUrl);
      HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
      connection.connect();

      try (InputStream inputStream = connection.getInputStream()) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(downloadDir.toPath() + "/" + this.name + ".jar");) {

          byte[] buffer = new byte[4096];
          int bytesRead;
          while ((bytesRead = inputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
          }

          fileOutputStream.close();
          inputStream.close();
          connection.disconnect();
        }
      }



      System.out.println("Jar file downloaded successfully");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
