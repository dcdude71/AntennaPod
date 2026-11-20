package de.danoeh.antennapod.model.playlist;

public class Playlist {
    private long id;
    private String name;
    private boolean autoDownload;
    private boolean showInAndroidAuto;
    private int position;
    private boolean downloadedOnly;
    private boolean manual;
    private boolean keepPlayed;

    public Playlist(long id, String name, boolean autoDownload, boolean showInAndroidAuto, int position) {
        this.id = id;
        this.name = name;
        this.autoDownload = autoDownload;
        this.showInAndroidAuto = showInAndroidAuto;
        this.position = position;
    }

    public Playlist(String name) {
        this(0, name, false, true, 0);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAutoDownload() {
        return autoDownload;
    }

    public void setAutoDownload(boolean autoDownload) {
        this.autoDownload = autoDownload;
    }

    public boolean isShowInAndroidAuto() {
        return showInAndroidAuto;
    }

    public void setShowInAndroidAuto(boolean showInAndroidAuto) {
        this.showInAndroidAuto = showInAndroidAuto;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isDownloadedOnly() {
        return downloadedOnly;
    }

    public void setDownloadedOnly(boolean downloadedOnly) {
        this.downloadedOnly = downloadedOnly;
    }

    public boolean isManual() {
        return manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }

    public boolean isKeepPlayed() {
        return keepPlayed;
    }

    public void setKeepPlayed(boolean keepPlayed) {
        this.keepPlayed = keepPlayed;
    }
}
