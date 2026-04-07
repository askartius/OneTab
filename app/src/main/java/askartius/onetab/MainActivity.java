package askartius.onetab;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Patterns;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.search.SearchBar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.search.SearchView;

import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        webView = findViewById(R.id.web_view);
        LinearProgressIndicator progressIndicator = findViewById(R.id.progress_indicator);
        SearchBar searchBar = findViewById(R.id.search_bar);
        SearchView searchView = findViewById(R.id.search_view);

        // Set up the WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Android 7)");
        webView.getSettings().setSaveFormData(false);
        webView.getSettings().setGeolocationEnabled(false);
        webView.getSettings().setDomStorageEnabled(false);
        webView.getSettings().setDatabaseEnabled(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Block specific websites
                if (request.getUrl().toString().contains("youtube.com")) {
                    Toast.makeText(MainActivity.this, R.string.content_blocked, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);

                progressIndicator.setProgress(newProgress);

                if (newProgress >= 100) {
                    progressIndicator.animate().alpha(0).setDuration(750);
                    searchBar.setText(webView.getUrl());
                } else if (progressIndicator.getAlpha() == 0) {
                    progressIndicator.setAlpha(1);
                }
            }
        });
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            String dialogTitle = getString(R.string.download_dialog_title);

            // Add the file size to the dialog title if it is known
            if (contentLength > 0) {
                double unitSize = 1.0;
                for (int i = 0; i < 5; i++) {
                    if (contentLength / unitSize > 100) {
                        unitSize *= 1024;
                    } else {
                        dialogTitle += String.format(Locale.getDefault(), getString(R.string.file_size_format), contentLength / unitSize, new char[]{'\0', 'k', 'M', 'G', 'T'}[i]);
                        break;
                    }
                }
            }

            // Show a confirmation dialog
            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle(dialogTitle)
                    .setMessage(fileName)
                    .setPositiveButton(R.string.download, (dialog, which) -> {
                        dialog.dismiss();

                        // Start the download
                        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                        request.setMimeType(mimetype);
                        request.setTitle(fileName);
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                        if (downloadManager != null) {
                            downloadManager.enqueue(request);
                            Toast.makeText(MainActivity.this, R.string.download_started, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.download_error, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss()).show();
        });

        // Disable third-party cookies
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        // Set up the back gesture handling
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchView.isShowing()) {
                    searchView.hide();
                    return;
                }

                if (webView.canGoBack()) {
                    webView.stopLoading();
                    webView.goBack();
                } else {
                    finish(); // Close the app
                }
            }
        });

        // Set up the search bar
        searchBar.setOnClickListener(v -> {
            searchView.setText(searchBar.getText());
            searchView.getEditText().selectAll();
            searchView.show();
        });
        searchBar.setOnLongClickListener(v -> {
            webView.reload();
            return true;
        });

        // Set up the search function
        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if (Patterns.WEB_URL.matcher(searchView.getText()).matches()) {
                webView.loadUrl(searchView.getText().toString());
            } else {
                webView.loadUrl(getString(R.string.search_string) + searchView.getText());
            }

            searchBar.setText(searchView.getText());

            searchView.hide();

            return true;
        });

        // Restore the previous webpage or load a new one
        if (savedInstanceState != null) {
            webView.loadUrl(savedInstanceState.getString("url", getString(R.string.home_page)));
        } else {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("url", webView.getUrl());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(@NonNull Intent intent) {
        // Check if the user opened a deep link
        if (Objects.equals(intent.getAction(), Intent.ACTION_VIEW)) {
            Uri uri = intent.getData();
            if (uri != null && (Objects.equals(uri.getScheme(), "https") || Objects.equals(uri.getScheme(), "http"))) {
                webView.loadUrl(uri.toString());
                return;
            }
        }

        webView.loadUrl(getString(R.string.home_page)); // Otherwise, load the home page
    }
}