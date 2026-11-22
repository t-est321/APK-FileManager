package com.dlna.FileManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
final class Probe {
  private static final String[] DATA_ROOT = {
    "/data/app", "/data/data", "/data/system", "/data/user", "/data/media"
  };
  private static final String[] SYSTEM_HINTS = {
    "packages.xml", "packages.list", "packages-stopped.xml", "users", "dropbox",
    "sync", "netstats", "appops.xml", "device_policies.xml",
    "ifw", "procstats", "usagestats", "job", "sensorservice.xml", "vold",
    "entropy.dat", "locksettings.db", "locksettings.db-journal", "users.xml",
    "runtime-permissions.xml", "uris.xml", "notification_policy.xml",
    "deviceidle.xml", "netpolicy.xml", "batterystats.bin", "battery-history",
    "inputmethod", "syncmanager", "graphicsstats", "heapdump", "dropbox",
    "shutdown-checkpoints", "users/0", "users/0.xml", "users/0/settings_system.xml",
    "users/0/settings_secure.xml", "users/0/settings_global.xml",
    "packages.xml.bak", "appops.xml.bak", "installd", "storaged", "uiderrors.txt"
  };
  private static final String[] STORAGE = {
    "/storage/emulated",
    "/storage/self", "/storage/self/primary"
  };
  private static final String[] MNT = {
    "/mnt/sdcard", "/mnt/runtime", "/mnt/media_rw", "/mnt/user", "/mnt/expand"
  };
  private static final String[] SYS = {
    "/sys/block", "/sys/bus", "/sys/class", "/sys/dev", "/sys/devices",
    "/sys/firmware", "/sys/fs", "/sys/kernel", "/sys/module", "/sys/power"
  };
  private static final String[] SYS_CLASS = {
    "backlight", "block", "bdi", "graphics", "hidraw", "input", "leds", "misc",
    "net", "power_supply", "rtc", "tty", "usb_gadget", "android_usb", "thermal",
    "typec", "wakeup", "rfkill", "mem", "dma", "vc", "ieee80211", "binder",
    "android_sched_tune", "udc", "extcon", "switch", "sound", "drm"
  };
  private static final String[] SYS_BLOCK = {
    "loop0", "loop1", "loop2", "loop3", "ram0", "ram1", "zram0", "mmcblk0",
    "mmcblk0p1", "mmcblk0p2", "mmcblk0p3", "sda", "sda1", "sdb", "dm-0", "dm-1",
    "userdata", "metadata", "by-name"
  };
  private static final String[] SYS_BUS = {
    "pci", "usb", "i2c", "spi", "platform", "hid", "mmc", "sdio", "scsi",
    "workqueue", "clocksource", "event_source", "amba", "iio"
  };
  private static final String[] SYS_DEVICES = {
    "system", "virtual", "platform", "soc0", "breakpoint", "tracepoint", "cpu"
  };
  private static final String[] SYS_SYSTEM = {
    "cpu", "memory", "container", "machinecheck"
  };
  private static final String[] SYS_CPU = {
    "cpu0", "cpu1", "cpu2", "cpu3", "cpu4", "cpu5", "cpu6", "cpu7",
    "cpufreq", "cpuidle", "present", "possible", "online", "offline",
    "isolated", "kernel_max", "power", "uevent", "modalias",
    "vulnerabilities", "smt", "hotplug"
  };
  private Probe() {}
  static boolean present(File f) {
    return f != null && f.exists();
  }
  static File[] androidData(Context c) {
    File base = new File(Environment.getExternalStorageDirectory(), "Android/data");
    return byPackages(c, base);
  }
  static File[] dataData(Context c) {
    return byPackages(c, new File("/data/data"));
  }
  static File[] dataUser0(Context c) {
    return byPackages(c, new File("/data/user/0"));
  }
  static File[] dataRoot() {
    return existing(DATA_ROOT);
  }
  static File[] dataSystem() {
    File base = new File("/data/system");
    File[] listed = base.listFiles();
    File[] have = existingUnder(base, SYSTEM_HINTS);
    File uid = new File(base, "uiderrors.txt");
    File[] extra = new File[]{uid};
    if (listed != null && listed.length > 0) {
      return FileOps.merge(FileOps.merge(listed, have), extra);
    }
    return FileOps.merge(have, extra);
  }
  static File[] dataApp(Context c) {
    List<File> out = new ArrayList<File>();
    HashSet<String> seen = new HashSet<String>();
    try {
      List<ApplicationInfo> apps = c.getPackageManager().getInstalledApplications(0);
      for (int i = 0; i < apps.size(); i++) {
        ApplicationInfo ai = apps.get(i);
        addAppPath(out, seen, ai.sourceDir);
        addAppPath(out, seen, ai.publicSourceDir);
      }
      Collections.sort(out, byName());
    } catch (Throwable ignored) {}
    return out.toArray(new File[out.size()]);
  }
  private static void addAppPath(List<File> out, HashSet<String> seen, String src) {
    if (src == null || src.length() == 0) {
      return;
    }
    File f = new File(src);
    File dir = f.getParentFile();
    File pick = dir != null && dir.getAbsolutePath().startsWith("/data/app") ? dir : f;
    if (present(pick) && seen.add(pick.getAbsolutePath())) {
      out.add(pick);
    }
  }
  private static File[] byPackages(Context c, File base) {
    List<File> out = new ArrayList<File>();
    try {
      List<PackageInfo> pkgs = c.getPackageManager().getInstalledPackages(0);
      for (int i = 0; i < pkgs.size(); i++) {
        File f = new File(base, pkgs.get(i).packageName);
        if (present(f)) {
          out.add(f);
        }
      }
      Collections.sort(out, byName());
    } catch (Throwable ignored) {}
    return out.toArray(new File[out.size()]);
  }
  private static File[] existing(String[] paths) {
    List<File> out = new ArrayList<File>();
    for (int i = 0; i < paths.length; i++) {
      File f = new File(paths[i]);
      if (present(f)) {
        out.add(f);
      }
    }
    return out.toArray(new File[out.size()]);
  }
  private static File[] existingUnder(File base, String[] names) {
    List<File> out = new ArrayList<File>();
    if (!present(base)) {
      return out.toArray(new File[0]);
    }
    for (int i = 0; i < names.length; i++) {
      File f = new File(base, names[i]);
      if (present(f)) {
        out.add(f);
      }
    }
    return out.toArray(new File[out.size()]);
  }
  private static File[] sysKids(String dirPath) {
    File dir = new File(dirPath);
    File[] listed = dir.listFiles();
    String[] names = null;
    if ("/sys/class".equals(dirPath)) {
      names = SYS_CLASS;
    } else if ("/sys/block".equals(dirPath)) {
      names = SYS_BLOCK;
    } else if ("/sys/bus".equals(dirPath)) {
      names = SYS_BUS;
    } else if ("/sys/devices".equals(dirPath) || "/sys/device".equals(dirPath)) {
      names = SYS_DEVICES;
    } else if ("/sys/devices/system".equals(dirPath) || "/sys/device/system".equals(dirPath)) {
      names = SYS_SYSTEM;
    } else if ("/sys/devices/system/cpu".equals(dirPath) || "/sys/devices/cpu".equals(dirPath)
        || "/sys/device/system/cpu".equals(dirPath) || "/sys/device/cpu".equals(dirPath)) {
      names = SYS_CPU;
    }
    File[] extra = names == null ? new File[0] : existingUnder(dir, names);
    if (listed != null && listed.length > 0) {
      return FileOps.merge(listed, extra);
    }
    return extra;
  }
  private static Comparator<File> byName() {
    return new Comparator<File>() {
      @Override
      public int compare(File a, File b) {
        return a.getName().compareToIgnoreCase(b.getName());
      }
    };
  }
  static boolean isAndroidDataDir(File dir) {
    try {
      File ext = Environment.getExternalStorageDirectory();
      return dir.getAbsolutePath().equals(new File(ext, "Android/data").getAbsolutePath());
    } catch (Exception e) {
      return false;
    }
  }
  static File[] externalDevices(Context c) {
    ArrayList<File> out = new ArrayList<File>();
    HashSet<String> seen = new HashSet<String>();
    addVol(out, seen, Environment.getExternalStorageDirectory());
    addKids(out, seen, new File("/storage"), new String[]{"emulated", "self", "0"});
    addKids(out, seen, new File("/mnt/media_rw"), null);
    String[] extra = {
      "/mnt/usb", "/mnt/sdcard", "/mnt/extSdCard", "/mnt/external_sd",
      "/mnt/expand", "/storage/sdcard1", "/storage/usbdisk", "/storage/usbotg"
    };
    for (int i = 0; i < extra.length; i++) {
      addVol(out, seen, new File(extra[i]));
    }
    if (android.os.Build.VERSION.SDK_INT >= 19) {
      try {
        File[] dirs = c.getExternalFilesDirs(null);
        if (dirs != null) {
          for (int i = 0; i < dirs.length; i++) {
            File root = volumeRoot(dirs[i]);
            addVol(out, seen, root);
          }
        }
      } catch (Throwable ignored) {}
    }
    addStorageManager(c, out, seen);
    addEnv(out, seen);
    return out.toArray(new File[out.size()]);
  }
  private static File volumeRoot(File f) {
    if (f == null) {
      return null;
    }
    File cur = f;
    while (cur.getParentFile() != null) {
      String p = cur.getAbsolutePath();
      File parent = cur.getParentFile();
      String pp = parent.getAbsolutePath();
      if ("/storage".equals(pp) || "/mnt".equals(pp) || "/mnt/media_rw".equals(pp)) {
        return cur;
      }
      if ("/".equals(p) || "/".equals(pp)) {
        break;
      }
      cur = parent;
    }
    return f;
  }
  private static void addKids(List<File> out, HashSet<String> seen, File dir, String[] skip) {
    File[] kids = dir.listFiles();
    if (kids == null) {
      return;
    }
    for (int i = 0; i < kids.length; i++) {
      File k = kids[i];
      String name = k.getName();
      if (skip != null) {
        boolean drop = false;
        for (int j = 0; j < skip.length; j++) {
          if (skip[j].equals(name)) {
            drop = true;
            break;
          }
        }
        if (drop) {
          continue;
        }
      }
      addVol(out, seen, k);
    }
  }
  private static void addVol(List<File> out, HashSet<String> seen, File f) {
    if (!present(f)) {
      return;
    }
    String p = f.getAbsolutePath();
    if (seen.add(p)) {
      out.add(f);
    }
  }
  static File[] usbVolumes(Context c) {
    ArrayList<File> out = new ArrayList<File>();
    HashSet<String> seen = new HashSet<String>();
    if (c != null && Build.VERSION.SDK_INT >= 24) {
      try {
        StorageManager sm = (StorageManager) c.getSystemService(Context.STORAGE_SERVICE);
        if (sm != null) {
          List vols = sm.getStorageVolumes();
          int i;
          for (i = 0; i < vols.size(); i++) {
            StorageVolume vol = (StorageVolume) vols.get(i);
            if (vol == null || vol.isPrimary()) {
              continue;
            }
            String st = vol.getState();
            if (!Environment.MEDIA_MOUNTED.equals(st)
                && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(st)) {
              continue;
            }
            File dir = volumeFile(vol);
            if (dir == null) {
              continue;
            }
            String p = dir.getAbsolutePath();
            if (p.indexOf("/emulated") >= 0 || p.endsWith("/self")) {
              continue;
            }
            if (seen.add(p)) {
              out.add(dir);
            }
          }
        }
      } catch (Throwable ignored) {}
    }
    if (c != null && Build.VERSION.SDK_INT >= 19) {
      try {
        File[] dirs = c.getExternalFilesDirs(null);
        if (dirs != null) {
          int i;
          for (i = 0; i < dirs.length; i++) {
            File root = volumeRoot(dirs[i]);
            if (root == null) {
              continue;
            }
            String p = root.getAbsolutePath();
            if (p.indexOf("/emulated") >= 0 || p.endsWith("/self") || "/storage/0".equals(p)) {
              continue;
            }
            if (seen.add(p)) {
              out.add(root);
            }
          }
        }
      } catch (Throwable ignored) {}
    }
    if (out.size() == 0 && c != null && Build.VERSION.SDK_INT < 24) {
      addStorageManager(c, out, seen);
    }
    return out.toArray(new File[out.size()]);
  }
  private static File volumeFile(StorageVolume vol) {
    try {
      Object d = vol.getClass().getMethod("getDirectory").invoke(vol);
      if (d instanceof File) {
        return (File) d;
      }
    } catch (Throwable ignored) {}
    try {
      Object p = vol.getClass().getMethod("getPath").invoke(vol);
      if (p instanceof String && ((String) p).length() > 0) {
        return new File((String) p);
      }
    } catch (Throwable ignored) {}
    return null;
  }
  private static void addEnv(List<File> out, HashSet<String> seen) {
    String[] keys = new String[]{"SECONDARY_STORAGE", "EXTERNAL_SDCARD_STORAGE", "EXTERNAL_SD_STORAGE"};
    int k;
    for (k = 0; k < keys.length; k++) {
      String v = System.getenv(keys[k]);
      if (v == null || v.length() == 0) {
        continue;
      }
      String[] parts = v.split(":");
      int i;
      for (i = 0; i < parts.length; i++) {
        if (parts[i].length() > 0) {
          addVol(out, seen, new File(parts[i]));
        }
      }
    }
  }
  private static void addStorageManager(Context c, List<File> out, HashSet<String> seen) {
    try {
      Object sm = c.getSystemService("storage");
      if (sm == null) {
        return;
      }
      Object list = sm.getClass().getMethod("getVolumeList").invoke(sm);
      if (list == null) {
        return;
      }
      int n = java.lang.reflect.Array.getLength(list);
      int i;
      for (i = 0; i < n; i++) {
        Object vol = java.lang.reflect.Array.get(list, i);
        if (vol == null) {
          continue;
        }
        String path = volumePath(vol);
        if (path == null || path.length() == 0) {
          continue;
        }
        addVol(out, seen, new File(path));
      }
    } catch (Throwable ignored) {}
  }
  private static String volumePath(Object vol) {
    try {
      Object p = vol.getClass().getMethod("getPath").invoke(vol);
      if (p instanceof String) {
        return (String) p;
      }
    } catch (Throwable ignored) {}
    try {
      Object d = vol.getClass().getMethod("getDirectory").invoke(vol);
      if (d instanceof File) {
        return ((File) d).getAbsolutePath();
      }
    } catch (Throwable ignored) {}
    return null;
  }
  static boolean isSystemPath(String p) {
    if (p == null) {
      return false;
    }
    if (p.equals("/data") || p.startsWith("/data/")) {
      return true;
    }
    return p.equals("/sys") || p.startsWith("/sys/");
  }
  static File[] forPath(Context c, String dirPath) {
    if ("/data".equals(dirPath)) {
      return dataRoot();
    }
    if ("/data/data".equals(dirPath)) {
      return dataData(c);
    }
    if ("/data/app".equals(dirPath)) {
      return dataApp(c);
    }
    if ("/data/system".equals(dirPath)) {
      return dataSystem();
    }
    if ("/data/user".equals(dirPath)) {
      return existing(new String[]{"/data/user/0", "/data/user/10", "/data/user/11"});
    }
    if ("/data/user/0".equals(dirPath)) {
      return dataUser0(c);
    }
    if ("/storage".equals(dirPath)) {
      return existing(STORAGE);
    }
    if ("/storage/emulated".equals(dirPath)) {
      return existing(new String[]{"/storage/emulated/0", "/storage/emulated/legacy"});
    }
    if ("/mnt".equals(dirPath)) {
      return existing(MNT);
    }
    if ("/sys".equals(dirPath)) {
      return existing(SYS);
    }
    if (dirPath.startsWith("/sys/")) {
      return sysKids(dirPath);
    }
    if (isAndroidDataDir(new File(dirPath))) {
      return androidData(c);
    }
    return new File[0];
  }
}
