package com.skyfz.vipplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Process;

import com.skyfz.vipplayer.loading.ZLoadingDialog;
import com.skyfz.vipplayer.loading.Z_TYPE;
import com.skyfz.vipplayer.utils.X5WebView;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.sdk.CookieSyncManager;
import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;
import com.tencent.smtt.utils.TbsLog;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private String regex = ".*//(.+?\\.)?(youku|iqiyi|tudou|qq|mgtv|letv|le|sohu|acfun|pptv|yinyuetai|yy|bilibili|wasu|163|56|fun|xunyingwang|meitudata|toutiao|tangdou)\\.(com|net|cn)/.*";

    private X5WebView mWebView = null;
    private ViewGroup mViewParent;
    private ImageButton mBack;
    private ImageButton mForward;
    private ImageButton mPlay;
    private ImageButton mExit;
    private ImageButton mHome;
    private ImageButton mMore;
    private Button mGo;
    private EditText mUrl;
    private ZLoadingDialog LoadingDialog = null;

    private static final String mHomeUrl = "http://m.v.baidu.com/";
    private static final String TAG = "VIPlayer";
    private static final int MAX_LENGTH = 14;

    private final int disable = 120;
    private final int enable = 255;

    private ProgressBar mPageLoadingProgressBar = null;

    private ValueCallback<Uri> uploadFile;

    private URL mIntentUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getSupportActionBar().hide();

        try {
            if (Integer.parseInt(android.os.Build.VERSION.SDK) >= 11) {
                getWindow()
                        .setFlags(
                                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
            }
        } catch (Exception e) {
        }

        setContentView(R.layout.activity_main);
        mViewParent = (ViewGroup) findViewById(R.id.webView1);

        initBtnListenser();

        mTestHandler.sendEmptyMessageDelayed(MSG_INIT_UI, 10);

        //去除QQ浏览器推广
        getWindow().getDecorView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                ArrayList<View> outView = new ArrayList<View>();
                getWindow().getDecorView().findViewsWithText(outView, "QQ浏览器", View.FIND_VIEWS_WITH_TEXT);
                int size = outView.size();
                if (outView != null && outView.size() > 0) {
                    outView.get(0).setVisibility(View.GONE);
                }
            }
        });
    }

    private void changGoForwardButton(WebView view) {
        if (view.canGoBack())
            mBack.setAlpha(enable);
        else
            mBack.setAlpha(disable);
        if (view.canGoForward())
            mForward.setAlpha(enable);
        else
            mForward.setAlpha(disable);
        if (view.getUrl() != null && view.getUrl().equalsIgnoreCase(mHomeUrl)) {
            mHome.setAlpha(disable);
            mHome.setEnabled(false);
        } else {
            mHome.setAlpha(enable);
            mHome.setEnabled(true);
        }
    }

    private void initProgressBar() {
        mPageLoadingProgressBar = (ProgressBar) findViewById(R.id.progressBar1);// new
        // ProgressBar(getApplicationContext(),
        // null,
        // android.R.attr.progressBarStyleHorizontal);
        mPageLoadingProgressBar.setMax(100);
        mPageLoadingProgressBar.setProgressDrawable(this.getResources()
                .getDrawable(R.drawable.color_progressbar));
    }

    private void init() {
        if(!QbSdk.isTbsCoreInited()){
            VIPlayer.mTestHandler = mTestHandler;
            LoadingDialog = new ZLoadingDialog(MainActivity.this);
            LoadingDialog.setLoadingBuilder(Z_TYPE.LEAF_ROTATE)//设置类型
                    .setLoadingColor(Color.BLUE)//颜色
                    .setHintText(getString(R.string.first_time))
                    .setHintTextSize(16) // 设置字体大小 dp
                    .setHintTextColor(Color.GRAY)  // 设置字体颜色
                    .show();
            return;
        }

        VIPlayer.mTestHandler = null;
        if(LoadingDialog != null){
            LoadingDialog.dismiss();
            LoadingDialog = null;
        }

        if(mWebView != null) return;

        mWebView = new X5WebView(this, null);

        mViewParent.addView(mWebView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT,
                FrameLayout.LayoutParams.FILL_PARENT));

        initProgressBar();

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Intent intent;
                boolean handled = false;
                // 判断URL
                if (url.startsWith("http:") || url.startsWith("https:")) {
                    view.loadUrl(url);
                    handled = true;
                }

                // baidu
                if (url.startsWith("bdvideo:")) {
                    handled = true;
                }

                return handled;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (Integer.parseInt(android.os.Build.VERSION.SDK) >= 16)
                    changGoForwardButton(view);
				/* mWebView.showLog("test Log"); */
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onJsConfirm(WebView arg0, String arg1, String arg2,
                                       JsResult arg3) {
                return super.onJsConfirm(arg0, arg1, arg2, arg3);
            }

            @Override
            public boolean onJsAlert(WebView arg0, String arg1, String arg2,
                                     JsResult arg3) {
                /**
                 * 这里写入你自定义的window alert
                 */
                return super.onJsAlert(null, arg1, arg2, arg3);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (!mUrl.hasFocus()) {
                    // !tbsContent.getUrl().equalsIgnoreCase(url)  &&
                    String url = view.getUrl();
                    if(Pattern.matches(regex, url)){
                        mPlay.setVisibility(View.VISIBLE);
                    }else{
                        mPlay.setVisibility(View.GONE);
                    }
                    if (url.startsWith(FIELDS.DEFAULT_API)){
                        mUrl.setText(R.string.defaultapi);
                        runJS("document.title = \""+getString(R.string.defaultapi)+"\";");
                    }else if (title != null && title.length() > MAX_LENGTH)
                        mUrl.setText(title.subSequence(0, MAX_LENGTH) + "...");
                    else
                        mUrl.setText(title);
                }
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // TODO Auto-generated method stub
//                mPageLoadingProgressBar.setProgress(newProgress);

//                if (mPageLoadingProgressBar != null && newProgress != 100) {
//                    mPageLoadingProgressBar.setVisibility(View.VISIBLE);
                if (mPageLoadingProgressBar != null && newProgress != 100) {
                    mPageLoadingProgressBar.setProgress(newProgress);

                } else {
                    mPageLoadingProgressBar.setProgress(0);
                }

            }
        });

        mWebView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(String arg0, String arg1, String arg2,
                                        String arg3, long arg4) {
                TbsLog.d(TAG, "url: " + arg0);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("allow to download？")
                        .setPositiveButton("yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
//                                        Toast.makeText(
//                                                MainActivity.this,
//                                                "fake message: i'll download...",
//                                                1000).show();
                                    }
                                })
                        .setNegativeButton("no",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        // TODO Auto-generated method stub
                                        Toast.makeText(
                                                MainActivity.this,
                                                "fake message: refuse download...",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                        .setOnCancelListener(
                                new DialogInterface.OnCancelListener() {

                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        // TODO Auto-generated method stub
                                        Toast.makeText(
                                                MainActivity.this,
                                                "fake message: refuse download...",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }).show();
            }
        });

        long time = System.currentTimeMillis();
        if (mIntentUrl == null) {
            mWebView.loadUrl(mHomeUrl);
        } else {
            mWebView.loadUrl(mIntentUrl.toString());
        }
        Log.d("time-cost", "cost time: "
                + (System.currentTimeMillis() - time));
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().sync();
    }

    private void runJS(String js){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.evaluateJavascript(js, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    //
                }
            });
        }
    }

    private void initBtnListenser() {
        mBack = (ImageButton) findViewById(R.id.btnBack1);
        mForward = (ImageButton) findViewById(R.id.btnForward1);
        mPlay = (ImageButton) findViewById(R.id.btnPlay);
        mExit = (ImageButton) findViewById(R.id.btnExit1);
        mHome = (ImageButton) findViewById(R.id.btnHome1);
        mGo = (Button) findViewById(R.id.btnGo1);
        mUrl = (EditText) findViewById(R.id.editUrl1);
        mMore = (ImageButton) findViewById(R.id.btnMore);
        if (Integer.parseInt(android.os.Build.VERSION.SDK) >= 16) {
            mBack.setAlpha(disable);
            mForward.setAlpha(disable);
            mHome.setAlpha(disable);
        }
        mHome.setEnabled(false);

        mBack.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mWebView != null && mWebView.canGoBack())
                    mWebView.goBack();
            }
        });

        mForward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mWebView != null && mWebView.canGoForward())
                    mWebView.goForward();
            }
        });

        mGo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String url = mUrl.getText().toString();
                goUrl(url);
            }
        });

        mMore.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MainActivity.this, mMore);
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        switch (id){
                            case R.id.about:
                                // Inflate the about message contents
                                View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

                                // When linking text, force to always use default color. This works
                                // around a pressed color state bug.
                                TextView textView = (TextView) messageView.findViewById(R.id.about_credits);
                                int defaultColor = textView.getTextColors().getDefaultColor();
                                textView.setTextColor(defaultColor);

                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setIcon(R.mipmap.ic_launcher);
                                    builder.setTitle(R.string.app_name);
                                    builder.setView(messageView);
                                    builder.create();
                                    builder.show();
                                break;
                            case R.id.api:
                                Intent intent = new Intent(MainActivity.this, Api_List.class);
                                startActivity(intent);
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                });

                popup.show();
            }
        });

        mUrl.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                String url = mWebView.getUrl();
                if (hasFocus) {
                    mGo.setVisibility(View.VISIBLE);
                    if (null == url)
                        return;
                    if (url.startsWith(mHomeUrl)) {
                        mUrl.setText("");
                        mGo.setText(R.string.home);
                        mGo.setTextColor(0X6F0F0F0F);
                    } if (url.startsWith(FIELDS.DEFAULT_API)){
                        mUrl.setText("");
                        mGo.setText(R.string.defaultapi);
                        mGo.setTextColor(0X6F0F0F0F);
                    } else {
                        mUrl.setText(url);
                        mGo.setText(R.string.go);
                        mGo.setTextColor(0X6F0000CD);
                    }
                } else {
                    mGo.setVisibility(View.GONE);
                    String title = mWebView.getTitle();
                    if (url != null && url.startsWith(FIELDS.DEFAULT_API))
                        mUrl.setText(R.string.defaultapi);
                    else if (url != null && url.startsWith(mHomeUrl))
                        mUrl.setText(R.string.defaultapi);
                    else if (title != null && title.length() > MAX_LENGTH)
                        mUrl.setText(title.subSequence(0, MAX_LENGTH) + "...");
                    else
                        mUrl.setText(title);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }

        });

        mUrl.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

                String url = null;
                if (mUrl.getText() != null) {
                    url = mUrl.getText().toString();
                }

                if (url == null
                        || mUrl.getText().toString().equalsIgnoreCase("")) {
                    mGo.setText(R.string.enter_url);
                    mGo.setTextColor(0X6F0F0F0F);
                } else {
                    mGo.setText(R.string.go);
                    mGo.setTextColor(0X6F0000CD);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
                // TODO Auto-generated method stub

            }
        });

        mUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    String url = mUrl.getText().toString();
                    goUrl(url);
                    handled = true;
                }
                return handled;
            }
        });

        mHome.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mWebView != null)
                    mWebView.loadUrl(mHomeUrl);
            }
        });

        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWebView != null){
                    SharedPreferences prefs = getSharedPreferences(FIELDS.SP_NAME, Context.MODE_PRIVATE);
                    long curId = prefs.getLong(FIELDS.SP_API_ID, (long)0);
                    String api_url = "";
                    if(curId == 0 ){
                        api_url = FIELDS.DEFAULT_API;
                    }else{
                        api_url = prefs.getString(FIELDS.SP_API_URL, FIELDS.DEFAULT_API);
                    }
                    mWebView.loadUrl(api_url+ URLEncoder.encode(mWebView.getUrl()));
                }
            }
        });

        mExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.app_name)
                    .setPositiveButton(R.string.confirmBtn, new AlertDialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            Process.killProcess(Process.myPid());
                        }
                    }).setNegativeButton(R.string.cancelBtn, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setMessage(R.string.quitnow)
                    .create().show();
            }
        });
    }

    private void goUrl(String url){
        if (mWebView != null){
            if (!url.startsWith("http:") && !url.startsWith("https:")) {
                if (url.startsWith("//")) {
                    url = "http:" + url;
                }else{
                    url = "http://" + url;
                }
            }
            mWebView.loadUrl(url);
            mWebView.requestFocus();
        }
    }

    boolean[] m_selected = new boolean[] { true, true, true, true, false,
            false, true };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
                if (Integer.parseInt(android.os.Build.VERSION.SDK) >= 16)
                    changGoForwardButton(mWebView);
                return true;
            } else
                return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 0:
                    if (null != uploadFile) {
                        Uri result = data == null || resultCode != RESULT_OK ? null
                                : data.getData();
                        uploadFile.onReceiveValue(result);
                        uploadFile = null;
                    }
                    break;
                default:
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (null != uploadFile) {
                uploadFile.onReceiveValue(null);
                uploadFile = null;
            }

        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent == null || mWebView == null || intent.getData() == null)
            return;
        mWebView.loadUrl(intent.getData().toString());
    }

    @Override
    protected void onDestroy() {
        if (mTestHandler != null)
            mTestHandler.removeCallbacksAndMessages(null);
        if (mWebView != null)
            mWebView.destroy();
        super.onDestroy();
    }

    public static final int MSG_OPEN_TEST_URL = 0;
    public static final int MSG_INIT_UI = 1;
    public static final int MSG_DOWNLOADING = 2;
    public static final int MSG_INSTALLING = 3;
    public static final int MSG_INITIALIZATION = 4;
    private final int mUrlStartNum = 0;
    private int mCurrentUrl = mUrlStartNum;
    public Handler mTestHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT_UI:
                    init();
                    break;
                case MSG_DOWNLOADING:
                    if(LoadingDialog != null){
                        Bundle b = msg.getData();
                        LoadingDialog.setText(getString(R.string.downloading)+"("+b.getInt("progress")+"%)");
                    }
                    break;
                case MSG_INSTALLING:
                    if(LoadingDialog != null){
                        LoadingDialog.setText(getString(R.string.installing));
                    }
                    break;
                case MSG_INITIALIZATION:
                    if(LoadingDialog != null){
                        LoadingDialog.setText(getString(R.string.initialization));
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };
}
