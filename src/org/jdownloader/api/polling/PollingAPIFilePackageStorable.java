package org.jdownloader.api.polling;

import java.util.ArrayList;
import java.util.List;

import jd.plugins.FilePackage;

import org.appwork.storage.Storable;

public class PollingAPIFilePackageStorable implements Storable {

    private FilePackage                          fp;
    private List<PollingAPIDownloadLinkStorable> downloadLinks = new ArrayList<PollingAPIDownloadLinkStorable>();

    public PollingAPIFilePackageStorable(/* Storable */) {
        this.fp = null;
    }

    public PollingAPIFilePackageStorable(FilePackage link) {
        this.fp = link;
    }

    public Long getUUID() {
        return fp.getUniqueID().getID();
    }

    public Long getDone() {
        return fp.getView().getDone();
    }

    public Long getSize() {
        return fp.getView().getSize();
    }

    public List<PollingAPIDownloadLinkStorable> getLinks() {
        return downloadLinks;
    }

    public void setLinks(List<PollingAPIDownloadLinkStorable> downloadLinks) {
        this.downloadLinks = downloadLinks;
    }
}