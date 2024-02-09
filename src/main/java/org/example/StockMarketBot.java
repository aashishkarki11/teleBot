package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;

public class StockMarketBot {
  private String TELEGRAM_BOT_TOKEN;
  private String ALPHA_VANTAGE_API_KEY;

  public StockMarketBot() {
    loadProperties();
  }

  public static void main(String[] args) {
    try {
      new StockMarketBot().start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadProperties() {
    try (InputStream inputStream = getClass().getClassLoader()
        .getResourceAsStream("Application.properties")) {
      Properties properties = new Properties();
      if (inputStream != null) {
        properties.load(inputStream);
        TELEGRAM_BOT_TOKEN = properties.getProperty("BOT_TOKEN");
        ALPHA_VANTAGE_API_KEY = properties.getProperty("ALPHA_VANTAGE_API_KEY");
      } else {
        throw new FileNotFoundException(
            "Property file 'Application.properties' not found in the classpath.");
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error loading properties", e);
    }
  }

  public void start() {
    while (true) {
      try {
        getUpdates();
        Thread.sleep(100000);
      } catch (Exception e) {
        e.printStackTrace();
        break;
      }
    }
  }

  public void getUpdates() {
    try {
      URL url = new URL(
          "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/getUpdates");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");

      try (InputStream inputStream = connection.getInputStream();
          Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
        StringBuilder response = new StringBuilder();
        while (scanner.hasNextLine()) {
          response.append(scanner.nextLine());
        }

        String responseBody = response.toString();
        JSONObject jsonObject = new JSONObject(responseBody);
        JSONArray resultArray = jsonObject.getJSONArray("result");

        for (int i = 0; i < resultArray.length(); i++) {
          JSONObject resultObj = resultArray.getJSONObject(i);
          JSONObject messageObj = resultObj.getJSONObject("message");
          JSONObject chatId = messageObj.getJSONObject("chat");
          int id = chatId.getInt("id");
          String text = messageObj.getString("text");

          if ("/quote".equals(text)) {
            String stockQuote = getStockQuote("IBM");
            sendMessage(id, stockQuote);
          }
          if ("/ibm".equals(text)) {
            String stockQuote = getIBMData("IBM");
            sendMessage(id, stockQuote);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void sendMessage(int chatId, String text) {
    try {
      URL url = new URL(
          "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setDoOutput(true);

      String requestBody = String.format("{\"chat_id\": \"%s\", \"text\": \"%s\"}",
          chatId, text);

      try (OutputStream outputStream = connection.getOutputStream()) {
        outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
      }

      try (InputStream inputStream = connection.getInputStream()) {
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
          while (scanner.hasNextLine()) {
            System.out.println(scanner.nextLine());
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //with api key
  public String getStockQuote(String symbol) {
    try {

      //deployed url
      String apiUrl = String.format(
          "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
          symbol, ALPHA_VANTAGE_API_KEY);

      URL url = new URL(apiUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");

      try (InputStream inputStream = connection.getInputStream();
          Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
        StringBuilder response = new StringBuilder();
        while (scanner.hasNextLine()) {
          response.append(scanner.nextLine());
        }

        JSONObject jsonObject = new JSONObject(response.toString());
        if (jsonObject.has("Global Quote")) {
          JSONObject globalQuote = jsonObject.getJSONObject("Global Quote");
          String retrievedSymbol = globalQuote.getString("01. symbol");
          String price = globalQuote.getString("05. price");
          String changePercent = globalQuote.getString("10. change percent");
          return String.format("Symbol: %s, Price: %s, Change Percent: %s",
              retrievedSymbol, price, changePercent);
        } else {
          return "Invalid symbol or data not available";
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      return "Error retrieving stock quote.";
    }
  }

  //demo stock api with API key
  public String getIBMData(String symbol) throws IOException {

    // for demo testing
    String apiUrl = String.format(
        "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=%s&interval=5min&apikey=demo",
        symbol);

    URL url = new URL(apiUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");

    // Read the response
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(connection.getInputStream()));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      response.append(line);
    }
    reader.close();

    JSONObject jsonResponse = new JSONObject(response.toString());
    JSONObject timeSeries = jsonResponse.getJSONObject("Time Series (5min)");
    String latestDataKey = timeSeries.keys().next();
    JSONObject latestData = timeSeries.getJSONObject(latestDataKey);
    return latestData.getString("4. close");
  }
}
