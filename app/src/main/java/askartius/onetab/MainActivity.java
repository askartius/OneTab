package askartius.onetab;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.webkit.CookieManager;
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
import com.google.android.material.search.SearchView;

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

        // Setup WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Android 7)");
        webView.getSettings().setSaveFormData(false);
        webView.getSettings().setGeolocationEnabled(false);
        webView.getSettings().setDomStorageEnabled(false);
        webView.getSettings().setDatabaseEnabled(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request.getUrl().toString().contains("youtube.com")) {
                    Toast.makeText(MainActivity.this, "This content is blocked", Toast.LENGTH_SHORT).show();
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

        // Disable cookies
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        // Setup back gesture handling
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
                    finish();
                }
            }
        });

        // Setup search bar
        searchBar.setOnClickListener(v -> {
            searchView.setText(searchBar.getText());
            searchView.getEditText().selectAll();
            searchView.show();
        });
        searchBar.setOnLongClickListener(v -> {
            webView.reload();
            return true;
        });

        // Setup search
        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if (Patterns.WEB_URL.matcher(searchView.getText()).matches()) {
                webView.loadUrl(searchView.getText().toString());
            } else {
                webView.loadUrl("https://www.bing.com/search?q=" + searchView.getText());
            }

            searchBar.setText(searchView.getText());

            searchView.hide();

            return true;
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(@NonNull Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_VIEW)) {
            Uri uri = intent.getData();

            if (uri != null && (Objects.equals(uri.getScheme(), "https") || Objects.equals(uri.getScheme(), "http"))) {
                webView.loadUrl(uri.toString());
                return;
            }
        }

        webView.loadUrl("https://www.bing.com");
    }
}