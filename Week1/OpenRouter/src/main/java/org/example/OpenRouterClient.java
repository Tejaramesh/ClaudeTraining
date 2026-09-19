package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class OpenRouterClient {
    private static final String ENV_FILE = ".env";
    private static final String API_KEY_VAR = "OPENROUTER_API_KEY";
    private static final String BASE_URL = "https://openrouter.ai/api/v1";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        try {
            boolean jsonFlag = Arrays.asList(args).contains("--json");

            String apiKey = loadApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("Error: OPENROUTER_API_KEY not configured");
                System.exit(1);
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .build();

            String authKeyJson = callEndpoint(client, apiKey, "/auth/key");
            if (authKeyJson == null) {
                System.exit(2);
            }

            String creditsJson = callEndpoint(client, apiKey, "/credits");
            if (creditsJson == null) {
                System.exit(2);
            }

            if (jsonFlag) {
                JsonObject authData = gson.fromJson(authKeyJson, JsonObject.class).getAsJsonObject("data");
                JsonObject merged = new JsonObject();
                for (String key : authData.keySet()) {
                    merged.add(key, authData.get(key));
                }
                System.out.println(gson.toJson(merged));
            } else {
                printReport(authKeyJson, creditsJson);
            }

            System.exit(0);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String loadApiKey() {
        try {
            Path envPath = Path.of(ENV_FILE);
            if (!Files.exists(envPath)) {
                return null;
            }
            String content = Files.readString(envPath);
            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.startsWith(API_KEY_VAR + "=")) {
                    return line.substring(API_KEY_VAR.length() + 1);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading .env file: " + e.getMessage());
        }
        return null;
    }

    private static String callEndpoint(HttpClient client, String apiKey, String endpoint) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Special case: 403 on /credits is expected and documented
            if (response.statusCode() == 403 && endpoint.equals("/credits")) {
                System.out.println("Info: Only management keys can fetch credits for an account");
                return "{}";
            }

            // Handle errors
            if (response.statusCode() == 401) {
                System.err.println("Error: Invalid API key");
                return null;
            }
            if (response.statusCode() == 403) {
                System.err.println("Error: Access denied");
                return null;
            }
            if (response.statusCode() == 429) {
                System.err.println("Error: Rate limit exceeded");
                return null;
            }
            if (response.statusCode() >= 500) {
                System.err.println("Error: Server error (HTTP " + response.statusCode() + ")");
                return null;
            }

            if (response.statusCode() != 200) {
                System.err.println("Error: HTTP " + response.statusCode());
                return null;
            }

            return response.body();

        } catch (IOException e) {
            System.err.println("Error: Connection failed - " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            System.err.println("Error: Request interrupted");
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static void printReport(String authKeyJson, String creditsJson) {
        JsonObject data = gson.fromJson(authKeyJson, JsonObject.class).getAsJsonObject("data");

        System.out.println("Key identity");
        System.out.println("  Label:                    " + getString(data, "label"));
        System.out.println("  Is management key:       " + getBoolean(data, "is_management_key"));
        System.out.println("  Is provisioning key:      " + getBoolean(data, "is_provisioning_key"));

        System.out.println();
        System.out.println("Limits and balance");
        System.out.println("  Limit:                    " + formatMoney(getDouble(data, "limit")));
        System.out.println("  Limit remaining:          " + formatMoney(getDouble(data, "limit_remaining")));
        System.out.println("  Limit reset:              " + (data.has("limit_reset") && !data.get("limit_reset").isJsonNull() ? data.get("limit_reset").getAsString() : "—"));

        System.out.println();
        System.out.println("Usage");
        System.out.println("  Total:                    " + formatMoney(getDouble(data, "usage")));
        System.out.println("  Daily:                    " + formatMoney(getDouble(data, "usage_daily")));
        System.out.println("  Weekly:                   " + formatMoney(getDouble(data, "usage_weekly")));
        System.out.println("  Monthly:                  " + formatMoney(getDouble(data, "usage_monthly")));
        System.out.println("  BYOK total:               " + formatMoney(getDouble(data, "byok_usage")));
        System.out.println("  BYOK daily:               " + formatMoney(getDouble(data, "byok_usage_daily")));
        System.out.println("  BYOK weekly:              " + formatMoney(getDouble(data, "byok_usage_weekly")));
        System.out.println("  BYOK monthly:             " + formatMoney(getDouble(data, "byok_usage_monthly")));

        System.out.println();
        System.out.println("Free-tier allowance");
        if (data.has("free_model_daily_requests") && !data.get("free_model_daily_requests").isJsonNull()) {
            JsonObject freeModels = data.getAsJsonObject("free_model_daily_requests");
            System.out.println("  Used:                     " + formatMoney(getDouble(freeModels, "used")));
            System.out.println("  Limit:                    " + formatMoney(getDouble(freeModels, "limit")));
            System.out.println("  Remaining:                " + formatMoney(getDouble(freeModels, "remaining")));
        } else {
            System.out.println("  Used:                     —");
            System.out.println("  Limit:                    —");
            System.out.println("  Remaining:                —");
        }

        System.out.println();
        System.out.println("Account credits");
        System.out.println("  Balance:                  (requires management key)");
    }

    private static String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "—";
    }

    private static boolean getBoolean(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() && obj.get(key).getAsBoolean();
    }

    private static double getDouble(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsDouble();
        }
        return 0.0;
    }

    private static String formatMoney(double amount) {
        return String.format("$%.4f", amount);
    }
}
