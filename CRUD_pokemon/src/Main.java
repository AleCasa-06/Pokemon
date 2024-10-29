import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jakewharton.fliptables.FlipTable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;


public class Main {
    public static void main(String[] args) throws SQLException, URISyntaxException, IOException, InterruptedException {

        String urlDB = "jdbc:mysql://localhost:3306/pokemon_casarotto";
        String user = "root";
        String password = "";

        Connection conn = DriverManager.getConnection(urlDB, user, password);
        System.out.println("Connesso con successo!");
        Statement stmt = conn.createStatement();



        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.print("Inserisci 1 per vedere il db, inserisci 2 per aggiungere un pokemon, 3 per uscire: ");
            int scelta = scanner.nextInt();

            if (scelta == 1) {
                String query = "SELECT * FROM dati";
                ResultSet rs = stmt.executeQuery(query);

                String[] headers = {"ID", "Nome", "Tipo1", "Tipo2"};
                List<String[]> dataList = new ArrayList<>();

                while (rs.next()) {
                    String[] row = {
                            rs.getString("ID"),
                            rs.getString("Nome"),
                            rs.getString("Tipo1"),
                            rs.getString("Tipo2")
                    };
                    dataList.add(row);
                }

// Converti la lista in un array
                String[][] data = dataList.toArray(new String[0][]);

                System.out.println(FlipTable.of(headers, data));
                rs.close();


            } else if(scelta == 2) {
                System.out.print("Inserisci il nome del pokemon: ");
                String nome = scanner.next();


                try {
                    String urlAPI = "https://pokeapi.co/api/v2/pokemon/" + nome;

                    HttpClient client = HttpClient.newHttpClient();


                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(new URI(urlAPI))
                            .GET()
                            .build();


                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    String responseBody = response.body();

                    // Convertire JSON in oggetto Java usando Gson
                    Gson gson = new Gson();
                    APIResponse apiResponse = gson.fromJson(responseBody, APIResponse.class);

                    List<APIResponse.Types> typesList = apiResponse.getTypes();
                    String[] types = {"", ""};

                    if (typesList != null) {
                        for (int i = 0; i < typesList.size(); i++) {
                            if (typesList.get(i).getType().getName() != null) {
                                types[i] = typesList.get(i).getType().getName();
                            }
                        }
                    }

                    String query = "INSERT INTO dati (ID, Nome, Tipo1, Tipo2) VALUES (?, ?, ?, ?)";
                    PreparedStatement statement = conn.prepareStatement(query);


                    statement.setInt(1, apiResponse.getId());
                    statement.setString(2, apiResponse.getNome());
                    statement.setString(3, types[0]);
                    statement.setString(4, types[1]);


                    int rowsInserted = statement.executeUpdate();



                    String apiKey = "TwV1glXVdGyn66s301s0Fdj67UZk5nnU"; // Inserisci la tua chiave API qui
                    String searchQuery = apiResponse.getNome(); // Modifica la query di ricerca come desideri

                    SwingUtilities.invokeLater(() -> {
                        JFrame frame = new JFrame("Giphy GIF Viewer");
                        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        frame.setSize(500, 500);
                        frame.setLayout(new BorderLayout());

                        JLabel label = new JLabel("Loading GIF...", SwingConstants.CENTER);
                        frame.add(label, BorderLayout.CENTER);
                        frame.setVisible(true);

                        new Thread(() -> {
                            try {
                                String gifUrl = getGifUrl(apiKey, searchQuery);
                                if (gifUrl != null) {
                                    BufferedImage gifImage = ImageIO.read(new URL(gifUrl));
                                    label.setIcon(new ImageIcon(gifImage));
                                    label.setText(null); // Rimuove il testo di caricamento
                                } else {
                                    label.setText("No GIF found.");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                label.setText("Error loading GIF.");
                            }
                        }).start();
                    });


                } catch (Exception e) {

                    System.out.println(e.getMessage());

                }


            }
            else{
                stmt.close();
                conn.close();
                break;
            }

        }




        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.



    }
    private static String getGifUrl(String apiKey, String searchQuery) throws Exception {
        String urlString = "https://api.giphy.com/v1/gifs/search?api_key=" + apiKey + "&q=" + searchQuery + "&limit=1";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Utilizza Gson per il parsing
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
        JsonArray gifs = jsonResponse.getAsJsonArray("data");

        if (gifs.size() > 0) {
            return gifs.get(0).getAsJsonObject().getAsJsonObject("images").getAsJsonObject("original").get("url").getAsString();
        }

        return null;
    }

}