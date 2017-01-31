package edu.stanford.algorithms;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hotchemi.android.rate.AppRate;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String BASE_URL = "http://rkpandey.com/AlgorithmHelper/";
    public static final int MILLISECONDS_UNTIL_EXPIRY = 1000 * 60 * 60 * 24; // 24 hours
    public static final long UNSAVED = -1;
    public static final String FILE_PREFIX = "file://";
    public static final String ERROR_FILE_PATH = FILE_PREFIX + "/android_asset/error.html";

    @BindView(R.id.webview) WebView _webView;
    @BindView(R.id.toolbar) Toolbar _toolbar;
    @BindView(R.id.drawer_layout) DrawerLayout _drawer;
    @BindView(R.id.nav_view) NavigationView _navigationView;
    @BindView(R.id.about_content) RelativeLayout _aboutContent;
    @BindView(R.id.donate_button) Button _donateButton;
    @BindView(R.id.rate_us_button) Button _rateUsButton;

    private Stack<Integer> _navigationIds;

    private boolean isExpired(long previousTime) {
        return System.currentTimeMillis() - previousTime > MILLISECONDS_UNTIL_EXPIRY;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Enable responsive layout
        _webView.getSettings().setUseWideViewPort(true);
        _webView.getSettings().setJavaScriptEnabled(true);
        _webView.addJavascriptInterface(new JavascriptInterfaceDownloader(this), "downloadHtml");
        _webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String pageName;
                if (!url.equals(ERROR_FILE_PATH) && url.contains(FILE_PREFIX)) {
                    pageName = url.substring(url.lastIndexOf(File.separatorChar) + 1);
                } else if (url.contains(BASE_URL)) {
                    pageName = url.replace(BASE_URL, "");
                } else {
                    // Loading an external page into the webview
                    return;
                }
                SharedPreferences preferences = getPreferences(MODE_PRIVATE);
                long lastSavedTime = preferences.getLong(pageName, UNSAVED);
                if (lastSavedTime == UNSAVED || isExpired(lastSavedTime)) {
                    preferences.edit().putLong(pageName, System.currentTimeMillis()).apply();
                    _webView.loadUrl("javascript:window.downloadHtml.processHtml('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>','" + pageName + "');");
                }
            }
        });
        setSupportActionBar(_toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, _drawer, _toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        _drawer.addDrawerListener(toggle);
        toggle.syncState();

        _navigationIds = new Stack<>();
        _navigationView.setNavigationItemSelectedListener(this);

        _navigationView.setCheckedItem(R.id.nav_trees);
        _navigationIds.push(R.id.nav_trees);
        _drawer.openDrawer(_navigationView);
    }

    @Override
    public void onBackPressed() {
        if (_drawer.isDrawerOpen(GravityCompat.START)) {
            _drawer.closeDrawer(GravityCompat.START);
            return;
        }
        if (_webView.canGoBack()) {
            _navigationIds.pop();
            _navigationView.setCheckedItem(_navigationIds.peek());
            _webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        _navigationIds.push(id);
        String pageName = "";
        if (id == R.id.nav_trees) {
            pageName = "Trees";
        } else if (id == R.id.nav_lists) {
            pageName = "Lists";
        } else if (id == R.id.nav_sorting) {
            pageName = "Sorting";
        } else if (id == R.id.nav_graphs) {
            pageName = "Graphs";
        } else if (id == R.id.nav_about) {
            // swap out content
            _webView.setVisibility(GONE);
            _aboutContent.setVisibility(VISIBLE);
            _drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        if (isOnline()) {
            // Load from internet
            _webView.loadUrl(BASE_URL + pageName);
        } else {
            // Load from local file
            String fileLocation = this.getFilesDir().getPath() + File.separator + pageName;
            if (new File(fileLocation).exists()) {
                _webView.loadUrl("file://" + fileLocation);
            } else {
                _webView.loadUrl(ERROR_FILE_PATH);
            }
        }
        _webView.setVisibility(VISIBLE);
        _aboutContent.setVisibility(GONE);
        _drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @OnClick(R.id.donate_button)
    public void onDonateButtonTap(View view) {
        System.out.println("Donate button tapped");
    }


    @OnClick(R.id.rate_us_button)
    public void onRateUsButtonTap(View view) {
        System.out.println("Hello World");
        AppRate.with(this).showRateDialog(this);
    }

    public boolean isOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException e)          { e.printStackTrace(); }
        catch (InterruptedException e) { e.printStackTrace(); }
        return false;
    }
}
