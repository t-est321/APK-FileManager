package com.dlna.FileManager;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public final class HtmlActivity extends Activity {
  private WebView wv;
  private File htmlFile;
  private File viewDir;
  private ValueCallback<Uri[]> fileCb;
  private ValueCallback<Uri> fileCbOld;
  @Override
  protected void onCreate(Bundle b) {
    AppSettings.load(this);
    Ui.applyTheme(this);
    super.onCreate(b);
    String path = getIntent().getStringExtra("path");
    htmlFile = new File(path == null ? "" : path);
    viewDir = AppSettings.htmlViewDir(this);
    viewDir.mkdirs();
    AppSettings s = AppSettings.get();
    if (s.htmlClearOnOpen) {
      FileOps.deleteRec(viewDir);
      viewDir.mkdirs();
    }
    if (s.htmlCopyAssets) {
      copyAssets(htmlFile, viewDir);
    } else {
      try {
        FileOps.copyRec(htmlFile, new File(viewDir, htmlFile.getName()));
      } catch (Exception ignored) {}
    }
    String content = readHtml(htmlFile);
    if (s.htmlShowBar) {
      content = injectBar(content, htmlFile.getName(), s.htmlHideBar);
    }
    wv = new WebView(this);
    applySettings(wv);
    wv.addJavascriptInterface(new Bridge(this), "FmBridge");
    wv.setWebViewClient(new WebViewClient() {
      @Override
      public void onReceivedError(WebView v, int errorCode, String desc, String failing) {
        OpLog.err(HtmlActivity.this, "html error " + errorCode + " " + desc + " " + failing);
      }
      @Override
      public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
        if (Build.VERSION.SDK_INT >= 23 && err != null) {
          OpLog.err(HtmlActivity.this, "html error " + err.getErrorCode() + " " + err.getDescription());
        }
      }
    });
    wv.setDownloadListener(new DownloadListener() {
      @Override
      public void onDownloadStart(String url, String userAgent, String cd, String mime, long len) {
        startDownload(url, cd, userAgent);
      }
    });
    wv.setWebChromeClient(new WebChromeClient() {
      @Override
      public boolean onConsoleMessage(ConsoleMessage cm) {
        if (cm != null && cm.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
          OpLog.err(HtmlActivity.this, "html console " + cm.message() + " @" + cm.lineNumber());
        }
        return true;
      }
      @Override
      public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb,
          FileChooserParams params) {
        if (fileCb != null) {
          fileCb.onReceiveValue(null);
        }
        fileCb = cb;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*" + "/" + "*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
          startActivityForResult(Intent.createChooser(i, "Open"), 71);
        } catch (Exception e) {
          fileCb = null;
          return false;
        }
        return true;
      }
      public void openFileChooser(ValueCallback<Uri> cb) {
        openFileChooser(cb, "*" + "/" + "*");
      }
      public void openFileChooser(ValueCallback<Uri> cb, String accept) {
        fileCbOld = cb;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(accept == null || accept.length() == 0 ? "*" + "/" + "*" : accept);
        try {
          startActivityForResult(Intent.createChooser(i, "Open"), 72);
        } catch (Exception e) {
          fileCbOld = null;
        }
      }
    });
    CrashHandler.install(this);
    setContentView(wv);
    Ui.bar(this);
    OpLog.info(this, "html open " + htmlFile.getAbsolutePath());
    String base = android.net.Uri.fromFile(viewDir).toString();
    if (!base.endsWith("/")) {
      base = base + "/";
    }
    wv.loadDataWithBaseURL(base, content, "text/html", "UTF-8", base);
  }
  private void applySettings(WebView w) {
    AppSettings s = AppSettings.get();
    try {
      WebSettings ws = w.getSettings();
      ws.setJavaScriptEnabled(s.htmlJs);
      ws.setDomStorageEnabled(s.htmlDom);
      ws.setAllowFileAccess(s.htmlFileAccess);
      if (Build.VERSION.SDK_INT >= 16) {
        ws.setAllowFileAccessFromFileURLs(false);
        ws.setAllowUniversalAccessFromFileURLs(false);
      }
      ws.setSupportZoom(s.htmlZoom);
      ws.setBuiltInZoomControls(s.htmlZoom);
      ws.setDisplayZoomControls(false);
      ws.setUseWideViewPort(s.htmlWide);
      ws.setLoadWithOverviewMode(s.htmlOverview);
      ws.setLoadsImagesAutomatically(s.htmlImages);
      ws.setBlockNetworkImage(!s.htmlImages);
      ws.setCacheMode(s.htmlCache ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_NO_CACHE);
      String ua = s.htmlUserAgent();
      if (ua != null && ua.length() > 0) {
        ws.setUserAgentString(ua);
      }
      if (Build.VERSION.SDK_INT >= 21) {
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
      }
      int sw = 0;
      if (s.htmlScreenIdx > 0 && s.htmlScreenIdx < AppSettings.HTML_SCREEN_PX.length) {
        sw = AppSettings.HTML_SCREEN_PX[s.htmlScreenIdx];
      }
      if (sw > 0) {
        int dw = getResources().getDisplayMetrics().widthPixels;
        if (dw > 0) {
          int scale = (int) (dw * 100L / sw);
          if (scale < 10) {
            scale = 10;
          }
          if (scale > 400) {
            scale = 400;
          }
          w.setInitialScale(scale);
        }
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);
      }
      try {
        w.setLayerType(s.htmlHwAccel ? android.view.View.LAYER_TYPE_HARDWARE
          : android.view.View.LAYER_TYPE_SOFTWARE, null);
      } catch (Exception ignored2) {}
    } catch (Exception ignored) {}
  }
  static final class Bridge {
    private final java.lang.ref.WeakReference<HtmlActivity> ref;
    Bridge(HtmlActivity a) {
      ref = new java.lang.ref.WeakReference<HtmlActivity>(a);
    }
    @JavascriptInterface
    public void exitHtmlView() {
      final HtmlActivity a = ref.get();
      if (a == null) {
        return;
      }
      a.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          a.finish();
        }
      });
    }
    @JavascriptInterface
    public void reloadHtmlFile() {
      final HtmlActivity a = ref.get();
      if (a == null) {
        return;
      }
      a.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          a.recreate();
        }
      });
    }
  }
  private static String readHtml(File f) {
    try {
      long lim = 5L * 1024L * 1024L;
      if (f.length() > lim) {
        return FileOps.readString(f).substring(0, (int) Math.min(f.length(), lim));
      }
      return FileOps.readString(f);
    } catch (Exception e) {
      return "<html><body><p>Read error</p></body></html>";
    }
  }
  private static void copyAssets(File html, File dest) {
    dest.mkdirs();
    try {
      FileOps.copyRec(html, new File(dest, html.getName()));
    } catch (Exception ignored) {}
    String content;
    try {
      content = FileOps.readString(html);
    } catch (Exception e) {
      return;
    }
    Set<String> refs = refsOf(content);
    File parent = html.getParentFile();
    if (parent == null) {
      return;
    }
    java.util.Iterator<String> it = refs.iterator();
    while (it.hasNext()) {
      String ref = it.next();
      if (ref.startsWith("http") || ref.startsWith("data:") || ref.startsWith("//") || ref.startsWith("#")) {
        continue;
      }
      while (ref.startsWith("./")) {
        ref = ref.substring(2);
      }
      if (ref.length() == 0 || ref.indexOf("..") >= 0) {
        continue;
      }
      File src = new File(parent, ref);
      if (!src.exists()) {
        continue;
      }
      File dst = new File(dest, ref);
      File dp = dst.getParentFile();
      if (dp != null) {
        dp.mkdirs();
      }
      try {
        FileOps.copyRec(src, dst);
      } catch (Exception ignored) {}
    }
  }
  private static Set<String> refsOf(String html) {
    Set<String> refs = new HashSet<String>();
    String[] pats = {
      "src=[\"']([^\"'#?]+)[\"']",
      "href=[\"']([^\"'#?]+)[\"']",
      "from[ \t]+[\"']([^\"'#?]+)[\"']",
      "url\\([\"']?([^\"')#?]+)[\"']?\\)"
    };
    for (int i = 0; i < pats.length; i++) {
      Matcher m = Pattern.compile(pats[i], Pattern.CASE_INSENSITIVE).matcher(html);
      while (m.find()) {
        String r = m.group(1).trim();
        if (r.length() > 0) {
          refs.add(r);
        }
      }
    }
    return refs;
  }
  private static String injectBar(String html, String fileName, boolean hideStart) {
    StringBuilder ctrl = new StringBuilder();
    ctrl.append("<script>");
    ctrl.append("function __fmBack(){try{FmBridge.exitHtmlView();}catch(e){history.go(-999);}}");
    ctrl.append("function __fmReload(){try{FmBridge.reloadHtmlFile();}catch(e){location.reload();}}");
    ctrl.append("function __fmHide(){var c=document.getElementById('__ctrl__');");
    ctrl.append("var s=document.getElementById('__ctrl_spacer__');");
    ctrl.append("var r=document.getElementById('__ctrl_restore__');");
    ctrl.append("if(c)c.style.display='none';if(s)s.style.display='none';if(r)r.style.display='block';}");
    ctrl.append("function __fmShow(){var c=document.getElementById('__ctrl__');");
    ctrl.append("var s=document.getElementById('__ctrl_spacer__');");
    ctrl.append("var r=document.getElementById('__ctrl_restore__');");
    ctrl.append("if(c)c.style.display='flex';if(s)s.style.display='block';if(r)r.style.display='none';}");
    ctrl.append("</script>");
    ctrl.append("<div id='__ctrl__' style='position:fixed;top:0;left:0;right:0;z-index:2147483647;");
    ctrl.append("background:rgba(20,28,38,0.95);display:flex;gap:5px;padding:6px 10px;");
    ctrl.append("flex-wrap:wrap;align-items:center;'>");
    ctrl.append("<button onclick='__fmBack()' style='background:#e74c3c;color:#fff;border:none;");
    ctrl.append("border-radius:6px;padding:5px 12px;font-size:13px;font-weight:700;'>Back</button>");
    ctrl.append("<button onclick='history.back()' style='background:#2980b9;color:#fff;border:none;");
    ctrl.append("border-radius:6px;padding:5px 9px;font-size:13px;font-weight:700;'>Prev</button>");
    ctrl.append("<button onclick='history.forward()' style='background:#2980b9;color:#fff;border:none;");
    ctrl.append("border-radius:6px;padding:5px 9px;font-size:13px;font-weight:700;'>Next</button>");
    ctrl.append("<button onclick='__fmReload()' style='background:#8e44ad;color:#fff;border:none;");
    ctrl.append("border-radius:6px;padding:5px 9px;font-size:13px;font-weight:700;'>Reload</button>");
    ctrl.append("<button onclick='__fmHide()' style='background:#8e44ad;color:#fff;border:none;");
    ctrl.append("border-radius:6px;padding:5px 9px;font-size:13px;font-weight:700;'>Hide toolbar</button>");
    ctrl.append("<span style='color:#bdc3c7;font-size:11px;flex-grow:1;'>");
    ctrl.append(esc(fileName));
    ctrl.append("</span></div><div id='__ctrl_spacer__' style='height:44px;'></div>");
    ctrl.append("<button id='__ctrl_restore__' onclick='__fmShow()' style='display:none;position:fixed;");
    ctrl.append("top:6px;right:8px;z-index:2147483647;background:rgba(20,28,38,0.92);color:#fff;");
    ctrl.append("border:none;border-radius:6px;padding:5px 10px;font-size:12px;font-weight:700;'>Menu</button>");
    if (hideStart) {
      ctrl.append("<script>setTimeout(function(){try{__fmHide();}catch(e){}},0);</script>");
    }
    if (html.contains("<body")) {
      int idx = html.indexOf(">", html.indexOf("<body"));
      if (idx >= 0) {
        return html.substring(0, idx + 1) + ctrl.toString() + html.substring(idx + 1);
      }
    }
    return ctrl.toString() + html;
  }
  private static String esc(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
  @Override
  protected void onActivityResult(int req, int res, Intent data) {
    super.onActivityResult(req, res, data);
    if (req == 71) {
      Uri[] uris = null;
      if (res == RESULT_OK && data != null) {
        if (data.getClipData() != null) {
          int n = data.getClipData().getItemCount();
          uris = new Uri[n];
          for (int i = 0; i < n; i++) {
            uris[i] = data.getClipData().getItemAt(i).getUri();
          }
        } else if (data.getData() != null) {
          uris = new Uri[]{data.getData()};
        }
      }
      if (fileCb != null) {
        fileCb.onReceiveValue(uris);
        fileCb = null;
      }
      return;
    }
    if (req == 72) {
      Uri u = res == RESULT_OK && data != null ? data.getData() : null;
      if (fileCbOld != null) {
        fileCbOld.onReceiveValue(u);
        fileCbOld = null;
      }
    }
  }
  private void startDownload(final String url, final String cd, final String userAgent) {
    if (url == null || url.length() == 0 || url.startsWith("blob:") || url.startsWith("data:")) {
      Ui.toast(this, "Download failed");
      return;
    }
    Ui.confirm(this, Lang.t("Download") + "?", new Runnable() {
      @Override
      public void run() {
        fetchBg(url, cd, userAgent);
      }
    });
  }
  private void fetchBg(final String url, final String cd, final String userAgent) {
    final java.lang.ref.WeakReference<HtmlActivity> href = new java.lang.ref.WeakReference<HtmlActivity>(this);
    Ui.bg(this, "Loading...", new Runnable() {
      @Override
      public void run() {
        File dst = fetch(url, cd, userAgent);
        final String msg = dst != null && dst.isFile()
          ? "Saved: " + dst.getName() + "\n" + dst.getAbsolutePath()
          : "Download failed";
        final HtmlActivity a = href.get();
        if (a == null) {
          return;
        }
        a.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Ui.toast(a, msg);
          }
        });
      }
    }, null);
  }
  private File fetch(String urlStr, String cd, String userAgent) {
    HttpURLConnection conn = null;
    FileOutputStream out = null;
    try {
      File dir = AppSettings.downloadDir();
      dir.mkdirs();
      URL url = new URL(urlStr);
      conn = (HttpURLConnection) url.openConnection();
      conn.setConnectTimeout(15000);
      conn.setReadTimeout(30000);
      conn.setInstanceFollowRedirects(true);
      String ua = userAgent;
      if (ua == null || ua.length() == 0) {
        ua = AppSettings.get().htmlUserAgent();
      }
      if (ua != null && ua.length() > 0) {
        conn.setRequestProperty("User-Agent", ua);
      }
      int code = conn.getResponseCode();
      if (code != 200 && code != 206) {
        return null;
      }
      String name = nameFrom(cd, url);
      File dst = new File(dir, TrashStore.uniqueName(dir, name));
      InputStream in = conn.getInputStream();
      out = new FileOutputStream(dst);
      byte[] buf = new byte[16384];
      int n;
      long max = 64L * 1024L * 1024L;
      long got = 0L;
      while ((n = in.read(buf)) > 0) {
        got += n;
        if (got > max) {
          break;
        }
        out.write(buf, 0, n);
      }
      try {
        in.close();
      } catch (Exception ignored) {}
      return dst;
    } catch (Exception e) {
      OpLog.err(this, "html download", e);
      return null;
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (Exception ignored) {}
      }
      if (conn != null) {
        conn.disconnect();
      }
    }
  }
  private static String nameFrom(String cd, URL url) {
    String fileName = null;
    if (cd != null) {
      String low = cd.toLowerCase(Locale.US);
      int idx = low.indexOf("filename=");
      if (idx >= 0) {
        fileName = cd.substring(idx + 9).replace("\"", "").trim();
        int sc = fileName.indexOf(';');
        if (sc >= 0) {
          fileName = fileName.substring(0, sc).trim();
        }
      }
    }
    if (fileName == null || fileName.length() == 0) {
      String p = url.getPath();
      if (p == null) {
        p = "";
      }
      int slash = p.lastIndexOf('/');
      fileName = slash >= 0 ? p.substring(slash + 1) : p;
    }
    if (fileName == null || fileName.length() == 0) {
      fileName = "download.bin";
    }
    fileName = fileName.replace('\\', '_').replace('/', '_');
    return fileName;
  }
  @Override
  public void onBackPressed() {
    if (wv != null && wv.canGoBack()) {
      wv.goBack();
      return;
    }
    super.onBackPressed();
  }
  @Override
  protected void onDestroy() {
    Ui.dismissOpen();
    if (fileCb != null) {
      try {
        fileCb.onReceiveValue(null);
      } catch (Exception ignored) {}
      fileCb = null;
    }
    if (fileCbOld != null) {
      try {
        fileCbOld.onReceiveValue(null);
      } catch (Exception ignored) {}
      fileCbOld = null;
    }
    if (wv != null) {
      try {
        wv.stopLoading();
        wv.loadUrl("about:blank");
        wv.clearHistory();
        wv.removeJavascriptInterface("FmBridge");
        wv.setWebChromeClient(null);
        wv.setWebViewClient(null);
        wv.removeAllViews();
        wv.destroy();
      } catch (Exception ignored) {}
      wv = null;
    }
    super.onDestroy();
  }
}
