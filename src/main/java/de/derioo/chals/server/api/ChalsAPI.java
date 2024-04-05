package de.derioo.chals.server.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import de.derioo.chals.server.api.types.Mod;
import org.json.simple.JSONArray;
import oshi.util.tuples.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ChalsAPI implements Api {

  private Pair<Long, Set<Mod>> cachedMods = new Pair<>(0L, new HashSet<>());

  @Override
  public Set<Mod> mods() throws IOException {
    List<String> modNames = new ArrayList<>();

    URL url = new URL("http://127.0.0.1:3000/mods");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");

    int responseCode = connection.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_OK) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
      reader.close();


      JsonArray jsonArray = JsonParser.parseString(response.toString()).getAsJsonArray();
      for (int i = 0; i < jsonArray.size(); i++) {
        modNames.add(jsonArray.get(i).getAsString());
      }
    } else {
      throw new IOException("Failed to fetch data from API. Response code: " + responseCode);
    }

    connection.disconnect();

    Set<Mod> mods = modNames.stream().map(Mod::new).collect(Collectors.toSet());
    cachedMods = new Pair<>(System.currentTimeMillis(), mods);
    return mods;
  }

  @Override
  public Set<Mod> getCachedMods()  {
    if (cachedMods.getA() + 5000L > System.currentTimeMillis()) return cachedMods.getB();

    new Thread(() -> {
        try {
            mods();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }).start();

    return cachedMods.getB();
  }
}
