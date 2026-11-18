package com.dlna.FileManager;
import java.io.File;
final class FileGlyph {
  static final int UP = 0;
  static final int DIR = 1;
  static final int FILE = 2;
  static final int IMG = 3;
  static final int AUDIO = 4;
  static final int VIDEO = 5;
  static final int ARCH = 6;
  static final int TEXT = 7;
  static final int CODE = 8;
  static final int APK = 9;
  static final int PDF = 10;
  static final int LINK = 11;
  private FileGlyph() {}
  static String defaultExts(int kind) {
    switch (kind) {
      case IMG: return "png,jpg,jpeg,gif,webp,bmp,heic,svg,ico";
      case AUDIO: return "mp3,wav,ogg,flac,m4a,aac,opus,amr,mid";
      case VIDEO: return "mp4,mkv,avi,webm,3gp,mov,flv,ts,m2ts,mpg,mpeg,wmv";
      case ARCH: return "zip,rar,7z,tar,gz,tgz,bz2,xz,jar,dat";
      case APK: return "apk,xapk";
      case PDF: return "pdf,doc,docx";
      case CODE: return "java,kt,py,c,cc,cpp,h,hpp,js,css,html,htm,xml,json,sh,rb,go,rs,lua,gradle";
      case TEXT: return "txt,md,log,ini,cfg,conf,properties,csv,tsv,srt,vtt,yml,yaml";
      default: return "";
    }
  }
  static int of(File f) {
    return f.isDirectory() ? DIR : forName(f.getName());
  }
  static int forName(String name) {
    String e = FileOps.ext(name);
    if (e.length() == 0) {
      return FILE;
    }
    int mapped = AppSettings.get().mapExt(e);
    if (mapped >= 0 && AppSettings.get().extGroupOn(mapped)) {
      return mapped;
    }
    int b = builtin(e);
    if (b != FILE && !AppSettings.get().extGroupOn(b)) {
      return FILE;
    }
    return b;
  }
  static int builtin(String e) {
    String tag = "|" + e + "|";
    if ("|png|jpg|jpeg|gif|webp|bmp|heic|svg|ico|".contains(tag)) {
      return IMG;
    }
    if ("|mp3|wav|ogg|flac|m4a|aac|opus|amr|mid|".contains(tag)) {
      return AUDIO;
    }
    if ("|mp4|mkv|avi|webm|3gp|mov|flv|ts|m2ts|mpg|mpeg|wmv|".contains(tag)) {
      return VIDEO;
    }
    if ("|zip|rar|7z|tar|gz|tgz|bz2|xz|jar|dat|".contains(tag)) {
      return ARCH;
    }
    if ("|apk|xapk|".contains(tag)) {
      return APK;
    }
    if ("|pdf|doc|docx|".contains(tag)) {
      return PDF;
    }
    if ("|java|kt|py|c|cc|cpp|h|hpp|js|css|html|htm|xml|json|sh|rb|go|rs|lua|gradle|".contains(tag)) {
      return CODE;
    }
    if ("|txt|md|log|ini|cfg|conf|properties|csv|tsv|srt|vtt|yml|yaml|".contains(tag)) {
      return TEXT;
    }
    return FILE;
  }
}
