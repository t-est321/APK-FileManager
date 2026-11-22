package com.dlna.FileManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import org.json.JSONObject;
public final class SettingsActivity extends Activity {
  private LinearLayout rows;
  private boolean changed = false;
  private boolean themeChanged = false;
  private String snap = "";
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    AppSettings.load(this);
    Ui.applyTheme(this);
    super.onCreate(savedInstanceState);
    Ui.bar(this);
    ScrollView scroll = new ScrollView(this);
    rows = new LinearLayout(this);
    rows.setOrientation(LinearLayout.VERTICAL);
    rows.setBackgroundColor(Theme.BG);
    scroll.addView(rows);
    setContentView(scroll);
    Ui.styleScroll(this, scroll);
    try {
      snap = AppSettings.get().toJson().toString();
    } catch (Exception e) {
      snap = "";
    }
    rebuild();
    applyKeepScreenOn();
  }
  @Override
  public void onBackPressed() {
    AppSettings.get().save(this);
    setResult(RESULT_OK);
    super.onBackPressed();
  }
  @Override
  protected void onDestroy() {
    Ui.dismissOpen();
    super.onDestroy();
  }
  private void backWithoutSave() {
    try {
      if (snap != null && snap.length() > 0) {
        AppSettings.get().applyJson(new JSONObject(snap));
        Theme.applyAccent(AppSettings.get().accentColor, AppSettings.get().darkTheme);
      }
    } catch (Exception ignored) {}
    setResult(RESULT_CANCELED);
    finish();
  }
  private void rebuild() {
    final AppSettings s = AppSettings.get();
    rows.setBackgroundColor(Theme.BG);
    rows.removeAllViews();
    addHeader(Lang.t("Settings"), "v4.2.1");
    rows.addView(actionBtn(Lang.t("Back without save"), Theme.DANGER, new Runnable() {
      @Override
      public void run() {
        backWithoutSave();
      }
    }));
    addHeader(Lang.t("Appearance"), null);
    rows.addView(cycleBtn(Lang.theme(), new Runnable() {
      @Override
      public void run() {
        s.darkTheme = !s.darkTheme;
        Theme.applyAccent(s.accentColor, s.darkTheme);
        themeChanged = true;
      }
    }));
    rows.addView(cycleBtn(Lang.language(), new Runnable() {
      @Override
      public void run() {
        s.langRu = !s.langRu;
        themeChanged = true;
      }
    }));
    addHeader(Lang.t("Color"), null);
    rows.addView(actionBtn(Lang.t("Accent color: ") + AppSettings.ACCENT_NAMES[s.accentColor] + "   \u25cf",
      Theme.ACCENT, new Runnable() {
        @Override
        public void run() {
          pickColor(false);
        }
      }));
    rows.addView(actionBtn(Lang.t("Folder color: ") + AppSettings.ACCENT_NAMES[s.folderColor],
      Theme.ACCENT, new Runnable() {
        @Override
        public void run() {
          pickColor(true);
        }
      }));
    rows.addView(actionBtn(Lang.t("Custom accent") + ": " + Theme.toHex(s.customAccent),
      Theme.ACCENT, new Runnable() {
        @Override
        public void run() {
          pickCustom(false);
        }
      }));
    rows.addView(actionBtn(Lang.t("Custom folder") + ": " + Theme.toHex(s.customFolder),
      Theme.ACCENT, new Runnable() {
        @Override
        public void run() {
          pickCustom(true);
        }
      }));
    rows.addView(toggleBtn(Lang.t("Custom chrome colors"), s.useCustomChrome, new Runnable() {
      @Override
      public void run() {
        s.useCustomChrome = !s.useCustomChrome;
        Theme.applyAccent(s.accentColor, s.darkTheme);
        themeChanged = true;
      }
    }));
    addHeader(Lang.t("Custom dark"), null);
    String[] chromeLabs = {
      Lang.t("Custom background"), Lang.t("Custom text"), Lang.t("Custom surface"),
      Lang.t("Custom outline"), Lang.t("Custom selection")
    };
    int ci;
    for (ci = 0; ci < 5; ci++) {
      addChromeRow(rows, chromeLabs[ci], false, ci);
    }
    addHeader(Lang.t("Custom light"), null);
    for (ci = 0; ci < 5; ci++) {
      addChromeRow(rows, chromeLabs[ci] + " (L)", true, ci);
    }
    addHeader(Lang.t("Browser"), "Listing, sort, hidden files");
    rows.addView(toggleBtn(Lang.t("Show hidden files"), s.showHidden, new Runnable() {
      @Override
      public void run() {
        s.showHidden = !s.showHidden;
      }
    }));
    rows.addView(cycleBtn(Lang.t("Sort: ") + AppSettings.SORT_NAMES[s.sortMode], new Runnable() {
      @Override
      public void run() {
        s.sortMode = (s.sortMode + 1) % AppSettings.SORT_NAMES.length;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Folders first"), s.foldersFirst, new Runnable() {
      @Override
      public void run() {
        s.foldersFirst = !s.foldersFirst;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Back button goes up"), s.backGoesUp, new Runnable() {
      @Override
      public void run() {
        s.backGoesUp = !s.backGoesUp;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Keep screen on"), s.keepScreenOn, new Runnable() {
      @Override
      public void run() {
        s.keepScreenOn = !s.keepScreenOn;
        Ui.applyKeepScreen(SettingsActivity.this);
        applyKeepScreenOn();
      }
    }));
    rows.addView(actionBtn(Lang.t("Help"), Theme.GOOD, new Runnable() {
      @Override
      public void run() {
        showHelp();
      }
    }));
    addHeader(Lang.t("Hide UI"), null);
    rows.addView(toggleBtn(Lang.t("Hide navigation chips"), s.hideNav, new Runnable() {
      @Override
      public void run() { s.hideNav = !s.hideNav; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide bookmarks"), s.hideMarks, new Runnable() {
      @Override
      public void run() { s.hideMarks = !s.hideMarks; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide tabs"), s.hideTabs, new Runnable() {
      @Override
      public void run() { s.hideTabs = !s.hideTabs; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide search"), s.hideSearch, new Runnable() {
      @Override
      public void run() { s.hideSearch = !s.hideSearch; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide glob chip"), s.hideGlob, new Runnable() {
      @Override
      public void run() { s.hideGlob = !s.hideGlob; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide scroll rail"), s.hideRail, new Runnable() {
      @Override
      public void run() { s.hideRail = !s.hideRail; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide Term button"), s.hideTerm, new Runnable() {
      @Override
      public void run() { s.hideTerm = !s.hideTerm; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide Stats button"), s.hideStats, new Runnable() {
      @Override
      public void run() { s.hideStats = !s.hideStats; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide Log button"), s.hideLog, new Runnable() {
      @Override
      public void run() { s.hideLog = !s.hideLog; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide path bar"), s.hidePath, new Runnable() {
      @Override
      public void run() { s.hidePath = !s.hidePath; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide Share"), s.hideShare, new Runnable() {
      @Override
      public void run() { s.hideShare = !s.hideShare; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide Copy path"), s.hideCopyPath, new Runnable() {
      @Override
      public void run() { s.hideCopyPath = !s.hideCopyPath; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide Copy name"), s.hideCopyName, new Runnable() {
      @Override
      public void run() { s.hideCopyName = !s.hideCopyName; }
    }));
    rows.addView(toggleBtn(Lang.t("Hide Info"), s.hideInfo, new Runnable() {
      @Override
      public void run() { s.hideInfo = !s.hideInfo; }
    }));
    addHeader(Lang.t("Interface"), "Fonts, icons, row details");
    rows.addView(cycleBtn(Lang.t("List font: ") + AppSettings.FONT_NAMES[s.listFont], new Runnable() {
      @Override
      public void run() {
        s.listFont = (s.listFont + 1) % AppSettings.FONT_NAMES.length;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Show file icons"), s.showIcons, new Runnable() {
      @Override
      public void run() {
        s.showIcons = !s.showIcons;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Show size and date"), s.showDetails, new Runnable() {
      @Override
      public void run() {
        s.showDetails = !s.showDetails;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Relative dates"), s.relDate, new Runnable() {
      @Override
      public void run() {
        s.relDate = !s.relDate;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Show folder item counts"), s.showFolderCounts, new Runnable() {
      @Override
      public void run() {
        s.showFolderCounts = !s.showFolderCounts;
      }
    }));
    addHeader(Lang.t("Files"), "Open rules for viewer and editor");
    rows.addView(toggleBtn(Lang.t("Media preview"), s.mediaPreview, new Runnable() {
      @Override
      public void run() {
        s.mediaPreview = !s.mediaPreview;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Open binary files"), s.openBinary, new Runnable() {
      @Override
      public void run() {
        s.openBinary = !s.openBinary;
      }
    }));
    rows.addView(actionBtn(Lang.t("Open size limit: ") + sizeLabel(s), Theme.ACCENT, new Runnable() {
      @Override
      public void run() {
        cycleOpenSize();
      }
    }));
    addHeader(Lang.t("Navigation bar"), "Show, hide, rename, reorder chips  |  / is a chip");
    rows.addView(actionBtn(Lang.t("Edit navigation chips (") + countNav(s) + ")", Theme.ACCENT, new Runnable() {
      @Override
      public void run() {
        editNav();
      }
    }));
    rows.addView(actionBtn(Lang.t("Add custom chip"), Theme.GOOD, new Runnable() {
      @Override
      public void run() {
        addNavChip();
      }
    }));
    rows.addView(actionBtn(Lang.t("Find external devices"), Theme.GOOD, new Runnable() {
      @Override
      public void run() {
        findDevices();
      }
    }));
    rows.addView(actionBtn(Lang.t("Get USB IDs") + " (demo)", Theme.GOOD, new Runnable() {
      @Override
      public void run() {
        showUsbIds();
      }
    }));
    addHeader(Lang.t("Tabs"), "Saved in tabs.txt  |  reorder and edit paths");
    rows.addView(actionBtn(Lang.t("Edit tabs (") + countTabs() + ")", Theme.ACCENT, new Runnable() {
      @Override
      public void run() {
        editTabs();
      }
    }));
    rows.addView(actionBtn(Lang.t("Add tab"), Theme.GOOD, new Runnable() {
      @Override
      public void run() {
        addTabItem();
      }
    }));
    addHeader(Lang.t("Notes"), AppSettings.notesFile(this).getAbsolutePath());
    rows.addView(actionBtn(Lang.t("Edit notes"), Theme.GOOD, new Runnable() {
      @Override
      public void run() {
        openNotes();
      }
    }));
    addHeader(Lang.t("File types"), "Default extensions and custom SVG icons");
    addTypeBtn("Image", FileGlyph.IMG);
    addTypeBtn("Audio", FileGlyph.AUDIO);
    addTypeBtn("Video", FileGlyph.VIDEO);
    addTypeBtn("Archive", FileGlyph.ARCH);
    addTypeBtn("APK", FileGlyph.APK);
    addTypeBtn("Document", FileGlyph.PDF);
    addTypeBtn("Code", FileGlyph.CODE);
    addTypeBtn("Text", FileGlyph.TEXT);
    rows.addView(actionBtn(Lang.t("Load SVG icon for extension"), Theme.GOOD, new Runnable() {
      @Override
      public void run() {
        loadSvgIcon();
      }
    }));
    addHeader(Lang.t("HTML"), "Local HTML viewer (app files only)");
    rows.addView(toggleBtn(Lang.t("JavaScript"), s.htmlJs, new Runnable() {
      @Override
      public void run() {
        s.htmlJs = !s.htmlJs;
      }
    }));
    rows.addView(toggleBtn(Lang.t("DOM storage"), s.htmlDom, new Runnable() {
      @Override
      public void run() {
        s.htmlDom = !s.htmlDom;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Zoom"), s.htmlZoom, new Runnable() {
      @Override
      public void run() {
        s.htmlZoom = !s.htmlZoom;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Load images"), s.htmlImages, new Runnable() {
      @Override
      public void run() {
        s.htmlImages = !s.htmlImages;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Control bar"), s.htmlShowBar, new Runnable() {
      @Override
      public void run() {
        s.htmlShowBar = !s.htmlShowBar;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Start with toolbar hidden"), s.htmlHideBar, new Runnable() {
      @Override
      public void run() {
        s.htmlHideBar = !s.htmlHideBar;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Allow file access"), s.htmlFileAccess, new Runnable() {
      @Override
      public void run() {
        s.htmlFileAccess = !s.htmlFileAccess;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Page cache"), s.htmlCache, new Runnable() {
      @Override
      public void run() {
        s.htmlCache = !s.htmlCache;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Wide viewport"), s.htmlWide, new Runnable() {
      @Override
      public void run() {
        s.htmlWide = !s.htmlWide;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Overview mode"), s.htmlOverview, new Runnable() {
      @Override
      public void run() {
        s.htmlOverview = !s.htmlOverview;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Copy local assets"), s.htmlCopyAssets, new Runnable() {
      @Override
      public void run() {
        s.htmlCopyAssets = !s.htmlCopyAssets;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Clear viewer cache on open"), s.htmlClearOnOpen, new Runnable() {
      @Override
      public void run() {
        s.htmlClearOnOpen = !s.htmlClearOnOpen;
      }
    }));
    rows.addView(actionBtn(Lang.t("User-Agent: ") + AppSettings.HTML_UA_NAMES[s.htmlUaMode]
        + (s.htmlUaMode == 3 && s.htmlUaCustom.length() > 0 ? "  [" + s.htmlUaCustom + "]" : ""),
      Theme.ACCENT, new Runnable() {
        @Override
        public void run() {
          pickUa();
        }
      }));
    rows.addView(cycleBtn(Lang.t("HTML screen: ") + AppSettings.HTML_SCREEN_NAMES[s.htmlScreenIdx], new Runnable() {
      @Override
      public void run() {
        s.htmlScreenIdx = (s.htmlScreenIdx + 1) % AppSettings.HTML_SCREEN_NAMES.length;
      }
    }));
    rows.addView(toggleBtn(Lang.t("HTML hardware accel"), s.htmlHwAccel, new Runnable() {
      @Override
      public void run() {
        s.htmlHwAccel = !s.htmlHwAccel;
      }
    }));
    addHeader(Lang.t("Editor"), "Text viewer");
    rows.addView(toggleBtn(Lang.t("Word wrap"), s.editorWordWrap, new Runnable() {
      @Override
      public void run() {
        s.editorWordWrap = !s.editorWordWrap;
      }
    }));
    rows.addView(cycleBtn(Lang.t("Font size: ") + AppSettings.FONT_NAMES[s.editorFont], new Runnable() {
      @Override
      public void run() {
        s.editorFont = (s.editorFont + 1) % AppSettings.FONT_NAMES.length;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Line numbers"), s.editorLineNo, new Runnable() {
      @Override
      public void run() {
        s.editorLineNo = !s.editorLineNo;
      }
    }));
    rows.addView(cycleBtn(Lang.t("Tab size: ") + AppSettings.TAB_SIZES[s.editorTabIdx], new Runnable() {
      @Override
      public void run() {
        s.editorTabIdx = (s.editorTabIdx + 1) % AppSettings.TAB_SIZES.length;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Spaces only (no tab chars)"), s.editorSpaces, new Runnable() {
      @Override
      public void run() {
        s.editorSpaces = !s.editorSpaces;
      }
    }));
    addHeader(Lang.t("Safety"), null);
    rows.addView(toggleBtn(Lang.t("Confirm move to trash"), s.confirmDelete, new Runnable() {
      @Override
      public void run() {
        s.confirmDelete = !s.confirmDelete;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Confirm permanent delete"), s.confirmPermanent, new Runnable() {
      @Override
      public void run() {
        s.confirmPermanent = !s.confirmPermanent;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Confirm overwrite"), s.confirmOverwrite, new Runnable() {
      @Override
      public void run() {
        s.confirmOverwrite = !s.confirmOverwrite;
      }
    }));
    rows.addView(cycleBtn(Lang.t("Trash keep: ") + s.trashDays() + " days", new Runnable() {
      @Override
      public void run() {
        s.trashDayIdx = (s.trashDayIdx + 1) % AppSettings.TRASH_DAYS.length;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Protect app folders"), s.protectOn, new Runnable() {
      @Override
      public void run() {
        if (s.protectOn && s.hasLockPass()) {
          s.protectOn = true;
          Ui.askPass(SettingsActivity.this, new Runnable() {
            @Override
            public void run() {
              AppSettings.get().protectOn = false;
              changed = true;
              saveAndRebuild();
            }
          });
          return;
        }
        s.protectOn = !s.protectOn;
      }
    }));
    rows.addView(actionBtn(Lang.t("Protected paths (") + s.protectPaths().length + ")", Theme.ACCENT, new Runnable() {
      @Override
      public void run() {
        Ui.askPass(SettingsActivity.this, new Runnable() {
          @Override
          public void run() {
            editProtect();
          }
        });
      }
    }));
    rows.addView(actionBtn(s.hasLockPass() ? "Clear lock password" : "Set lock password",
      Theme.ACCENT, new Runnable() {
        @Override
        public void run() {
          editLockPass();
        }
      }));
    addHeader(Lang.t("Optimization"), "RAM, cache, logs, thumbs");
    rows.addView(toggleBtn(Lang.t("Disable system probes"), s.optNoProbe, new Runnable() {
      @Override
      public void run() {
        s.optNoProbe = !s.optNoProbe;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Disable heap / RAM"), s.optNoHeap, new Runnable() {
      @Override
      public void run() {
        s.optNoHeap = !s.optNoHeap;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Compress image display"), s.imgCompress, new Runnable() {
      @Override
      public void run() {
        s.imgCompress = !s.imgCompress;
        Thumbs.clear();
      }
    }));
    rows.addView(toggleBtn(Lang.t("Icon / SVG gradient"), s.iconGradient, new Runnable() {
      @Override
      public void run() {
        s.iconGradient = !s.iconGradient;
        Icons.clear();
      }
    }));
    rows.addView(actionBtn(Lang.t("Clear saved scroll"), Theme.ACCENT, new Runnable() {
      @Override
      public void run() {
        MainActivity.clearScroll();
        Ui.toast(SettingsActivity.this, "Scroll positions cleared");
      }
    }));
    rows.addView(toggleBtn(Lang.t("Disable thumbnails"), s.optNoThumbs, new Runnable() {
      @Override
      public void run() {
        s.optNoThumbs = !s.optNoThumbs;
        if (s.optNoThumbs) {
          Thumbs.clear();
        }
      }
    }));
    rows.addView(cycleBtn(Lang.t("Preview size: ") + AppSettings.THUMB_NAMES[s.thumbSize], new Runnable() {
      @Override
      public void run() {
        s.thumbSize = (s.thumbSize + 1) % AppSettings.THUMB_NAMES.length;
        Thumbs.clear();
      }
    }));
    rows.addView(toggleBtn(Lang.t("Disable APK folder icons"), s.optNoPkgIcons, new Runnable() {
      @Override
      public void run() {
        s.optNoPkgIcons = !s.optNoPkgIcons;
        if (s.optNoPkgIcons) {
          PkgIcons.clear();
        }
      }
    }));
    rows.addView(toggleBtn(Lang.t("Disable archive browse"), s.optNoArchiveBrowse, new Runnable() {
      @Override
      public void run() {
        s.optNoArchiveBrowse = !s.optNoArchiveBrowse;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Disable HTML viewer"), s.optNoHtml, new Runnable() {
      @Override
      public void run() {
        s.optNoHtml = !s.optNoHtml;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Disable logging"), s.optNoLog, new Runnable() {
      @Override
      public void run() {
        s.optNoLog = !s.optNoLog;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Disable Info SHA256"), s.optNoSha256, new Runnable() {
      @Override
      public void run() {
        s.optNoSha256 = !s.optNoSha256;
      }
    }));
    rows.addView(toggleBtn(Lang.t("Disable rounded corners"), s.optNoRound, new Runnable() {
      @Override
      public void run() {
        s.optNoRound = !s.optNoRound;
        themeChanged = true;
      }
    }));
    rows.addView(actionBtn(Lang.t("Clear Cache"), Theme.ACCENT, new Runnable() {
      @Override
      public void run() {
        clearAppCache();
      }
    }));
    rows.addView(actionBtn(Lang.t("Clear Entry Folders"), Theme.ACCENT, new Runnable() {
      @Override
      public void run() {
        clearEntryFolders();
      }
    }));
    rows.addView(actionBtn(Lang.t("Clear crash/error logs"), Theme.ACCENT, new Runnable() {
      @Override
      public void run() {
        OpLog.clearCrashLogs(SettingsActivity.this);
        Ui.toast(SettingsActivity.this, "Crash and error logs cleared");
      }
    }));
    addHeader(Lang.t("SVG"), "Viewer render of .svg files");
    rows.addView(cycleBtn(Lang.t("SVG resolution: ") + AppSettings.SVG_RES_NAMES[s.svgResIdx], new Runnable() {
      @Override
      public void run() {
        s.svgResIdx = (s.svgResIdx + 1) % AppSettings.SVG_RES_NAMES.length;
      }
    }));
    rows.addView(toggleBtn(Lang.t("SVG gradients"), s.svgGrads, new Runnable() {
      @Override
      public void run() {
        s.svgGrads = !s.svgGrads;
      }
    }));
    rows.addView(toggleBtn(Lang.t("SVG 16-bit color"), s.svgRgb565, new Runnable() {
      @Override
      public void run() {
        s.svgRgb565 = !s.svgRgb565;
      }
    }));
    rows.addView(cycleBtn(Lang.t("SVG background: ") + AppSettings.SVG_BG_NAMES[s.svgBgIdx], new Runnable() {
      @Override
      public void run() {
        s.svgBgIdx = (s.svgBgIdx + 1) % AppSettings.SVG_BG_NAMES.length;
      }
    }));
    addHeader(Lang.t("Backup"), AppSettings.publicDir().getAbsolutePath());
    rows.addView(actionBtn(Lang.t("Backup: settings, tabs, bookmarks, notes"), Theme.ACCENT, new Runnable() {
      @Override
      public void run() {
        backupMenu();
      }
    }));
    rows.addView(actionBtn(Lang.t("Reset to defaults"), Theme.DANGER, new Runnable() {
      @Override
      public void run() {
        Ui.confirm(SettingsActivity.this, "Reset all settings to defaults?", new Runnable() {
          @Override
          public void run() {
            AppSettings.get().reset();
            Theme.applyAccent(AppSettings.get().accentColor, AppSettings.get().darkTheme);
            changed = true;
            themeChanged = true;
            saveAndRebuild();
          }
        });
      }
    }));
    rows.addView(actionBtn(Lang.t("Back"), Theme.TEXT, new Runnable() {
      @Override
      public void run() {
        onBackPressed();
      }
    }));
  }
  private int[] accentSet(boolean lightVariant) {
    int off = lightVariant ? 2 : 0;
    int[] out = new int[AppSettings.ACCENT_NAMES.length];
    int i;
    for (i = 0; i < Theme.ACCENTS.length; i++) {
      out[i] = Theme.ACCENTS[i][off];
    }
    AppSettings s = AppSettings.get();
    out[out.length - 1] = s.customAccent == 0 ? 0xFF2E7BC7 : s.customAccent;
    return out;
  }
  private void pickColor(final boolean folder) {
    final AppSettings s = AppSettings.get();
    String title = Lang.t(folder ? "Folder color" : "Accent color");
    Ui.menu(this, title, AppSettings.ACCENT_NAMES, accentSet(!s.darkTheme), new Ui.OnPick() {
      @Override
      public void onPick(int w) {
        if (folder) {
          s.folderColor = w;
        } else {
          s.accentColor = w;
        }
        if (w == Theme.ACCENTS.length) {
          pickCustom(folder);
          return;
        }
        if (folder) {
          Icons.clear();
        } else {
          Theme.applyAccent(w, s.darkTheme);
        }
        changed = true;
        saveAndRebuild();
      }
    });
  }
  private void addChromeRow(LinearLayout rows, String label, final boolean light, final int idx) {
    AppSettings s = AppSettings.get();
    int[] slot = light ? s.chromeL : s.chromeD;
    int cur = (idx >= 0 && idx < slot.length) ? slot[idx] : 0;
    rows.addView(actionBtn(label + ": " + Theme.toHex(cur), Theme.ACCENT, new Runnable() {
      @Override
      public void run() {
        pickChrome(light, idx);
      }
    }));
  }
  private void pickCustom(final boolean folder) {
    final AppSettings s = AppSettings.get();
    String cur = Theme.toHex(folder ? s.customFolder : s.customAccent);
    Ui.input(this, Lang.t("Custom") + " RRGGBB", cur, new Ui.OnText() {
      @Override
      public void on(String text) {
        int c = Theme.parseHex(text, folder ? s.customFolder : s.customAccent);
        if (folder) {
          s.customFolder = c;
          s.folderColor = Theme.ACCENTS.length;
          Icons.clear();
        } else {
          s.customAccent = c;
          s.accentColor = Theme.ACCENTS.length;
          Theme.applyAccent(s.accentColor, s.darkTheme);
        }
        changed = true;
        saveAndRebuild();
      }
    });
  }
  private void pickChrome(final boolean light, final int idx) {
    final AppSettings s = AppSettings.get();
    final int[] slot = light ? s.chromeL : s.chromeD;
    if (idx < 0 || idx >= slot.length) {
      return;
    }
    final int cur = slot[idx];
    Ui.input(this, Lang.t("Custom") + " RRGGBB", Theme.toHex(cur), new Ui.OnText() {
      @Override
      public void on(String text) {
        int c = Theme.parseHex(text, cur);
        if (c != 0) {
          c = 0xFF000000 | (c & 0xFFFFFF);
        }
        slot[idx] = c;
        s.useCustomChrome = true;
        Theme.applyAccent(s.accentColor, s.darkTheme);
        themeChanged = true;
        changed = true;
        saveAndRebuild();
      }
    });
  }
  private void clearAppCache() {
    final File cacheDir = getCacheDir();
    final File ext = getExternalCacheDir();
    Ui.bg(this, "Clearing cache...", new Runnable() {
      @Override
      public void run() {
        long bytes = FileOps.sizeOf(cacheDir);
        FileOps.deleteRec(cacheDir);
        cacheDir.mkdirs();
        if (ext != null) {
          bytes += FileOps.sizeOf(ext);
          FileOps.deleteRec(ext);
          ext.mkdirs();
        }
        final long freed = bytes;
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            if (isFinishing()) {
              return;
            }
            Thumbs.clear();
            Icons.clear();
            Ui.toast(SettingsActivity.this, "Cache cleared: " + FileOps.humanSize(freed));
          }
        });
      }
    }, null);
  }
  private void clearEntryFolders() {
    File files = getFilesDir();
    int n = FileOps.deleteEmptyDirs(files, TrashStore.trashDir(files));
    Ui.toast(this, "Empty folders removed: " + n);
  }
  private int countNav(AppSettings s) {
    int n = 0;
    java.util.List<AppSettings.NavItem> items = s.navItems();
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).on) {
        n++;
      }
    }
    return n;
  }
  private void addTypeBtn(final String title, final int kind) {
    AppSettings s = AppSettings.get();
    String csv = s.extCsv(kind);
    if (csv.length() == 0) {
      csv = FileGlyph.defaultExts(kind);
    }
    boolean on = s.extGroupOn(kind);
    Button b = actionBtn(title + (on ? ":  " + csv : ":  off"),
      on ? Theme.ACCENT : Theme.TEXT_DIM, new Runnable() {
        @Override
        public void run() {
          if (!AppSettings.get().extGroupOn(kind)) {
            Ui.toast(SettingsActivity.this, "off");
            return;
          }
          editType(kind, title);
        }
      });
    b.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        AppSettings.get().toggleExtGroup(kind);
        changed = true;
        saveAndRebuild();
        Ui.toast(SettingsActivity.this, AppSettings.get().extGroupOn(kind) ? "ON" : "off");
        return true;
      }
    });
    rows.addView(b);
  }
  private void editType(final int kind, String title) {
    AppSettings s = AppSettings.get();
    String cur = s.extCsv(kind);
    if (cur.length() == 0) {
      cur = FileGlyph.defaultExts(kind);
    }
    Ui.input(this, title + " extensions (comma separated):", cur, new Ui.OnText() {
      @Override
      public void on(String t) {
        AppSettings.get().setExtCsv(kind, t);
        changed = true;
        saveAndRebuild();
      }
    });
  }
  private void loadSvgIcon() {
    FilePick.show(this, Environment.getExternalStorageDirectory(), "Pick SVG icon", "svg", new FilePick.OnFile() {
      @Override
      public void on(final File file) {
        Ui.input(SettingsActivity.this, "Extension for this icon (e.g. dat):", "", new Ui.OnText() {
          @Override
          public void on(String t) {
            String ext = AppSettings.normExts(t);
            if (ext.length() == 0) {
              Ui.toast(SettingsActivity.this, "Need an extension");
              return;
            }
            boolean ok = ExtIcons.save(SettingsActivity.this, ext, file);
            ExtIcons.clear();
            Icons.clear();
            Ui.toast(SettingsActivity.this, ok ? "SVG icon for ." + ext : "SVG load failed");
          }
        });
      }
    });
  }
  private void editNav() {
    final java.util.List<AppSettings.NavItem> items = AppSettings.get().navItems();
    String[] names = new String[items.size()];
    int[] colors = new int[items.size()];
    for (int i = 0; i < items.size(); i++) {
      AppSettings.NavItem n = items.get(i);
      names[i] = (n.on ? "[on]  " : "[off] ") + n.label + "  " + n.path;
      colors[i] = n.on ? Theme.GOOD : Theme.TEXT_DIM;
    }
    Ui.menu(this, "Navigation chips", names, colors, new Ui.OnPick() {
      @Override
      public void onPick(int w) {
        navChipMenu(items, w);
      }
    });
  }
  private void navChipMenu(final java.util.List<AppSettings.NavItem> items, final int idx) {
    if (idx < 0 || idx >= items.size()) {
      return;
    }
    Ui.menu(this, items.get(idx).label,
      new String[]{"Toggle", "Edit name and path", "Move up", "Move down", "Delete"},
      new int[]{Theme.ACCENT, Theme.ACCENT, Theme.TEXT, Theme.TEXT, Theme.DANGER},
      new Ui.OnPick() {
        @Override
        public void onPick(int w) {
          AppSettings.NavItem n = items.get(idx);
          if (w == 0) {
            n.on = !n.on;
          } else if (w == 1) {
            editNavItem(items, idx);
            return;
          } else if (w == 2 && idx > 0) {
            items.remove(idx);
            items.add(idx - 1, n);
          } else if (w == 3 && idx < items.size() - 1) {
            items.remove(idx);
            items.add(idx + 1, n);
          } else if (w == 4) {
            items.remove(idx);
          }
          AppSettings.get().setNavItems(items);
          changed = true;
          saveAndRebuild();
        }
      });
  }
  private void editNavItem(final java.util.List<AppSettings.NavItem> items, final int idx) {
    final AppSettings.NavItem n = items.get(idx);
    Ui.input(this, "Chip name:", n.label, new Ui.OnText() {
      @Override
      public void on(String name) {
        if (name.length() == 0) {
          name = n.label;
        }
        final String nm = name;
        Ui.input(SettingsActivity.this, "Path ($SD $ANDROID $APP /data /):", n.path, new Ui.OnText() {
          @Override
          public void on(String path) {
            if (path.length() == 0) {
              path = n.path;
            }
            n.label = nm;
            n.path = path;
            AppSettings.get().setNavItems(items);
            changed = true;
            saveAndRebuild();
          }
        });
      }
    });
  }
  private void addNavChip() {
    Ui.input(this, "Chip name:", "", new Ui.OnText() {
      @Override
      public void on(String name) {
        if (name.length() == 0) {
          Ui.toast(SettingsActivity.this, "Need a name");
          return;
        }
        final String nm = name;
        Ui.input(SettingsActivity.this, "Path:", "", new Ui.OnText() {
          @Override
          public void on(String path) {
            if (path.length() == 0) {
              Ui.toast(SettingsActivity.this, "Need a path");
              return;
            }
            java.util.List<AppSettings.NavItem> items = AppSettings.get().navItems();
            AppSettings.NavItem n = new AppSettings.NavItem();
            n.label = nm;
            n.path = path;
            n.on = true;
            items.add(n);
            AppSettings.get().setNavItems(items);
            changed = true;
            saveAndRebuild();
          }
        });
      }
    });
  }
  private String sizeLabel(AppSettings s) {
    if (s.openSize == AppSettings.OPEN_SIZE_NAMES.length - 1) {
      return "Custom (" + FileOps.humanSize(s.openSizeCustom) + ")";
    }
    return AppSettings.OPEN_SIZE_NAMES[s.openSize];
  }
  private void cycleOpenSize() {
    final AppSettings s = AppSettings.get();
    s.openSize = (s.openSize + 1) % AppSettings.OPEN_SIZE_NAMES.length;
    if (s.openSize == AppSettings.OPEN_SIZE_NAMES.length - 1) {
      Ui.input(this, "Custom open size in KB:",
        String.valueOf(Math.max(1L, s.openSizeCustom / 1024L)), new Ui.OnText() {
          @Override
          public void on(String t) {
            try {
              long kb = Long.parseLong(t.trim());
              if (kb < 1L) {
                kb = 1L;
              }
              s.openSizeCustom = kb * 1024L;
            } catch (Exception e) {
              s.openSizeCustom = 524288L;
            }
            changed = true;
            saveAndRebuild();
          }
        });
      return;
    }
    changed = true;
    saveAndRebuild();
  }
  private void editLockPass() {
    final AppSettings s = AppSettings.get();
    if (s.hasLockPass()) {
      Ui.askPass(this, new Runnable() {
        @Override
        public void run() {
          s.setLockPass("");
          changed = true;
          saveAndRebuild();
          Ui.toast(SettingsActivity.this, "Password cleared");
        }
      });
      return;
    }
    Ui.input(this, "Lock password", "", new Ui.OnText() {
      @Override
      public void on(String t) {
        if (t.length() == 0) {
          Ui.toast(SettingsActivity.this, "Wrong password");
          return;
        }
        s.setLockPass(t);
        changed = true;
        saveAndRebuild();
      }
    });
  }
  private void editProtect() {
    final AppSettings s = AppSettings.get();
    s.materializeProtect();
    final String[] paths = s.protectPaths();
    String[] names = new String[paths.length + 1];
    int[] colors = new int[paths.length + 1];
    for (int i = 0; i < paths.length; i++) {
      names[i] = paths[i];
      colors[i] = Theme.ACCENT;
    }
    names[paths.length] = "Add custom path";
    colors[paths.length] = Theme.GOOD;
    Ui.menu(this, "Protected paths", names, colors, new Ui.OnPick() {
      @Override
      public void onPick(int w) {
        if (w == paths.length) {
          Ui.input(SettingsActivity.this, "Path to protect:", "/", new Ui.OnText() {
            @Override
            public void on(String t) {
              if (t.length() == 0) {
                return;
              }
              s.addProtect(t);
              changed = true;
              saveAndRebuild();
            }
          });
          return;
        }
        final String path = paths[w];
        Ui.menu(SettingsActivity.this, path,
          new String[]{"Edit", "Delete"},
          new int[]{Theme.ACCENT, Theme.DANGER}, new Ui.OnPick() {
            @Override
            public void onPick(int k) {
              if (k == 0) {
                Ui.input(SettingsActivity.this, "Path to protect:", path, new Ui.OnText() {
                  @Override
                  public void on(String t) {
                    if (t.length() == 0) {
                      return;
                    }
                    s.replaceProtect(path, t);
                    changed = true;
                    saveAndRebuild();
                  }
                });
              } else {
                s.removeProtect(path);
                changed = true;
                saveAndRebuild();
              }
            }
          });
      }
    });
  }
  private void findDevices() {
    final File[] found = Probe.externalDevices(this);
    if (found.length == 0) {
      Ui.toast(this, "No external devices");
      return;
    }
    String[] names = new String[found.length];
    int[] colors = new int[found.length];
    for (int i = 0; i < found.length; i++) {
      names[i] = found[i].getName() + "  " + found[i].getAbsolutePath();
      colors[i] = Theme.GOOD;
    }
    Ui.menu(this, "External devices", names, colors, new Ui.OnPick() {
      @Override
      public void onPick(int w) {
        if (w < 0 || w >= found.length) {
          return;
        }
        addDeviceChip(found[w]);
      }
    });
  }
  private void showUsbIds() {
    final File[] found = Probe.usbVolumes(this);
    if (found.length == 0) {
      Ui.toast(this, "No USB/MicroSD volumes");
      return;
    }
    String[] names = new String[found.length];
    int[] colors = new int[found.length];
    for (int i = 0; i < found.length; i++) {
      names[i] = found[i].getName() + "  " + found[i].getAbsolutePath();
      colors[i] = Theme.GOOD;
    }
    Ui.menu(this, "USB / MicroSD IDs", names, colors, new Ui.OnPick() {
      @Override
      public void onPick(int w) {
        if (w < 0 || w >= found.length) {
          return;
        }
        addDeviceChip(found[w]);
      }
    });
  }
  private void addDeviceChip(File dir) {
    java.util.List<AppSettings.NavItem> items = AppSettings.get().navItems();
    String path = dir.getAbsolutePath();
    for (int i = 0; i < items.size(); i++) {
      if (path.equals(items.get(i).path)) {
        items.get(i).on = true;
        AppSettings.get().setNavItems(items);
        changed = true;
        saveAndRebuild();
        Ui.toast(SettingsActivity.this, "Already in navigation");
        return;
      }
    }
    AppSettings.NavItem n = new AppSettings.NavItem();
    n.label = dir.getName().length() == 0 ? "Ext" : dir.getName();
    n.path = path;
    n.on = true;
    items.add(n);
    AppSettings.get().setNavItems(items);
    changed = true;
    saveAndRebuild();
    Ui.toast(this, "Added " + n.label);
  }
  private void addHeader(String title, String sub) {
    TextView t = new TextView(this);
    t.setText(title);
    t.setTextColor(Theme.ACCENT);
    t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
    t.setTextSize(16);
    t.setPadding(Theme.dp(this, 14), Theme.dp(this, 18), Theme.dp(this, 14), Theme.dp(this, 4));
    rows.addView(t);
    if (sub != null) {
      TextView sv = new TextView(this);
      sv.setText(sub);
      sv.setTextColor(Theme.TEXT_DIM);
      sv.setTextSize(10);
      sv.setPadding(Theme.dp(this, 14), 0, Theme.dp(this, 14), Theme.dp(this, 4));
      rows.addView(sv);
    }
  }
  private Button toggleBtn(String label, boolean on, final Runnable flip) {
    Button b = mkBtn(label + ":  " + (on ? "ON" : "off"), on ? Theme.GOOD : Theme.TEXT_DIM);
    b.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        flip.run();
        changed = true;
        saveAndRebuild();
      }
    });
    return b;
  }
  private Button cycleBtn(String label, final Runnable cycle) {
    Button b = mkBtn(label, Theme.ACCENT);
    b.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        cycle.run();
        changed = true;
        saveAndRebuild();
      }
    });
    return b;
  }
  private Button actionBtn(String label, int color, final Runnable act) {
    Button b = mkBtn(label, color);
    b.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        act.run();
      }
    });
    return b;
  }
  private int countTabs() {
    return AppSettings.readTabs(this).size();
  }
  private void editTabs() {
    final java.util.List<AppSettings.TabItem> items = AppSettings.readTabs(this);
    if (items.size() == 0) {
      Ui.toast(this, "No saved tabs");
      return;
    }
    String[] names = new String[items.size()];
    int[] colors = new int[items.size()];
    for (int i = 0; i < items.size(); i++) {
      AppSettings.TabItem t = items.get(i);
      String label = t.name != null && t.name.length() > 0 ? t.name : new File(t.path).getName();
      if (label.length() == 0) {
        label = "/";
      }
      names[i] = label + "  " + t.path;
      colors[i] = Theme.ACCENT;
    }
    Ui.menu(this, "Tabs", names, colors, new Ui.OnPick() {
      @Override
      public void onPick(int w) {
        tabItemMenu(items, w);
      }
    });
  }
  private void tabItemMenu(final java.util.List<AppSettings.TabItem> items, final int idx) {
    if (idx < 0 || idx >= items.size()) {
      return;
    }
    Ui.menu(this, items.get(idx).path,
      new String[]{"Edit name and path", "Move up", "Move down", "Delete"},
      new int[]{Theme.ACCENT, Theme.TEXT, Theme.TEXT, Theme.DANGER},
      new Ui.OnPick() {
        @Override
        public void onPick(int w) {
          AppSettings.TabItem t = items.get(idx);
          if (w == 0) {
            editTabItem(items, idx);
            return;
          } else if (w == 1 && idx > 0) {
            items.remove(idx);
            items.add(idx - 1, t);
          } else if (w == 2 && idx < items.size() - 1) {
            items.remove(idx);
            items.add(idx + 1, t);
          } else if (w == 3) {
            if (items.size() <= 1) {
              Ui.toast(SettingsActivity.this, "Last tab");
              return;
            }
            items.remove(idx);
          }
          AppSettings.writeTabs(SettingsActivity.this, items);
          changed = true;
          saveAndRebuild();
        }
      });
  }
  private void editTabItem(final java.util.List<AppSettings.TabItem> items, final int idx) {
    final AppSettings.TabItem t = items.get(idx);
    String curName = t.name != null ? t.name : "";
    Ui.input(this, "Tab name (empty = folder name):", curName, new Ui.OnText() {
      @Override
      public void on(String name) {
        final String nm = name;
        Ui.input(SettingsActivity.this, "Tab path:", t.path, new Ui.OnText() {
          @Override
          public void on(String path) {
            if (path.length() == 0) {
              path = t.path;
            }
            t.name = nm;
            t.path = path;
            AppSettings.writeTabs(SettingsActivity.this, items);
            changed = true;
            saveAndRebuild();
          }
        });
      }
    });
  }
  private void addTabItem() {
    Ui.input(this, "Tab name (optional):", "", new Ui.OnText() {
      @Override
      public void on(String name) {
        final String nm = name;
        Ui.input(SettingsActivity.this, "Tab path:", "/", new Ui.OnText() {
          @Override
          public void on(String path) {
            if (path.length() == 0) {
              Ui.toast(SettingsActivity.this, "Need a path");
              return;
            }
            java.util.List<AppSettings.TabItem> items = AppSettings.readTabs(SettingsActivity.this);
            AppSettings.TabItem t = new AppSettings.TabItem();
            t.name = nm;
            t.path = path;
            items.add(t);
            AppSettings.writeTabs(SettingsActivity.this, items);
            changed = true;
            saveAndRebuild();
          }
        });
      }
    });
  }
  private void openNotes() {
    File n = AppSettings.notesFile(this);
    try {
      if (!n.exists()) {
        FileOps.writeString(n, "");
      }
    } catch (Exception ignored) {}
    Intent i = new Intent(this, EditorActivity.class);
    i.putExtra("path", n.getAbsolutePath());
    startActivity(i);
  }
  private void saveAndRebuild() {
    changed = true;
    if (themeChanged) {
      themeChanged = false;
      Ui.bar(this);
      Ui.applyKeepScreen(this);
    }
    rebuild();
  }
  private Button mkBtn(String label, int color) {
    Button b = Ui.btn(this, label, color);
    b.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
    int ph = Theme.dp(this, 14);
    b.setPadding(ph, 0, ph, 0);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.setMargins(Theme.dp(this, 10), Theme.dp(this, 3), Theme.dp(this, 10), Theme.dp(this, 3));
    b.setLayoutParams(lp);
    return b;
  }
  private void showHelp() {
    Ui.alert(this, "Help",
      "File Manager help\nBrowse\n  Tap selects a file or folder. Double-tap opens it.\n  Row menu (vertical dots) shows commands for one item.\n  Multi-select: tap several rows, then the dots on a selected row.\n  Pull down at the top of the list to refresh.\n  Tap the path bar to type a path. Long-press the path for Settings.\nSearch\n  Matches names in this folder and subfolders.\n  Enable * glob for wildcards (* and ?).\nTabs and marks\n  + adds a tab. Long-press a tab to rename, change path, move or close.\n  Tabs are saved to tabs.txt.\n  Marks (bookmarks) are chips under the navigation bar.\nArchives\n  Pack to ZIP / TAR / TAR.GZ. Archives go to APP_DATA/.../Archives.\n  Double-tap zip/tar/tar.gz to browse. Open a file inside to edit; Save writes it back.\n  Extract or Copy to... writes files out.\nProtect\n  Settings > Safety. Default trees: app data and APP_DATA.\n  The path list can be edited. SHA-256 lock password guards those settings.\n  Terminal rm/mv/mkdir/touch/cp refuse protected paths.\nHTML\n  Local viewer. Downloads go to APP_DATA/.../DownloadedFiles.\n  Pages can pick a file. Other apps can VIEW/SEND html or text into this app.\nTerminal\n  Built-in shell: cd, cd.., ls, cat, rm, cp, mv, mkdir, help.\nLogs\n  ops.log in cache (optional). ErrorLog_/CrashLog_ timestamps always in APP_DATA.\nHide UI\n  Hide Term, Stats and Log buttons on the action panel.\nUSB\n  Get USB IDs (demo) lists real /storage and /mnt/media_rw volume paths.\n  Tap a volume to add it to the navigation clip.\nFile types\n  Long-press a file-type row in Settings to disable that extension group.",
      true);
  }
  private void backupMenu() {
    Ui.menu(this, "Backup",
      new String[]{"Export all", "Import all"},
      new int[]{Theme.ACCENT, Theme.GOOD}, new Ui.OnPick() {
        @Override
        public void onPick(int w) {
          if (w == 0) {
            doExport();
          } else {
            Ui.confirm(SettingsActivity.this,
              "Replace settings, tabs, bookmarks and notes from public folder?",
              new Runnable() {
                @Override
                public void run() {
                  doImport();
                }
              });
          }
        }
      });
  }
  private void doExport() {
    boolean ok = AppSettings.get().exportToPublic();
    AppSettings.copySidecar(this, true);
    Ui.toast(this, ok
      ? "Exported settings, tabs, bookmarks, notes to " + AppSettings.publicDir().getAbsolutePath()
      : "Export failed");
  }
  private void doImport() {
    boolean ok = AppSettings.get().importFromPublic(SettingsActivity.this);
    if (ok) {
      finishAffinity();
      Process.killProcess(Process.myPid());
      System.exit(0);
      return;
    }
    Ui.toast(this, "Import failed (file missing or bad)");
  }
  private void pickUa() {
    final AppSettings s = AppSettings.get();
    Ui.menu(this, "HTML User-Agent", AppSettings.HTML_UA_NAMES,
      new int[]{Theme.ACCENT, Theme.ACCENT, Theme.ACCENT, Theme.GOOD}, new Ui.OnPick() {
        @Override
        public void onPick(int w) {
          s.htmlUaMode = w;
          changed = true;
          if (w == 3) {
            Ui.input(SettingsActivity.this, "Custom User-Agent", s.htmlUaCustom, new Ui.OnText() {
              @Override
              public void on(String text) {
                s.htmlUaCustom = text;
                saveAndRebuild();
              }
            });
            return;
          }
          saveAndRebuild();
        }
      });
  }
  private void applyKeepScreenOn() {
    rows.setKeepScreenOn(AppSettings.get().keepScreenOn);
    Ui.applyKeepScreen(this);
  }
}
