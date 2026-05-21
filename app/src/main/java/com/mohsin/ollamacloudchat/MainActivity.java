package com.mohsin.ollamacloudchat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private EditText apiKeyInput;
    private EditText baseUrlInput;
    private EditText manualModelInput;
    private EditText messageInput;
    private Spinner modelSpinner;
    private Button sendButton;
    private Button refreshButton;
    private LinearLayout chatBox;
    private ScrollView scrollView;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ArrayList<ChatMessage> messages = new ArrayList<>();
    private final ArrayList<String> models = new ArrayList<>();
    private ArrayAdapter<String> modelAdapter;
    private SharedPreferences prefs;

    private static final String DEFAULT_BASE_URL = "https://ollama.com/api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("ollama_cloud_chat_settings", MODE_PRIVATE);
        buildUi();
        loadSavedSettings();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(10));
        root.setBackgroundColor(Color.parseColor("#F8FAFC"));
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("Ollama Cloud Chat");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#0F172A"));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Use your Ollama API key, load cloud models, and chat from Android.");
        subtitle.setTextSize(13);
        subtitle.setTextColor(Color.parseColor("#475569"));
        subtitle.setPadding(0, dp(3), 0, dp(10));
        root.addView(subtitle);

        apiKeyInput = new EditText(this);
        apiKeyInput.setHint("Ollama API Key");
        apiKeyInput.setSingleLine(true);
        apiKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        styleInput(apiKeyInput);
        root.addView(apiKeyInput, matchWrap());

        baseUrlInput = new EditText(this);
        baseUrlInput.setHint("Base URL");
        baseUrlInput.setSingleLine(true);
        baseUrlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        styleInput(baseUrlInput);
        root.addView(baseUrlInput, matchWrap());

        LinearLayout modelRow = new LinearLayout(this);
        modelRow.setOrientation(LinearLayout.HORIZONTAL);
        modelRow.setGravity(Gravity.CENTER_VERTICAL);
        modelRow.setPadding(0, dp(6), 0, dp(6));
        root.addView(modelRow, matchWrap());

        modelSpinner = new Spinner(this);
        modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, models);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(modelAdapter);
        modelRow.addView(modelSpinner, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        refreshButton = new Button(this);
        refreshButton.setText("Load Models");
        refreshButton.setAllCaps(false);
        modelRow.addView(refreshButton);

        manualModelInput = new EditText(this);
        manualModelInput.setHint("Manual model name (optional, e.g. gpt-oss:20b-cloud)");
        manualModelInput.setSingleLine(true);
        manualModelInput.setInputType(InputType.TYPE_CLASS_TEXT);
        styleInput(manualModelInput);
        root.addView(manualModelInput, matchWrap());

        TextView manualHint = new TextView(this);
        manualHint.setText("If model loading fails, type a model name here. Manual model name will be used first.");
        manualHint.setTextSize(12);
        manualHint.setTextColor(Color.parseColor("#64748B"));
        manualHint.setPadding(dp(4), 0, dp(4), dp(6));
        root.addView(manualHint);

        scrollView = new ScrollView(this);
        chatBox = new LinearLayout(this);
        chatBox.setOrientation(LinearLayout.VERTICAL);
        chatBox.setPadding(0, dp(8), 0, dp(8));
        scrollView.addView(chatBox);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        LinearLayout sendRow = new LinearLayout(this);
        sendRow.setOrientation(LinearLayout.HORIZONTAL);
        sendRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(sendRow, matchWrap());

        messageInput = new EditText(this);
        messageInput.setHint("Write your message...");
        messageInput.setMinLines(1);
        messageInput.setMaxLines(5);
        styleInput(messageInput);
        sendRow.addView(messageInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        sendButton = new Button(this);
        sendButton.setText("Send");
        sendButton.setAllCaps(false);
        sendRow.addView(sendButton);

        refreshButton.setOnClickListener(v -> loadModels());
        sendButton.setOnClickListener(v -> sendMessage());
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < models.size()) {
                    prefs.edit().putString("model", models.get(position)).apply();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        addBubble("assistant", "Paste your Ollama API key, tap Load Models, choose a model, then chat. If model list does not load, type the model name manually. Default cloud base URL is https://ollama.com/api");
    }

    private void loadSavedSettings() {
        apiKeyInput.setText(prefs.getString("api_key", ""));
        baseUrlInput.setText(prefs.getString("base_url", DEFAULT_BASE_URL));
        manualModelInput.setText(prefs.getString("manual_model", ""));
        String savedModel = prefs.getString("model", "gpt-oss:20b-cloud");
        models.clear();
        models.add(savedModel);
        modelAdapter.notifyDataSetChanged();
    }

    private void saveSettings() {
        prefs.edit()
                .putString("api_key", apiKeyInput.getText().toString().trim())
                .putString("base_url", normalizeBaseUrl(baseUrlInput.getText().toString().trim()))
                .putString("manual_model", manualModelInput.getText().toString().trim())
                .apply();
    }

    private void loadModels() {
        saveSettings();
        setBusy(true, "Loading...");
        executor.execute(() -> {
            try {
                String baseUrl = normalizeBaseUrl(baseUrlInput.getText().toString().trim());
                String response = request("GET", baseUrl + "/tags", null);
                JSONObject json = new JSONObject(response);
                JSONArray arr = json.optJSONArray("models");
                if (arr == null || arr.length() == 0) {
                    throw new Exception("No models found from /api/tags.");
                }

                List<String> loaded = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    String name = item.optString("name", "").trim();
                    if (!name.isEmpty()) loaded.add(name);
                }

                runOnUiThread(() -> {
                    models.clear();
                    models.addAll(loaded);
                    modelAdapter.notifyDataSetChanged();
                    restoreSelectedModel();
                    addBubble("assistant", "Loaded " + loaded.size() + " model(s). Choose one and start chatting.");
                    setBusy(false, "Load Models");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    addBubble("error", "Model loading failed: " + readableError(e));
                    setBusy(false, "Load Models");
                });
            }
        });
    }

    private void restoreSelectedModel() {
        String savedModel = prefs.getString("model", "");
        for (int i = 0; i < models.size(); i++) {
            if (models.get(i).equals(savedModel)) {
                modelSpinner.setSelection(i);
                return;
            }
        }
    }

    private void sendMessage() {
        saveSettings();
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;
        String selectedModel = getActiveModelName();
        if (selectedModel.isEmpty()) {
            toast("Load/select a model or type model name manually.");
            return;
        }

        hideKeyboard();
        messageInput.setText("");
        messages.add(new ChatMessage("user", text));
        addBubble("user", text);
        TextView pending = addBubble("assistant", "Thinking...");
        sendButton.setEnabled(false);

        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("model", selectedModel);
                body.put("stream", false);

                JSONArray messageArray = new JSONArray();
                for (ChatMessage m : messages) {
                    JSONObject obj = new JSONObject();
                    obj.put("role", m.role);
                    obj.put("content", m.content);
                    messageArray.put(obj);
                }
                body.put("messages", messageArray);

                String baseUrl = normalizeBaseUrl(baseUrlInput.getText().toString().trim());
                String response = request("POST", baseUrl + "/chat", body.toString());
                JSONObject json = new JSONObject(response);
                JSONObject messageObj = json.optJSONObject("message");
                String answer = messageObj == null ? "" : messageObj.optString("content", "");
                if (answer.trim().isEmpty()) {
                    answer = json.optString("response", "");
                }
                if (answer.trim().isEmpty()) {
                    throw new Exception("Empty response: " + response);
                }

                String finalAnswer = answer.trim();
                messages.add(new ChatMessage("assistant", finalAnswer));
                runOnUiThread(() -> {
                    pending.setText(finalAnswer);
                    scrollBottom();
                    sendButton.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pending.setText("Request failed: " + readableError(e));
                    pending.setTextColor(Color.parseColor("#7F1D1D"));
                    sendButton.setEnabled(true);
                });
            }
        });
    }

    private String getActiveModelName() {
        String manualModel = manualModelInput == null ? "" : manualModelInput.getText().toString().trim();
        if (!manualModel.isEmpty()) {
            return manualModel;
        }
        Object selected = modelSpinner == null ? null : modelSpinner.getSelectedItem();
        return selected == null ? "" : selected.toString().trim();
    }

    private String request(String method, String urlString, String body) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(120000);
        connection.setRequestProperty("Content-Type", "application/json");

        String apiKey = apiKeyInput.getText().toString().trim();
        if (!apiKey.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }

        if (body != null) {
            connection.setDoOutput(true);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bytes);
            }
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String response = readAll(stream);
        connection.disconnect();

        if (status < 200 || status >= 300) {
            throw new Exception("HTTP " + status + " - " + response);
        }
        return response;
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String normalizeBaseUrl(String value) {
        String url = value == null || value.trim().isEmpty() ? DEFAULT_BASE_URL : value.trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/api")) return url;
        return url + "/api";
    }

    private TextView addBubble(String role, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setLineSpacing(2, 1.05f);
        view.setPadding(dp(12), dp(10), dp(12), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(6), 0, dp(6));

        if ("user".equals(role)) {
            bg.setColor(Color.parseColor("#2563EB"));
            view.setTextColor(Color.WHITE);
            params.gravity = Gravity.END;
            params.leftMargin = dp(42);
        } else if ("error".equals(role)) {
            bg.setColor(Color.parseColor("#FEE2E2"));
            view.setTextColor(Color.parseColor("#7F1D1D"));
            params.gravity = Gravity.START;
            params.rightMargin = dp(42);
        } else {
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
            view.setTextColor(Color.parseColor("#0F172A"));
            params.gravity = Gravity.START;
            params.rightMargin = dp(42);
        }

        view.setBackground(bg);
        chatBox.addView(view, params);
        scrollBottom();
        return view;
    }

    private void styleInput(EditText input) {
        input.setTextSize(14);
        input.setPadding(dp(12), dp(9), dp(12), dp(9));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.parseColor("#CBD5E1"));
        input.setBackground(bg);
    }

    private LinearLayout.LayoutParams matchWrap() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(4), 0, dp(4));
        return params;
    }

    private void setBusy(boolean busy, String refreshText) {
        refreshButton.setEnabled(!busy);
        refreshButton.setText(refreshText);
    }

    private void scrollBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(messageInput.getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    private String readableError(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.trim().isEmpty()) return e.toString();
        return msg.length() > 700 ? msg.substring(0, 700) + "..." : msg;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private static class ChatMessage {
        final String role;
        final String content;

        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
