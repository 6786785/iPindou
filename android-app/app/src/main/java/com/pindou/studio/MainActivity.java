package com.pindou.studio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQUEST_OPEN_FILE = 2001;
    private static final int REQUEST_GALLERY_PERMISSION = 2003;

    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private String pendingGalleryName;
    private String pendingGalleryMime;
    private String pendingGalleryBase64;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySystemBars();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        // The page owns its light/dark palette. Prevent WebView from applying a second forced-dark pass.
        if (Build.VERSION.SDK_INT >= 33) {
            settings.setAlgorithmicDarkeningAllowed(false);
        } else if (Build.VERSION.SDK_INT >= 29) {
            settings.setForceDark(WebSettings.FORCE_DARK_OFF);
        }

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        updateWebViewBackground();
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;
                Intent intent;
                try {
                    intent = params.createIntent();
                } catch (Exception ignored) {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                }
                try {
                    startActivityForResult(intent, REQUEST_OPEN_FILE);
                    return true;
                } catch (Exception error) {
                    fileChooserCallback = null;
                    toast("无法打开系统文件选择器");
                    return false;
                }
            }
        });

        // Always start a fresh page instance; the page restores its durable autosave from the native store.
        webView.loadUrl("file:///android_asset/index-pro.html");
    }

    private void applySystemBars() {
        boolean dark = isDarkMode();
        getWindow().setStatusBarColor(dark ? Color.rgb(33, 31, 27) : Color.rgb(242, 238, 230));
        getWindow().setNavigationBarColor(dark ? Color.rgb(33, 31, 27) : Color.rgb(242, 238, 230));
        int flags = dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private boolean isDarkMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private void updateWebViewBackground() {
        if (webView == null) return;
        webView.setBackgroundColor(isDarkMode() ? Color.rgb(33, 31, 27) : Color.rgb(242, 238, 230));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applySystemBars();
        updateWebViewBackground();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_FILE && fileChooserCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileChooserCallback.onReceiveValue(result);
            fileChooserCallback = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_GALLERY_PERMISSION) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            writePendingGalleryImage();
        } else {
            clearPendingGalleryImage();
            toast("没有存储权限，图片未保存");
        }
    }

    private File projectsDirectory() {
        File directory = new File(getFilesDir(), "projects");
        if (!directory.exists()) directory.mkdirs();
        return directory;
    }

    private File autosaveFile() {
        return new File(getFilesDir(), "autosave.pbdproj");
    }

    private void writeTextAtomic(File target, String text) throws Exception {
        File temporary = new File(target.getParentFile(), target.getName() + ".tmp");
        try (FileOutputStream stream = new FileOutputStream(temporary, false)) {
            stream.write(text.getBytes(StandardCharsets.UTF_8));
            stream.getFD().sync();
        }
        if (target.exists() && !target.delete()) throw new IllegalStateException("无法更新旧文件");
        if (!temporary.renameTo(target)) throw new IllegalStateException("无法完成文件写入");
    }

    private String readText(File file) throws Exception {
        if (!file.exists()) return "";
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream stream = new FileInputStream(file)) {
            int offset = 0;
            while (offset < bytes.length) {
                int count = stream.read(bytes, offset, bytes.length - offset);
                if (count < 0) break;
                offset += count;
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String safeId(String candidate) {
        if (candidate != null && candidate.matches("[A-Za-z0-9_-]{8,80}")) return candidate;
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String safeFileName(String candidate, String fallback) {
        String value = candidate == null ? "" : candidate.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return value.isEmpty() ? fallback : value;
    }

    private void saveImageToGalleryInternal(String fileName, String mimeType, String base64) {
        pendingGalleryName = safeFileName(fileName, "拼豆图纸.png");
        pendingGalleryMime = mimeType == null || mimeType.isEmpty() ? "image/png" : mimeType;
        pendingGalleryBase64 = base64;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_GALLERY_PERMISSION);
            return;
        }
        writePendingGalleryImage();
    }

    private void writePendingGalleryImage() {
        try {
            byte[] bytes = Base64.decode(pendingGalleryBase64, Base64.DEFAULT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, pendingGalleryName);
                values.put(MediaStore.Images.Media.MIME_TYPE, pendingGalleryMime);
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/爱拼豆");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IllegalStateException("无法创建相册文件");
                try (OutputStream stream = getContentResolver().openOutputStream(uri, "w")) {
                    if (stream == null) throw new IllegalStateException("无法写入相册");
                    stream.write(bytes);
                }
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
            } else {
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "爱拼豆");
                if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("无法创建相册目录");
                File target = uniqueFile(directory, pendingGalleryName);
                try (FileOutputStream stream = new FileOutputStream(target)) { stream.write(bytes); }
                MediaScannerConnection.scanFile(this, new String[]{target.getAbsolutePath()}, new String[]{pendingGalleryMime}, null);
            }
            toast("图片已保存到相册 / 爱拼豆");
        } catch (Exception error) {
            toast("图片保存失败：" + error.getMessage());
        } finally {
            clearPendingGalleryImage();
        }
    }

    private File uniqueFile(File directory, String name) {
        File target = new File(directory, name);
        if (!target.exists()) return target;
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        return new File(directory, stem + "_" + System.currentTimeMillis() + extension);
    }

    private void clearPendingGalleryImage() {
        pendingGalleryName = null;
        pendingGalleryMime = null;
        pendingGalleryBase64 = null;
    }

    private void toast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
    }

    public class AndroidBridge {
        @JavascriptInterface
        public String saveProjectInternal(String requestedId, String title, String summaryJson, String projectJson) {
            try {
                String id = safeId(requestedId);
                File directory = projectsDirectory();
                writeTextAtomic(new File(directory, id + ".pbdproj"), projectJson);
                JSONObject summary = summaryJson == null || summaryJson.isEmpty() ? new JSONObject() : new JSONObject(summaryJson);
                JSONObject metadata = new JSONObject();
                metadata.put("id", id);
                metadata.put("title", title == null || title.trim().isEmpty() ? "未命名项目" : title.trim());
                metadata.put("size", summary.optString("size", "未生成"));
                metadata.put("colors", summary.optInt("colors", 0));
                metadata.put("modifiedAt", System.currentTimeMillis());
                writeTextAtomic(new File(directory, id + ".meta.json"), metadata.toString());
                return id;
            } catch (Exception error) {
                toast("项目保存失败：" + error.getMessage());
                return "";
            }
        }

        @JavascriptInterface
        public String listProjects() {
            JSONArray result = new JSONArray();
            try {
                File[] files = projectsDirectory().listFiles((dir, name) -> name.endsWith(".meta.json"));
                if (files == null) return result.toString();
                Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
                for (File file : files) {
                    try { result.put(new JSONObject(readText(file))); }
                    catch (Exception ignored) { }
                }
            } catch (Exception ignored) { }
            return result.toString();
        }

        @JavascriptInterface
        public String loadProjectInternal(String id) {
            try { return readText(new File(projectsDirectory(), safeId(id) + ".pbdproj")); }
            catch (Exception error) { return ""; }
        }

        @JavascriptInterface
        public boolean deleteProjectInternal(String id) {
            String safe = safeId(id);
            File data = new File(projectsDirectory(), safe + ".pbdproj");
            File metadata = new File(projectsDirectory(), safe + ".meta.json");
            boolean dataDeleted = !data.exists() || data.delete();
            boolean metadataDeleted = !metadata.exists() || metadata.delete();
            return dataDeleted && metadataDeleted;
        }

        @JavascriptInterface
        public void saveAutosave(String projectJson) {
            try { writeTextAtomic(autosaveFile(), projectJson); }
            catch (Exception error) { toast("自动保存失败"); }
        }

        @JavascriptInterface
        public String loadAutosave() {
            try { return readText(autosaveFile()); }
            catch (Exception error) { return ""; }
        }

        @JavascriptInterface
        public void clearAutosave() {
            File file = autosaveFile();
            if (file.exists()) file.delete();
        }

        @JavascriptInterface
        public void saveImageToGallery(String fileName, String mimeType, String base64) {
            runOnUiThread(() -> saveImageToGalleryInternal(fileName, mimeType, base64));
        }

        @JavascriptInterface
        public void saveBase64(String fileName, String mimeType, String base64) {
            try {
                File directory = new File(getFilesDir(), "exports");
                if (!directory.exists()) directory.mkdirs();
                File target = uniqueFile(directory, safeFileName(fileName, "export.bin"));
                try (FileOutputStream stream = new FileOutputStream(target)) {
                    stream.write(Base64.decode(base64, Base64.DEFAULT));
                }
                toast("文件已保存在应用内部");
            } catch (Exception error) {
                toast("文件保存失败：" + error.getMessage());
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidBridge");
            webView.destroy();
        }
        super.onDestroy();
    }
}
