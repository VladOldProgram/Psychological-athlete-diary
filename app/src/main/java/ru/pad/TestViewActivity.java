package ru.pad;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


public class TestViewActivity extends AppCompatActivity {
    private String sportsmanUid, testName, testUrl, testType;

    private DatabaseReference dbUser;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_view);
        getUserData();
        initWidgets();
    }

    private void getUserData() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            sportsmanUid = extras.getString("sportsmanUid");
            testName = extras.getString("testName");
            testUrl = extras.getString("testUrl");
            testType = extras.getString("testType");
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        dbUser = database.getReference("Users/" + sportsmanUid);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWidgets() {
        WebView webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);

        CustomWebViewClient webViewClient = new CustomWebViewClient();
        webView.setWebViewClient(webViewClient);

        //webView.loadUrl(url);
        String data;
        Document doc;
        try {
            doc = new getCleanedHTMLPageTask().execute(testUrl).get();
            data = doc.outerHtml();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        webView.loadDataWithBaseURL(testUrl, data, "text/html; charset=UTF-8", null, testUrl);
    }

    public class CustomWebViewClient extends WebViewClient {

        public CustomWebViewClient() { }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            String testResultUrl;
            if (url.contains("https://psytests.org")) {
                String data;
                Document doc;
                try {
                    doc = new getCleanedHTMLPageTask().execute(url).get();
                    data = doc.outerHtml();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                if (url.contains("/result?v=")
                        && testType.equals("availableTests")
                ) {
                    testResultUrl = url;

                    String currentTime = LocalTime.now().toString();
                    int i = currentTime.lastIndexOf(".");
                    currentTime = currentTime.substring(0, i);
                    dbUser
                            .child("activity")
                            .child(LocalDate.now().format(formatter))
                            .child("completedTests")
                            .child(testName + " " + currentTime)
                            .setValue(testResultUrl);
                    dbUser
                            .child("activity")
                            .child(LocalDate.now().format(formatter))
                            .child("availableTests")
                            .child(testName)
                            .removeValue();
                }

                webView.loadDataWithBaseURL(url, data, "text/html; charset=UTF-8", null, url);

                return false;
            }

            return true;
        }
        /*
        @Override
        public void onPageFinished(WebView webView, String url) {
            webView.loadUrl(
                    "javascript:(function() { " +
                            "document.getElementsByClassName('reel reel_F')[0].remove(); " +
                            "document.querySelector('body > div:last-of-type').remove(); " +
                            "document.querySelector('body > div:last-of-type').remove(); " +
                            "document.querySelector('body > div:last-of-type').remove(); " +
                            "document.getElementById('nac1').remove(); " +
                            "document.getElementById('hDr').remove(); " +
                            "document.getElementById('hDrm').remove(); " +
                            "document.getElementsByTagName('footer')[0].remove(); " +
                    "})()"
            );
        }
        */
    }

    @SuppressWarnings("deprecation")
    private static class getCleanedHTMLPageTask extends AsyncTask<String, String, Document> {
        @Override
        protected Document doInBackground(String... urls) {
            Document doc;
            try {
                doc = Jsoup.connect(urls[0]).get();
                /*for (Element element : doc.select(
                        "head>script:nth-of-type(2)," +
                                "head>script:nth-of-type(3)," +
                                "head>script:nth-of-type(4)," +
                                "head>script:nth-of-type(5)," +
                                "body>script:nth-of-type(2)," +
                                "body>script:nth-of-type(3)," +
                                ".reel," +
                                "#nac1," +
                                "#hDr," +
                                "#hDrm," +
                                "footer"
                )) {
                    element.remove();
                }*/
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return doc;
        }
    }
}