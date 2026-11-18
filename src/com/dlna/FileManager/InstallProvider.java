package com.dlna.FileManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
public final class InstallProvider extends ContentProvider {
  static final String AUTH = "com.dlna.FileManager.file";
  static Uri uriFor(File f) {
    String path = f.getAbsolutePath();
    Uri.Builder b = new Uri.Builder();
    b.scheme("content");
    b.authority(AUTH);
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    String[] parts = path.split("/", -1);
    for (int i = 0; i < parts.length; i++) {
      b.appendPath(parts[i]);
    }
    return b.build();
  }
  @Override
  public boolean onCreate() {
    return true;
  }
  private static File fileFor(Uri uri) {
    if (uri == null) {
      return new File("");
    }
    String p = uri.getPath();
    if (p == null || p.length() == 0) {
      p = uri.getEncodedPath();
      if (p != null) {
        p = Uri.decode(p);
      }
    }
    if (p == null || p.length() == 0) {
      return new File("");
    }
    if (!p.startsWith("/")) {
      p = "/" + p;
    }
    return new File(p);
  }
  private boolean allowed(File f) {
    android.content.Context c = getContext();
    if (c == null || f == null) {
      return false;
    }
    if (FileOps.under(f, c.getCacheDir()) || FileOps.under(f, c.getFilesDir())) {
      return true;
    }
    java.io.File ext = c.getExternalCacheDir();
    return ext != null && FileOps.under(f, ext);
  }
  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
    File f = fileFor(uri);
    if (!f.isFile() || !allowed(f)) {
      throw new FileNotFoundException(f.getAbsolutePath());
    }
    return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
  }
  @Override
  public Cursor query(Uri uri, String[] proj, String sel, String[] args, String sort) {
    File f = fileFor(uri);
    if (!allowed(f)) {
      return new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME});
    }
    String[] cols = proj != null && proj.length > 0
      ? proj
      : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, "_data"};
    MatrixCursor c = new MatrixCursor(cols);
    Object[] row = new Object[cols.length];
    for (int i = 0; i < cols.length; i++) {
      String col = cols[i];
      if (OpenableColumns.DISPLAY_NAME.equals(col)) {
        row[i] = f.getName();
      } else if (OpenableColumns.SIZE.equals(col)) {
        row[i] = Long.valueOf(f.length());
      } else if ("_data".equals(col)) {
        row[i] = f.getAbsolutePath();
      } else if ("mime_type".equals(col)) {
        row[i] = FileOps.mime(f.getName());
      } else {
        row[i] = null;
      }
    }
    c.addRow(row);
    return c;
  }
  @Override
  public String getType(Uri uri) {
    return FileOps.mime(fileFor(uri).getName());
  }
  @Override
  public Uri insert(Uri uri, ContentValues v) {
    return null;
  }
  @Override
  public int delete(Uri uri, String s, String[] a) {
    return 0;
  }
  @Override
  public int update(Uri uri, ContentValues v, String s, String[] a) {
    return 0;
  }
}
