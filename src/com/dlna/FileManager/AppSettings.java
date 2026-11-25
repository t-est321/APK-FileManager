package com.dlna.FileManager;
import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;
final class AppSettings {
  static final String[] SORT_NAMES = {
    "Name A-Z", "Date newest", "Size largest", "Name Z-A",
    "Date oldest", "Size smallest", "Extension", "Type"
  };
  static final String[] FONT_NAMES = {"Small", "Normal", "Large"};
  static final String[] ACCENT_NAMES = {"Blue", "Green", "Amber", "Purple", "Red", "Custom"};
  static final String[] OPEN_SIZE_NAMES = {
    "64 KB", "256 KB", "512 KB", "1 MB", "2 MB", "4 MB", "8 MB", "Custom"
  };
  static final long[] OPEN_SIZE_BYTES = {
    65536L, 262144L, 524288L, 1048576L, 2097152L, 4194304L, 8388608L, 524288L
  };
  static final int[] FONT_SP = {11, 13, 16};
  static final int[] LIST_SP = {12, 14, 16};
  static final int[] TRASH_DAYS = {7, 14, 30, 60, 90};
  static final String[] THUMB_NAMES = {"Small", "Normal", "Large"};
  static final int[] THUMB_PX = {96, 192, 256};
  static final int[] TAB_SIZES = {2, 4, 8};
  static final String[] SVG_RES_NAMES = {"512", "768", "1280", "1920"};
  static final int[] SVG_RES_PX = {512, 768, 1280, 1920};
  static final String[] SVG_BG_NAMES = {"White", "Black", "None"};
  boolean showHidden = false;
  int sortMode = 0;
  boolean keepScreenOn = false;
  boolean confirmDelete = true;
  boolean confirmPermanent = true;
  boolean editorWordWrap = false;
  boolean foldersFirst = true;
  boolean backGoesUp = true;
  int editorFont = 1;
  int listFont = 1;
  boolean showIcons = true;
  boolean showDetails = true;
  int accentColor = 0;
  int customAccent = 0xFF2E7BC7;
  int customFolder = 0xFFB99737;
  boolean mediaPreview = true;
  String extraTextExts = "";
  boolean darkTheme = true;
  int folderColor = 0;
  boolean iconGradient = true;
  boolean openBinary = false;
  int openSize = 2;
  long openSizeCustom = 524288L;
  boolean optNoThumbs = false;
  boolean optNoProbe = false;
  boolean langRu = false;
  boolean optNoArchiveBrowse = false;
  boolean optNoHtml = false;
  boolean optNoLog = true;
  boolean optNoRound = false;
  boolean optNoPkgIcons = false;
  boolean optNoSha256 = false;
  boolean optNoHeap = false;
  boolean hideNav = false;
  boolean hideMarks = false;
  boolean hideTabs = false;
  boolean hideSearch = false;
  boolean hideGlob = false;
  boolean hideRail = false;
  boolean hideTerm = false;
  boolean hideNotes = false;
  boolean hideStats = false;
  boolean hideLog = true;
  boolean hidePath = false;
  boolean hideShare = false;
  boolean hideCopyPath = false;
  boolean hideCopyName = false;
  boolean hideInfo = false;
  boolean relDate = false;
  boolean showFolderCounts = true;
  int trashDayIdx = 2;
  boolean confirmOverwrite = false;
  boolean optNoConfirmTab = false;
  boolean imgCompress = false;
  boolean useCustomChrome = false;
  static final int[] CHROME_DEF_D = new int[]{
    0xFF000000, 0xFFEDEDED, 0xFF080423, 0xFF050218, 0xFF050218
  };
  static final int[] CHROME_DEF_L = new int[]{
    0xFFE9E9E9, 0xFF000000, 0xFFFFFFFF, 0xFF808080, 0xFFC9C0FF
  };
  private static final String[] CHROME_K_D = new String[]{
    "customBg", "customText", "customSurface", "customStroke", "customSel"
  };
  private static final String[] CHROME_K_L = new String[]{
    "customBgL", "customTextL", "customSurfaceL", "customStrokeL", "customSelL"
  };
  int[] chromeD = CHROME_DEF_D.clone();
  int[] chromeL = CHROME_DEF_L.clone();
  int thumbSize = 1;
  int editorTabIdx = 1;
  boolean editorSpaces = true;
  boolean editorLineNo = true;
  int lastTab = 2;
  int svgResIdx = 3;
  boolean svgGrads = true;
  boolean svgRgb565 = false;
  int svgBgIdx = 0;
  boolean protectOn = false;
  String protectExtra = "";
  String protectList = "";
  String protectHash = "";
  int extOff = 0;
  boolean htmlJs = true;
  boolean htmlDom = true;
  boolean htmlZoom = false;
  boolean htmlImages = true;
  boolean htmlShowBar = true;
  boolean htmlHideBar = false;
  boolean htmlFileAccess = true;
  boolean htmlCache = true;
  boolean htmlWide = true;
  boolean htmlOverview = true;
  boolean htmlCopyAssets = true;
  boolean htmlClearOnOpen = true;
  int htmlUaMode = 0;
  String htmlUaCustom = "";
  static final String[] HTML_SCREEN_NAMES = {"Auto", "360", "768", "1024", "1280"};
  static final int[] HTML_SCREEN_PX = {0, 360, 768, 1024, 1280};
  int htmlScreenIdx = 0;
  boolean htmlHwAccel = true;
  static final String[] HTML_UA_NAMES = {"Default", "Mobile", "Desktop", "Custom"};
  static final String UA_MOBILE =
    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
  static final String UA_DESKTOP =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
  String navBar = "";
  String extImg = "";
  String extAudio = "";
  String extVideo = "";
  String extArch = "";
  String extText = "";
  String extCode = "";
  String extApk = "";
  String extPdf = "";
  static final class NavItem {
    String label;
    String path;
    boolean on;
  }
  private static AppSettings inst;
  static boolean holdSave = false;
  private AppSettings() {}
  static synchronized AppSettings get() {
    if (inst == null) {
      inst = new AppSettings();
    }
    return inst;
  }
  static File file(Context c) {
    return new File(c.getFilesDir(), "settings.json");
  }
  static File publicDir() {
    return new File(Environment.getExternalStorageDirectory(), "APP_DATA/com.dlna.FileManager");
  }
  static File publicSettings() {
    return new File(publicDir(), "settings.json");
  }
  static File extractDir() {
    return new File(publicDir(), "Extracted");
  }
  static File archiveDir() {
    File d = new File(publicDir(), "Archives");
    if (!d.exists()) {
      d.mkdirs();
    }
    return d;
  }
  static File downloadDir() {
    File d = new File(publicDir(), "DownloadedFiles");
    if (!d.exists()) {
      d.mkdirs();
    }
    return d;
  }
  static File incomingDir(Context c) {
    return cacheSub(c, "Incoming");
  }
  static File cacheSub(Context c, String name) {
    File d = new File(c.getCacheDir(), name);
    if (!d.exists()) {
      d.mkdirs();
    }
    return d;
  }
  static File installDir(Context c) {
    return cacheSub(c, "Install");
  }
  static File openAsDir(Context c) {
    return cacheSub(c, "OpenAs");
  }
  static File zipOpenDir(Context c) {
    return cacheSub(c, "ZipOpen");
  }
  static File htmlViewDir(Context c) {
    return cacheSub(c, "html_view");
  }
  String htmlUserAgent() {
    if (htmlUaMode == 1) {
      return UA_MOBILE;
    }
    if (htmlUaMode == 2) {
      return UA_DESKTOP;
    }
    if (htmlUaMode == 3 && htmlUaCustom != null && htmlUaCustom.length() > 0) {
      return htmlUaCustom;
    }
    return null;
  }
  static File notesFile(Context c) {
    return new File(c.getFilesDir(), "notes.txt");
  }
  static File tabsFile(Context c) {
    return new File(c.getFilesDir(), "tabs.txt");
  }
  static File publicNotes() {
    return new File(publicDir(), "notes.txt");
  }
  static File publicTabs() {
    return new File(publicDir(), "tabs.txt");
  }
  static File publicBookmarks() {
    return new File(publicDir(), "bookmarks.txt");
  }
  static final class TabItem {
    String name;
    String path;
  }
  static boolean tabsFilePresent(Context c) {
    File f = tabsFile(c);
    return f.isFile() && f.length() > 0L;
  }
  static List<TabItem> readTabs(Context c) {
    List<TabItem> out = new ArrayList<TabItem>();
    File f = tabsFile(c);
    if (!f.isFile() || f.length() == 0L) {
      return out;
    }
    try {
      String raw = FileOps.readString(f);
      String[] lines = raw.split("\n");
      for (int i = 0; i < lines.length; i++) {
        String line = lines[i].trim();
        if (line.length() == 0) {
          continue;
        }
        if (line.startsWith(".at\t")) {
          try {
            get().lastTab = Integer.parseInt(line.substring(4).trim());
          } catch (Exception ignored) {}
          continue;
        }
        TabItem t = new TabItem();
        int tab = line.indexOf('\t');
        if (tab >= 0) {
          t.name = line.substring(0, tab).trim();
          t.path = line.substring(tab + 1).trim();
        } else {
          t.name = "";
          t.path = line;
        }
        if (t.path.length() == 0) {
          t.path = "/";
        }
        out.add(t);
      }
    } catch (Exception ignored) {}
    return out;
  }
  static void writeTabs(Context c, List<TabItem> items) {
    if (holdSave || items == null || items.size() == 0) {
      return;
    }
    try {
      StringBuilder sb = new StringBuilder();
      sb.append(".at\t").append(get().lastTab);
      for (int i = 0; i < items.size(); i++) {
        sb.append('\n');
        TabItem t = items.get(i);
        String path = t.path == null || t.path.length() == 0 ? "/" : t.path;
        if (t.name != null && t.name.length() > 0) {
          sb.append(t.name).append('\t').append(path);
        } else {
          sb.append(path);
        }
      }
      String body = sb.toString();
      File f = tabsFile(c);
      if (f.isFile()) {
        try {
          if (body.equals(FileOps.readString(f))) {
            return;
          }
        } catch (Exception ignored) {}
      }
      FileOps.writeString(f, body);
    } catch (Exception ignored) {}
  }
  static boolean copySidecar(Context c, boolean toPublic) {
    try {
      File dir = publicDir();
      if (toPublic && !dir.exists()) {
        dir.mkdirs();
      }
      File srcT = toPublic ? tabsFile(c) : publicTabs();
      File dstT = toPublic ? publicTabs() : tabsFile(c);
      File srcN = toPublic ? notesFile(c) : publicNotes();
      File dstN = toPublic ? publicNotes() : notesFile(c);
      File srcB = toPublic ? Bookmarks.file(c) : publicBookmarks();
      File dstB = toPublic ? publicBookmarks() : Bookmarks.file(c);
      if (srcT.isFile()) {
        FileOps.copyRec(srcT, dstT);
      }
      if (srcN.isFile()) {
        FileOps.copyRec(srcN, dstN);
      }
      if (srcB.isFile()) {
        FileOps.copyRec(srcB, dstB);
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }
  static synchronized void load(Context c) {
    AppSettings s = get();
    try {
      s.applyJson(new JSONObject(FileOps.readString(file(c))));
    } catch (Exception ignored) {}
  }
  synchronized void save(Context c) {
    if (holdSave) {
      return;
    }
    try {
      FileOps.writeString(file(c), toJson().toString());
    } catch (Exception ignored) {}
  }
  JSONObject toJson() {
    JSONObject j = new JSONObject();
    try {
      j.put("showHidden", showHidden);
      j.put("sortMode", sortMode);
      j.put("keepScreenOn", keepScreenOn);
      j.put("confirmDelete", confirmDelete);
      j.put("confirmPermanent", confirmPermanent);
      j.put("editorWordWrap", editorWordWrap);
      j.put("foldersFirst", foldersFirst);
      j.put("backGoesUp", backGoesUp);
      j.put("editorFont", editorFont);
      j.put("listFont", listFont);
      j.put("showIcons", showIcons);
      j.put("showDetails", showDetails);
      j.put("accentColor", accentColor);
      j.put("customAccent", customAccent);
      j.put("customFolder", customFolder);
      j.put("mediaPreview", mediaPreview);
      j.put("extraTextExts", extraTextExts);
      j.put("darkTheme", darkTheme);
      j.put("folderColor", folderColor);
      j.put("iconGradient", iconGradient);
      j.put("openBinary", openBinary);
      j.put("openSize", openSize);
      j.put("openSizeCustom", openSizeCustom);
      j.put("optNoThumbs", optNoThumbs);
      j.put("optNoProbe", optNoProbe);
      j.put("langRu", langRu);
      j.put("optNoArchiveBrowse", optNoArchiveBrowse);
      j.put("optNoHtml", optNoHtml);
      j.put("optNoLog", optNoLog);
      j.put("optNoRound", optNoRound);
      j.put("optNoPkgIcons", optNoPkgIcons);
      j.put("optNoSha256", optNoSha256);
      j.put("optNoHeap", optNoHeap);
      j.put("hideNav", hideNav);
      j.put("hideMarks", hideMarks);
      j.put("hideTabs", hideTabs);
      j.put("hideSearch", hideSearch);
      j.put("hideGlob", hideGlob);
      j.put("hideRail", hideRail);
      j.put("hideTerm", hideTerm);
      j.put("hideNotes", hideNotes);
      j.put("hideStats", hideStats);
      j.put("hideLog", hideLog);
      j.put("hidePath", hidePath);
      j.put("hideShare", hideShare);
      j.put("hideCopyPath", hideCopyPath);
      j.put("hideCopyName", hideCopyName);
      j.put("hideInfo", hideInfo);
      j.put("relDate", relDate);
      j.put("showFolderCounts", showFolderCounts);
      j.put("trashDayIdx", trashDayIdx);
      j.put("confirmOverwrite", confirmOverwrite);
      j.put("optNoConfirmTab", optNoConfirmTab);
      j.put("imgCompress", imgCompress);
      j.put("useCustomChrome", useCustomChrome);
      int ci;
      for (ci = 0; ci < 5; ci++) {
        j.put(CHROME_K_D[ci], chromeD[ci] & 0xFFFFFFFFL);
        j.put(CHROME_K_L[ci], chromeL[ci] & 0xFFFFFFFFL);
      }
      j.put("thumbSize", thumbSize);
      j.put("editorTabIdx", editorTabIdx);
      j.put("editorSpaces", editorSpaces);
      j.put("editorLineNo", editorLineNo);
      j.put("lastTab", lastTab);
      j.put("svgResIdx", svgResIdx);
      j.put("svgGrads", svgGrads);
      j.put("svgRgb565", svgRgb565);
      j.put("svgBgIdx", svgBgIdx);
      j.put("protectOn", protectOn);
      j.put("protectExtra", protectExtra == null ? "" : protectExtra);
      j.put("protectList", protectList == null ? "" : protectList);
      j.put("protectHash", protectHash == null ? "" : protectHash);
      j.put("extOff", extOff);
      j.put("htmlJs", htmlJs);
      j.put("htmlDom", htmlDom);
      j.put("htmlZoom", htmlZoom);
      j.put("htmlImages", htmlImages);
      j.put("htmlShowBar", htmlShowBar);
      j.put("htmlHideBar", htmlHideBar);
      j.put("htmlFileAccess", htmlFileAccess);
      j.put("htmlCache", htmlCache);
      j.put("htmlWide", htmlWide);
      j.put("htmlOverview", htmlOverview);
      j.put("htmlCopyAssets", htmlCopyAssets);
      j.put("htmlClearOnOpen", htmlClearOnOpen);
      j.put("htmlUaMode", htmlUaMode);
      j.put("htmlUaCustom", htmlUaCustom == null ? "" : htmlUaCustom);
      j.put("htmlScreenIdx", htmlScreenIdx);
      j.put("htmlHwAccel", htmlHwAccel);
      j.put("navBar", navBar);
      j.put("extImg", extImg);
      j.put("extAudio", extAudio);
      j.put("extVideo", extVideo);
      j.put("extArch", extArch);
      j.put("extText", extText);
      j.put("extCode", extCode);
      j.put("extApk", extApk);
      j.put("extPdf", extPdf);
    } catch (Exception ignored) {}
    return j;
  }
  void applyJson(JSONObject j) {
    showHidden = j.optBoolean("showHidden", showHidden);
    sortMode = j.optInt("sortMode", sortMode);
    if (sortMode < 0 || sortMode >= SORT_NAMES.length) {
      sortMode = 0;
    }
    keepScreenOn = j.optBoolean("keepScreenOn", keepScreenOn);
    confirmDelete = j.optBoolean("confirmDelete", confirmDelete);
    confirmPermanent = j.optBoolean("confirmPermanent", confirmPermanent);
    editorWordWrap = j.optBoolean("editorWordWrap", editorWordWrap);
    foldersFirst = j.optBoolean("foldersFirst", foldersFirst);
    backGoesUp = j.optBoolean("backGoesUp", backGoesUp);
    editorFont = j.optInt("editorFont", editorFont);
    if (editorFont < 0 || editorFont >= FONT_NAMES.length) {
      editorFont = 1;
    }
    listFont = j.optInt("listFont", listFont);
    if (listFont < 0 || listFont >= FONT_NAMES.length) {
      listFont = 1;
    }
    showIcons = j.optBoolean("showIcons", showIcons);
    showDetails = j.optBoolean("showDetails", showDetails);
    accentColor = j.optInt("accentColor", accentColor);
    if (accentColor < 0 || accentColor >= ACCENT_NAMES.length) {
      accentColor = 0;
    }
    mediaPreview = j.optBoolean("mediaPreview", mediaPreview);
    extraTextExts = normExts(j.optString("extraTextExts", extraTextExts));
    darkTheme = j.optBoolean("darkTheme", darkTheme);
    folderColor = j.optInt("folderColor", folderColor);
    if (folderColor < 0 || folderColor >= ACCENT_NAMES.length) {
      folderColor = 0;
    }
    customAccent = (int) j.optLong("customAccent", customAccent);
    customFolder = (int) j.optLong("customFolder", customFolder);
    iconGradient = j.optBoolean("iconGradient", iconGradient);
    openBinary = j.optBoolean("openBinary", openBinary);
    openSize = j.optInt("openSize", openSize);
    if (openSize < 0 || openSize >= OPEN_SIZE_NAMES.length) {
      openSize = 2;
    }
    openSizeCustom = j.optLong("openSizeCustom", openSizeCustom);
    if (openSizeCustom < 1024L) {
      openSizeCustom = 1024L;
    }
    optNoThumbs = j.optBoolean("optNoThumbs", optNoThumbs);
    optNoProbe = j.optBoolean("optNoProbe", optNoProbe);
    langRu = j.optBoolean("langRu", langRu);
    optNoArchiveBrowse = j.optBoolean("optNoArchiveBrowse", optNoArchiveBrowse);
    optNoHtml = j.optBoolean("optNoHtml", optNoHtml);
    optNoLog = j.optBoolean("optNoLog", optNoLog);
    optNoRound = j.optBoolean("optNoRound", optNoRound);
    optNoPkgIcons = j.optBoolean("optNoPkgIcons", optNoPkgIcons);
    optNoSha256 = j.optBoolean("optNoSha256", optNoSha256);
    optNoHeap = j.optBoolean("optNoHeap", optNoHeap);
    hideNav = j.optBoolean("hideNav", hideNav);
    hideMarks = j.optBoolean("hideMarks", hideMarks);
    hideTabs = j.optBoolean("hideTabs", hideTabs);
    hideSearch = j.optBoolean("hideSearch", hideSearch);
    hideGlob = j.optBoolean("hideGlob", hideGlob);
    hideRail = j.optBoolean("hideRail", hideRail);
    hideTerm = j.optBoolean("hideTerm", hideTerm);
    hideNotes = j.optBoolean("hideNotes", hideNotes);
    hideStats = j.optBoolean("hideStats", hideStats);
    hideLog = j.optBoolean("hideLog", hideLog);
    hidePath = j.optBoolean("hidePath", hidePath);
    hideShare = j.optBoolean("hideShare", hideShare);
    hideCopyPath = j.optBoolean("hideCopyPath", hideCopyPath);
    hideCopyName = j.optBoolean("hideCopyName", hideCopyName);
    hideInfo = j.optBoolean("hideInfo", hideInfo);
    relDate = j.optBoolean("relDate", relDate);
    showFolderCounts = j.optBoolean("showFolderCounts", showFolderCounts);
    trashDayIdx = j.optInt("trashDayIdx", trashDayIdx);
    if (trashDayIdx < 0 || trashDayIdx >= TRASH_DAYS.length) {
      trashDayIdx = 2;
    }
    confirmOverwrite = j.optBoolean("confirmOverwrite", confirmOverwrite);
    optNoConfirmTab = j.optBoolean("optNoConfirmTab", optNoConfirmTab);
    imgCompress = j.optBoolean("imgCompress", imgCompress);
    useCustomChrome = j.optBoolean("useCustomChrome", useCustomChrome);
    int ci;
    for (ci = 0; ci < 5; ci++) {
      chromeD[ci] = (int) j.optLong(CHROME_K_D[ci], chromeD[ci]);
      chromeL[ci] = (int) j.optLong(CHROME_K_L[ci], chromeL[ci]);
    }
    thumbSize = j.optInt("thumbSize", thumbSize);
    if (thumbSize < 0 || thumbSize >= THUMB_PX.length) {
      thumbSize = 1;
    }
    editorTabIdx = j.optInt("editorTabIdx", editorTabIdx);
    if (editorTabIdx < 0 || editorTabIdx >= TAB_SIZES.length) {
      editorTabIdx = 1;
    }
    editorSpaces = j.optBoolean("editorSpaces", editorSpaces);
    editorLineNo = j.optBoolean("editorLineNo", editorLineNo);
    lastTab = j.optInt("lastTab", lastTab);
    svgResIdx = j.optInt("svgResIdx", svgResIdx);
    if (svgResIdx < 0 || svgResIdx >= SVG_RES_PX.length) {
      svgResIdx = 2;
    }
    svgGrads = j.optBoolean("svgGrads", svgGrads);
    svgRgb565 = j.optBoolean("svgRgb565", svgRgb565);
    svgBgIdx = j.optInt("svgBgIdx", svgBgIdx);
    if (svgBgIdx < 0 || svgBgIdx >= SVG_BG_NAMES.length) {
      svgBgIdx = 0;
    }
    protectOn = j.optBoolean("protectOn", protectOn);
    protectExtra = j.optString("protectExtra", protectExtra);
    protectList = j.optString("protectList", protectList);
    protectHash = j.optString("protectHash", protectHash);
    extOff = j.optInt("extOff", extOff);
    htmlJs = j.optBoolean("htmlJs", htmlJs);
    htmlDom = j.optBoolean("htmlDom", htmlDom);
    htmlZoom = j.optBoolean("htmlZoom", htmlZoom);
    htmlImages = j.optBoolean("htmlImages", htmlImages);
    htmlShowBar = j.optBoolean("htmlShowBar", htmlShowBar);
    htmlHideBar = j.optBoolean("htmlHideBar", htmlHideBar);
    htmlFileAccess = j.optBoolean("htmlFileAccess", htmlFileAccess);
    htmlCache = j.optBoolean("htmlCache", htmlCache);
    htmlWide = j.optBoolean("htmlWide", htmlWide);
    htmlOverview = j.optBoolean("htmlOverview", htmlOverview);
    htmlCopyAssets = j.optBoolean("htmlCopyAssets", htmlCopyAssets);
    htmlClearOnOpen = j.optBoolean("htmlClearOnOpen", htmlClearOnOpen);
    htmlUaMode = j.optInt("htmlUaMode", htmlUaMode);
    if (htmlUaMode < 0 || htmlUaMode >= HTML_UA_NAMES.length) {
      htmlUaMode = 0;
    }
    htmlUaCustom = j.optString("htmlUaCustom", htmlUaCustom);
    htmlScreenIdx = j.optInt("htmlScreenIdx", htmlScreenIdx);
    if (htmlScreenIdx < 0 || htmlScreenIdx >= HTML_SCREEN_PX.length) {
      htmlScreenIdx = 0;
    }
    htmlHwAccel = j.optBoolean("htmlHwAccel", htmlHwAccel);
    navBar = j.optString("navBar", navBar);
    extImg = normExts(j.optString("extImg", extImg));
    extAudio = normExts(j.optString("extAudio", extAudio));
    extVideo = normExts(j.optString("extVideo", extVideo));
    extArch = normExts(j.optString("extArch", extArch));
    extText = normExts(j.optString("extText", extText));
    extCode = normExts(j.optString("extCode", extCode));
    extApk = normExts(j.optString("extApk", extApk));
    extPdf = normExts(j.optString("extPdf", extPdf));
  }
  void reset() {
    showHidden = false;
    sortMode = 0;
    keepScreenOn = false;
    confirmDelete = true;
    confirmPermanent = true;
    editorWordWrap = false;
    foldersFirst = true;
    backGoesUp = true;
    editorFont = 1;
    listFont = 1;
    showIcons = true;
    showDetails = true;
    accentColor = 0;
    customAccent = 0xFF2E7BC7;
    customFolder = 0xFFB99737;
    mediaPreview = true;
    extraTextExts = "";
    darkTheme = true;
    folderColor = 0;
    iconGradient = true;
    openBinary = false;
    openSize = 2;
    openSizeCustom = 524288L;
    optNoThumbs = false;
    optNoProbe = false;
    langRu = false;
    optNoArchiveBrowse = false;
    optNoHtml = false;
    optNoLog = true;
    optNoRound = false;
    optNoPkgIcons = false;
    optNoSha256 = false;
    optNoHeap = false;
    hideNav = false;
    hideMarks = false;
    hideTabs = false;
    hideSearch = false;
    hideGlob = false;
    hideRail = false;
    hideTerm = false;
    hideNotes = false;
    hideStats = false;
    hideLog = true;
    hidePath = false;
    hideShare = false;
    hideCopyPath = false;
    hideCopyName = false;
    hideInfo = false;
    relDate = false;
    showFolderCounts = true;
    trashDayIdx = 2;
    confirmOverwrite = false;
    optNoConfirmTab = false;
    imgCompress = false;
    useCustomChrome = false;
    System.arraycopy(CHROME_DEF_D, 0, chromeD, 0, 5);
    System.arraycopy(CHROME_DEF_L, 0, chromeL, 0, 5);
    thumbSize = 1;
    editorTabIdx = 1;
    editorSpaces = true;
    editorLineNo = true;
    lastTab = 2;
    svgResIdx = 3;
    svgGrads = true;
    svgRgb565 = false;
    svgBgIdx = 0;
    protectOn = false;
    protectExtra = "";
    protectList = "";
    protectHash = "";
    extOff = 0;
    htmlJs = true;
    htmlDom = true;
    htmlZoom = false;
    htmlImages = true;
    htmlShowBar = true;
    htmlHideBar = false;
    htmlFileAccess = true;
    htmlCache = true;
    htmlWide = true;
    htmlOverview = true;
    htmlCopyAssets = true;
    htmlClearOnOpen = true;
    htmlUaMode = 0;
    htmlUaCustom = "";
    htmlScreenIdx = 0;
    htmlHwAccel = true;
    navBar = "";
    extImg = "";
    extAudio = "";
    extVideo = "";
    extArch = "";
    extText = "";
    extCode = "";
    extApk = "";
    extPdf = "";
  }
  static List<NavItem> defaultNav() {
    List<NavItem> out = new ArrayList<NavItem>();
    out.add(nav("/", "/", true));
    out.add(nav("SD", "$SD", true));
    out.add(nav("Android", "$ANDROID", true));
    out.add(nav("Data", "/data", true));
    out.add(nav("App Data", "$APP", true));
    return out;
  }
  private static NavItem nav(String label, String path, boolean on) {
    NavItem n = new NavItem();
    n.label = label;
    n.path = path;
    n.on = on;
    return n;
  }
  List<NavItem> navItems() {
    if (navBar == null || navBar.length() == 0) {
      return defaultNav();
    }
    List<NavItem> out = new ArrayList<NavItem>();
    String[] lines = navBar.split("\n");
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      if (line.length() == 0) {
        continue;
      }
      String[] p = split3(line);
      NavItem n = new NavItem();
      n.label = p[0];
      n.path = p[1];
      n.on = !"0".equals(p[2]);
      if (n.label.length() > 0 && n.path.length() > 0) {
        out.add(n);
      }
    }
    return out.size() > 0 ? out : defaultNav();
  }
  void setNavItems(List<NavItem> items) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) {
        sb.append('\n');
      }
      NavItem n = items.get(i);
      sb.append(n.label).append('\t').append(n.path).append('\t').append(n.on ? "1" : "0");
    }
    navBar = sb.toString();
  }
  private static String[] split3(String line) {
    String[] out = new String[]{"", "", "1"};
    int a = line.indexOf('\t');
    if (a < 0) {
      out[0] = line;
      out[1] = line;
      return out;
    }
    out[0] = line.substring(0, a);
    int b = line.indexOf('\t', a + 1);
    if (b < 0) {
      out[1] = line.substring(a + 1);
      return out;
    }
    out[1] = line.substring(a + 1, b);
    out[2] = line.substring(b + 1);
    return out;
  }
  static File resolveNav(Context c, String path) {
    if (path == null || path.length() == 0 || "$ROOT".equals(path) || "/".equals(path)) {
      return new File("/");
    }
    if ("$SD".equals(path)) {
      return Environment.getExternalStorageDirectory();
    }
    if ("$ANDROID".equals(path)) {
      return new File(Environment.getExternalStorageDirectory(), "Android/data");
    }
    if ("$APP".equals(path)) {
      File p = c.getFilesDir().getParentFile();
      return p != null ? p : c.getFilesDir();
    }
    return new File(path);
  }
  static String[] defaultProtect() {
    return new String[] {
      "/data/user/0/com.dlna.FileManager",
      "/data/data/com.dlna.FileManager",
      "/storage/emulated/0/APP_DATA/com.dlna.FileManager"
    };
  }
  String[] protectPaths() {
    ArrayList<String> out = parseProtectRaw(protectList);
    if (out.size() > 0) {
      return out.toArray(new String[out.size()]);
    }
    String[] def = defaultProtect();
    for (int i = 0; i < def.length; i++) {
      out.add(def[i]);
    }
    try {
      File pub = publicDir();
      if (pub != null) {
        String p = pub.getAbsolutePath();
        if (!out.contains(p)) {
          out.add(p);
        }
      }
    } catch (Exception ignored) {}
    if (protectExtra != null && protectExtra.length() > 0) {
      String[] lines = protectExtra.split("\n");
      for (int i = 0; i < lines.length; i++) {
        addUnique(out, lines[i]);
      }
    }
    return out.toArray(new String[out.size()]);
  }
  private static ArrayList<String> parseProtectRaw(String raw) {
    ArrayList<String> out = new ArrayList<String>();
    if (raw == null || raw.length() == 0) {
      return out;
    }
    String[] lines = raw.split("\n");
    for (int i = 0; i < lines.length; i++) {
      addUnique(out, lines[i]);
    }
    return out;
  }
  private static void addUnique(ArrayList<String> out, String line) {
    if (line == null) {
      return;
    }
    line = line.trim();
    while (line.endsWith("/") && line.length() > 1) {
      line = line.substring(0, line.length() - 1);
    }
    if (line.length() == 0) {
      return;
    }
    if (!out.contains(line)) {
      out.add(line);
    }
  }
  private void writeProtectList(ArrayList<String> items) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) {
        sb.append('\n');
      }
      sb.append(items.get(i));
    }
    protectList = sb.toString();
  }
  void materializeProtect() {
    if (protectList != null && protectList.length() > 0) {
      return;
    }
    String[] cur = protectPaths();
    ArrayList<String> items = new ArrayList<String>();
    for (int i = 0; i < cur.length; i++) {
      items.add(cur[i]);
    }
    writeProtectList(items);
  }
  void addProtect(String path) {
    materializeProtect();
    ArrayList<String> items = parseProtectRaw(protectList);
    addUnique(items, path);
    writeProtectList(items);
  }
  void removeProtect(String path) {
    materializeProtect();
    if (path == null) {
      return;
    }
    ArrayList<String> items = parseProtectRaw(protectList);
    String n = path.trim();
    while (n.endsWith("/") && n.length() > 1) {
      n = n.substring(0, n.length() - 1);
    }
    items.remove(n);
    writeProtectList(items);
  }
  void replaceProtect(String from, String to) {
    materializeProtect();
    ArrayList<String> items = parseProtectRaw(protectList);
    String n = from == null ? "" : from.trim();
    int i = items.indexOf(n);
    if (i < 0) {
      addProtect(to);
      return;
    }
    items.remove(i);
    addUnique(items, to);
    writeProtectList(items);
  }
  boolean extGroupOn(int kind) {
    if (kind < 0 || kind > 31) {
      return true;
    }
    return (extOff & (1 << kind)) == 0;
  }
  void toggleExtGroup(int kind) {
    if (kind < 0 || kind > 31) {
      return;
    }
    extOff ^= (1 << kind);
  }
  boolean hasLockPass() {
    return protectHash != null && protectHash.length() > 0;
  }
  boolean checkLockPass(String pass) {
    if (!hasLockPass()) {
      return true;
    }
    return protectHash.equals(FileOps.sha256Hex(pass == null ? "" : pass));
  }
  void setLockPass(String pass) {
    if (pass == null || pass.length() == 0) {
      protectHash = "";
      return;
    }
    protectHash = FileOps.sha256Hex(pass);
  }
  int trashDays() {
    int i = trashDayIdx;
    if (i < 0 || i >= TRASH_DAYS.length) {
      i = 2;
    }
    return TRASH_DAYS[i];
  }
  int thumbPx() {
    int i = thumbSize;
    if (i < 0 || i >= THUMB_PX.length) {
      i = 1;
    }
    return THUMB_PX[i];
  }
  int tabSpaces() {
    int i = editorTabIdx;
    if (i < 0 || i >= TAB_SIZES.length) {
      i = 1;
    }
    return TAB_SIZES[i];
  }
  int svgRes() {
    int i = svgResIdx;
    if (i < 0 || i >= SVG_RES_PX.length) {
      i = 2;
    }
    return SVG_RES_PX[i];
  }
  int svgBg() {
    int i = svgBgIdx;
    if (i == 1) {
      return 0xFF000000;
    }
    if (i == 2) {
      return 0;
    }
    return 0xFFFFFFFF;
  }
  long openLimit() {
    if (openSize == OPEN_SIZE_NAMES.length - 1) {
      return openSizeCustom < 1024L ? 1024L : openSizeCustom;
    }
    int i = openSize;
    if (i < 0 || i >= OPEN_SIZE_BYTES.length) {
      i = 2;
    }
    return OPEN_SIZE_BYTES[i];
  }
  int mapExt(String e) {
    if (e == null || e.length() == 0) {
      return -1;
    }
    String n = "n." + e;
    if (extImg.length() > 0 && FileOps.extListed(extImg, n)) {
      return FileGlyph.IMG;
    }
    if (extAudio.length() > 0 && FileOps.extListed(extAudio, n)) {
      return FileGlyph.AUDIO;
    }
    if (extVideo.length() > 0 && FileOps.extListed(extVideo, n)) {
      return FileGlyph.VIDEO;
    }
    if (extArch.length() > 0 && FileOps.extListed(extArch, n)) {
      return FileGlyph.ARCH;
    }
    if (extApk.length() > 0 && FileOps.extListed(extApk, n)) {
      return FileGlyph.APK;
    }
    if (extPdf.length() > 0 && FileOps.extListed(extPdf, n)) {
      return FileGlyph.PDF;
    }
    if (extCode.length() > 0 && FileOps.extListed(extCode, n)) {
      return FileGlyph.CODE;
    }
    if (extText.length() > 0 && FileOps.extListed(extText, n)) {
      return FileGlyph.TEXT;
    }
    return -1;
  }
  String extCsv(int kind) {
    if (kind == FileGlyph.IMG) {
      return extImg;
    }
    if (kind == FileGlyph.AUDIO) {
      return extAudio;
    }
    if (kind == FileGlyph.VIDEO) {
      return extVideo;
    }
    if (kind == FileGlyph.ARCH) {
      return extArch;
    }
    if (kind == FileGlyph.APK) {
      return extApk;
    }
    if (kind == FileGlyph.PDF) {
      return extPdf;
    }
    if (kind == FileGlyph.CODE) {
      return extCode;
    }
    if (kind == FileGlyph.TEXT) {
      return extText;
    }
    return "";
  }
  void setExtCsv(int kind, String csv) {
    String v = normExts(csv);
    if (kind == FileGlyph.IMG) {
      extImg = v;
    } else if (kind == FileGlyph.AUDIO) {
      extAudio = v;
    } else if (kind == FileGlyph.VIDEO) {
      extVideo = v;
    } else if (kind == FileGlyph.ARCH) {
      extArch = v;
    } else if (kind == FileGlyph.APK) {
      extApk = v;
    } else if (kind == FileGlyph.PDF) {
      extPdf = v;
    } else if (kind == FileGlyph.CODE) {
      extCode = v;
    } else if (kind == FileGlyph.TEXT) {
      extText = v;
    }
  }
  static String normExts(String s) {
    if (s == null) {
      return "";
    }
    String[] parts = s.toLowerCase(Locale.US).split("[,;\\s]+");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      String p = parts[i];
      while (p.startsWith(".")) {
        p = p.substring(1);
      }
      if (p.length() == 0) {
        continue;
      }
      String token = "," + p + ",";
      if (("," + sb.toString() + ",").contains(token)) {
        continue;
      }
      if (sb.length() > 0) {
        sb.append(',');
      }
      sb.append(p);
    }
    return sb.toString();
  }
  synchronized boolean exportToPublic() {
    try {
      File dst = publicSettings();
      File parent = dst.getParentFile();
      if (parent != null && !parent.exists()) {
        parent.mkdirs();
      }
      FileOps.writeString(dst, toJson().toString());
      return true;
    } catch (Exception e) {
      return false;
    }
  }
  synchronized boolean importFromPublic(Context c) {
    try {
      holdSave = true;
      applyJson(new JSONObject(FileOps.readString(publicSettings())));
      copySidecar(c, false);
      FileOps.writeString(file(c), toJson().toString());
      return true;
    } catch (Exception e) {
      holdSave = false;
      return false;
    }
  }
}
