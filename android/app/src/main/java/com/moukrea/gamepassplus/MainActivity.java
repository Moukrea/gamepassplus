package com.moukrea.gamepassplus;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Scanner;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private WebView GamePassWebView;
    private Dialog promptDialog;
    private boolean useBetterXcloud = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setup();

        checkNetwork(false);
    }

    /**
     * Checks if device has network access
     * Shows dialog if not
     * Moves to showAcknowledgement if yes
     *
     * @param retry Will behave differently if set to true
     */
    private void checkNetwork(boolean retry) {
        if (isNetworkAvailable()) {
            dismissDialog();
            showAcknowledgements();
        } else if (retry) {
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            promptDialog.findViewById(R.id.scrollView).startAnimation(shake);
        } else {
            String titleText = getString(R.string.error_occurred);
            int imageResource = R.drawable.no_network;
            String messageText = getString(R.string.no_network_access);
            String primaryButtonText = getString(R.string.retry);
            View.OnClickListener primaryButtonAction = view -> checkNetwork(true);
            String secondaryButtonText = getString(R.string.exit);
            View.OnClickListener secondaryButtonAction = view -> finish();

            showDialog(
                titleText,
                imageResource,
                messageText,
                primaryButtonText,
                primaryButtonAction,
                secondaryButtonText,
                secondaryButtonAction
            );
        }
    }

    /**
     * Shows Acknowledgements if never displayed (first run)
     * Waits for user to agree then move to installBetterXcloud
     */
    private void showAcknowledgements() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        if (prefs.getBoolean("AgreedToAcknowledgements", false)) {
            installBetterXcloud();
        } else {
            String titleText = getString(R.string.acknowledgements);
            String messageText = getString(R.string.credits);
            String primaryButtonText = getString(R.string.agreed_thanks);
            View.OnClickListener primaryButtonAction = view -> {
                prefs.edit().putBoolean("AgreedToAcknowledgements", true).apply();
                dismissDialog();
                installBetterXcloud();
            };

            showDialog(
                titleText,
                0,
                messageText,
                primaryButtonText,
                primaryButtonAction,
                "",
                null
            );
        }
    }

    /**
     * Checks if Better xCloud is installed locally
     * Downloads it if not (show dialog)
     * If successful, proceed to runGamePass
     * If failed show dialog: "Retry" / "Continue without Better xCloud"
     */
    private void installBetterXcloud() {
        File betterXcloudScript = new File(getFilesDir(), "better-xcloud.user.js");
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String localVersion = prefs.getString("betterXcloudVersion", null);

        String latestVersion = fetchLatestVersion();
        boolean isNewVersionAvailable = latestVersion != null && !Objects.equals(localVersion, latestVersion);
        // Check if script exists and if it's the latest version
        if (!betterXcloudScript.exists() || localVersion == null || isNewVersionAvailable) {
            showDialog(getString(R.string.installation),
                    R.drawable.download,
                    getString(R.string.download_betterxcloud),
                    "", null, "", null);

            downloadScript();
        } else {
            runGamePass(true);
        }
    }

    /**
     * Downloads Better xCloud
     */
    private void downloadScript() {
        Thread thread = new Thread(() -> {
            try {
                URL url = new URL("https://github.com/redphx/better-xcloud/raw/main/better-xcloud.user.js");
                String scriptContent = getString(url);
                String latestVersion = extractVersionFromScript(scriptContent);

                // Store the extracted version
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                prefs.edit().putString("betterXcloudVersion", latestVersion).apply();

                runOnUiThread(() -> {
                    dismissDialog();
                    runGamePass(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    String primaryButtonText = getString(R.string.retry);
                    View.OnClickListener primaryButtonAction = view -> {
                        dismissDialog();
                        installBetterXcloud();
                    };
                    String secondaryButtonText = getString(R.string.go_without_betterxcloud);
                    View.OnClickListener secondaryButtonAction = view -> {
                        dismissDialog();
                        runGamePass(false);
                    };

                    showDialog(
                            getString(R.string.error_occurred),
                            R.drawable.error,
                            getString(R.string.download_error),
                            primaryButtonText,
                            primaryButtonAction,
                            secondaryButtonText,
                            secondaryButtonAction
                    );
                });
            }
        });
        thread.start();
    }

    /**
     * Extracted from downloadScript as recommended by Android Studio, kinda pointless
     * @param url Url to fetch the Better xCloud version from
     * @return The script string
     * @throws IOException Exception
     */
    @NonNull
    private String getString(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        InputStream in = new BufferedInputStream(connection.getInputStream());
        FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "better-xcloud.user.js"));

        // We'll use a ByteArrayOutputStream to capture the output for version parsing
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
            baos.write(buffer, 0, count);
        }
        out.close();
        in.close();
        connection.disconnect();

        // Convert the ByteArrayOutputStream to a String
        return baos.toString("UTF-8");
    }

    /**
     * Extracts the version number from the script content.
     * Assumes the version line is formatted like: // @version      3.5.2
     * @return string
     */
    private String extractVersionFromScript(String scriptContent) {
        try (Scanner scanner = new Scanner(scriptContent)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("@version")) {
                    return line.split("\\s+")[2];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    /**
     * Get latest Better xCloud version number from GitHub
     * @return string
     */
    private String fetchLatestVersion() {
        try {
            URL url = new URL("https://api.github.com/repos/redphx/better-xcloud/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                String json = new Scanner(inputStream).useDelimiter("\\A").next();
                JSONObject latestRelease = new JSONObject(json);
                return latestRelease.getString("tag_name").substring(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Default or error handling
    }

    /**
     * Loads Game Pass into WebView
     * Injects custom scripts and Better xCloud
     * Checks for Better xCloud update
     *
     * @param useBetterXcloud will skip Better xCloud injection if set to false
     */
    private void runGamePass(boolean useBetterXcloud) {
        this.useBetterXcloud = useBetterXcloud;
        GamePassWebView.loadUrl("https://www.xbox.com/play");
    }

    /**
     * Utility function for initial setup,
     * Separated to de-clutter onCreate
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setup() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_main);

        GamePassWebView = findViewById(R.id.webview);
        WebSettings webSettings = GamePassWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);

        webSettings.setDomStorageEnabled(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(GamePassWebView, true);

        String customUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (HTML, like Gecko) Chrome/115.0.0.0 Safari/537.36 Edg/115.0.1901.188";
        webSettings.setUserAgentString(customUserAgent);

        GamePassWebView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.gray));

        GamePassWebView.setWebViewClient(new WebViewClient() {
            // shouldOverrideUrlLoading is deprecated
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (MainActivity.this.useBetterXcloud) {
                    injectScript(view);
                }
            }
        });
    }

    /**
     * Utility function to check if network is available
     * @return boolean
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
        return networkCapabilities != null && (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    /**
     * Utility function to dismiss dialogs based on the common Dialog template
     */
    private void dismissDialog() {
        if (promptDialog != null && promptDialog.isShowing()) {
            promptDialog.dismiss();
        }
    }

    /**
     * Allows Game Pass to be browsed with back button
     * Deprecated method
     */
    @Override
    public void onBackPressed() {
        if (GamePassWebView.canGoBack()) {
            GamePassWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Ensure UI Flags are consistent
     * @param hasFocus Whether the window of this activity has focus.
     *
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    /**
     * Overrides onConfigurationChanged to adjust dialogs if necessary
     * @param newConfig The new device configuration.
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ||
                newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

            // Call your method to adjust the layout
            if (promptDialog != null && promptDialog.isShowing()) {
                adjustScrollConstraintHeight();
            }
        }
    }

    /**
     * Show dialogs with a Game Pass like theme, using a generic template
     * @param titleText Title of the dialog
     * @param imageResource Icon to show (can be empty with 0 (int), in which case no image will be shown)
     * @param messageText Main text
     * @param primaryButtonText Text for the primary button (can be empty, in which case the primary button won't be shown)
     * @param primaryButtonClickListener Action to perform on click on the primary button (can be null if primary button is not used)
     * @param secondaryButtonText Text for the secondary button (can be empty, in which case the secondary button won't be shown)
     * @param secondaryButtonClickListener Text for the primary button (can be null if secondary button is not used)
     */
    private void showDialog(String titleText, int imageResource, String messageText, String primaryButtonText, View.OnClickListener primaryButtonClickListener, String secondaryButtonText, View.OnClickListener secondaryButtonClickListener) {
        promptDialog = new Dialog(this, R.style.FullScreenDialog);
        promptDialog.setContentView(R.layout.prompt_layout);

        TextView titleTextView = promptDialog.findViewById(R.id.title);
        titleTextView.setText(titleText);

        ImageView imageView = promptDialog.findViewById(R.id.layoutIcon);
        if (imageResource != 0) {
            imageView.setImageResource(imageResource);
        } else {
            imageView.setVisibility(View.GONE);
        }

        TextView messageTextView = promptDialog.findViewById(R.id.message);
        messageTextView.setText(messageText);

        Button primaryButton = promptDialog.findViewById(R.id.btnPrimary);
        if (primaryButtonText != null && !primaryButtonText.isEmpty()) {
            primaryButton.setText(primaryButtonText);
            primaryButton.setOnClickListener(primaryButtonClickListener);
        } else {
            primaryButton.setVisibility(View.GONE);
        }

        Button secondaryButton = promptDialog.findViewById(R.id.btnSecondary);
        if (secondaryButtonText != null && !secondaryButtonText.isEmpty()) {
            secondaryButton.setText(secondaryButtonText);
            secondaryButton.setOnClickListener(secondaryButtonClickListener);
        } else {
            secondaryButton.setVisibility(View.GONE);
        }

        adjustScrollConstraintHeight();

        Window window = promptDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        promptDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        promptDialog.show();
        promptDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        promptDialog.getWindow().getDecorView().post(() -> {
            primaryButton.requestFocus();
            primaryButton.setSelected(true);
            primaryButton.setActivated(true);
        });
    }

    private void injectScript(WebView webView) {
        try {
            File scriptFile = new File(getFilesDir(), "better-xcloud.user.js");
            InputStream inputStream = new FileInputStream(scriptFile);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);

            String script = "if (document.readyState === 'loading') {" +
                    "var script = document.createElement('script');" +
                    "script.type = 'text/javascript';" +
                    // Decode and set the script content
                    "script.text = decodeURIComponent(escape(window.atob('" + encoded + "')));" +
                    "document.documentElement.appendChild(script);" +
                    "} else {" +
                    // If the page is not in 'loading', force a reload
                    "window.location.reload();" +
                    "}";
            webView.evaluateJavascript(script, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * UI tweaks to adjust Dialogs on orientation change
     */
    private void adjustScrollConstraintHeight() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;

        int orientation = getResources().getConfiguration().orientation;
        double heightRatio = orientation == Configuration.ORIENTATION_LANDSCAPE ? 0.59 : 0.8;
        int desiredHeight = (int) (screenHeight * heightRatio);

        ConstraintLayout scrollConstraint = promptDialog.findViewById(R.id.scrollConstraint);
        scrollConstraint.setMinHeight(desiredHeight);
    }
}
