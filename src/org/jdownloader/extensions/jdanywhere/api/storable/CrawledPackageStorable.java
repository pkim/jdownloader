package org.jdownloader.extensions.jdanywhere.api.storable;

import java.util.List;

import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.storage.Storable;

public class CrawledPackageStorable implements Storable {

    public String getName() {
        return pkg.getName();
    }

    public void setName(String name) {
    }

    public long getId() {
        return pkg.getUniqueID().getID();
    }

    public void setId(long id) {
    }

    // public String getDldir() {
    // return pkg.getDownloadDirectory();
    // }

    public void setDldir(String dldir) {
    }

    public String getComment() {
        String comment = pkg.getComment();
        if (comment == null || comment.length() == 0) return null;
        return comment;
    }

    public void setComment(String comment) {
    }

    /**
     * @return the added
     */
    public long getAdded() {
        return pkg.getCreated();
    }

    public void setAdded(long added) {
    }

    /**
     * @return the links
     */
    public List<CrawledLinkStoreable> getLinks() {
        return links;
    }

    /**
     * @param links
     *            the links to set
     */
    public void setLinks(List<CrawledLinkStoreable> links) {
        this.links = links;
    }

    private List<CrawledLinkStoreable> links;
    private CrawledPackage               pkg;

    @SuppressWarnings("unused")
    private CrawledPackageStorable() {
    }

    public CrawledPackageStorable(CrawledPackage pkg) {
        this.pkg = pkg;
    }
}