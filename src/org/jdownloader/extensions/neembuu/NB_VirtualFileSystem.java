/*
 * Copyright (C) 2012 Shashank Tulsyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jdownloader.extensions.neembuu;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import jpfm.DirectoryStream;
import jpfm.fs.SimpleReadOnlyFileSystem;
import jpfm.mount.Mount;
import jpfm.volume.vector.VectorRootDirectory;
import neembuu.vfs.file.ConstrainUtility;
import org.jdownloader.extensions.neembuu.gui.VirtualFilesPanel;

import org.jdownloader.extensions.neembuu.postprocess.PostProcessors;

/**
 * 
 * @author Shashank Tulsyan
 */
public final class NB_VirtualFileSystem extends SimpleReadOnlyFileSystem {

    static NB_VirtualFileSystem newInstance() {
        return new NB_VirtualFileSystem(new VectorRootDirectory());
    }

    private final LinkedList<DownloadSession> sessions           = new LinkedList<DownloadSession>();
    private Mount                             mount              = null;
    private boolean                           removedAllSessions = false;
    private String                            mountLocation      = null;
    private VirtualFilesPanel                 virtualFilesPanel  = null;
    
    private NB_VirtualFileSystem(DirectoryStream rootDirectoryStream) {
        super(rootDirectoryStream);
    }

    public final DirectoryStream getRootDirectory() {
        return /* (VectorRootDirectory) */rootDirectoryStream;
    }

    public String getMountLocation(DownloadSession jdds) throws IOException {
        synchronized (sessions) {
            if (mountLocation == null) {
                mountLocation = makeMountLocation(jdds);
            }
            return mountLocation;
        }
    }

    public VirtualFilesPanel getVirtualFilesPanel() {
        synchronized(sessions){
            return virtualFilesPanel;
        }
    }

    public void setVirtualFilesPanel(VirtualFilesPanel virtualFilesPanel) {
        synchronized(sessions){
            if(this.virtualFilesPanel!=null)throw new IllegalStateException("Already set");
            this.virtualFilesPanel = virtualFilesPanel;
        }
    }

    private static String makeMountLocation(DownloadSession jdds) throws IOException {
        File baseDir = new File(NeembuuExtension.getInstance().getBasicMountLocation());
        if (!baseDir.exists()) baseDir.mkdir();
        File mountLoc = new File(baseDir, jdds.getDownloadLink().getFilePackage().getName());
        mountLoc.deleteOnExit();

        if (mountLoc.exists()) {
            try {
                java.nio.file.Files.delete(mountLoc.toPath());
            } catch (Exception a) {
                mountLoc = new File(mountLoc.toString() + Math.random());
            }
        }
        if (!jpfm.util.PreferredMountTypeUtil.isFolderAPreferredMountLocation()) {
            mountLoc.createNewFile();
        } else {
            mountLoc.mkdir();
        }
        return mountLoc.getAbsolutePath();
    }

    public void addSession(DownloadSession session) {
        synchronized (sessions) {
            sessions.add(session);
        }
        session.getWatchAsYouDownloadSession().getSeekableConnectionFile().setParent(rootDirectoryStream);
        ((VectorRootDirectory) rootDirectoryStream).add(session.getWatchAsYouDownloadSession().getSeekableConnectionFile());

        postProcess();
    }

    public boolean sessionsCompleted() throws Exception {

        synchronized (sessions) {
            Iterator<DownloadSession> it = sessions.iterator();
            while (it.hasNext()) {
                DownloadSession session = it.next();
                if (session.getWatchAsYouDownloadSession().getTotalDownload() != session.getDownloadLink().getDownloadSize()) { return false; }
            }
            // sessions.remove(session);
            // NeembuuExtension.getInstance().getGUI().removeSession(session);
        }
        unmountAndEndSessions();
        return true;
        // ((VectorRootDirectory)rootDirectoryStream).remove(session.getWatchAsYouDownloadSession().getSeekableConnectionFile());
    }

    Mount getMount() {
        if (mount != null) {
            if (!mount.isMounted()) mount = null;
        }
        return mount;
    }

    private void unMount() throws Exception {
        synchronized (sessions) {
            if (mount.isMounted()) mount.unMount();
        }
    }

    public void setMount(Mount mount) {
        this.mount = mount;
    }

    public void unmountAndEndSessions() {
        unmountAndEndSessions(false);
    }

    public void unmountAndEndSessions(boolean alreadyUnmounted) {
        DownloadSession jdds = null;
        synchronized (sessions) {
            if (removedAllSessions) return;
            removedAllSessions = true;
            virtualFilesPanel = null;
            Iterator<DownloadSession> it = sessions.iterator();
            while (it.hasNext()) {
                DownloadSession session = it.next();
                if (jdds == null) jdds = session;
                ((VectorRootDirectory) rootDirectoryStream).remove(session.getWatchAsYouDownloadSession().getSeekableConnectionFile());
                session.getDownloadLink().setEnabled(false);
                try {
                    session.getWatchAsYouDownloadSession().getSeekableConnectionFile().close();
                } catch (IllegalStateException exception) {
                    // ignore
                }
                it.remove();
            }
        }

        if (jdds != null) {
            try {
                if (!alreadyUnmounted) unMount();
            } catch (Exception a) {
                a.printStackTrace(System.err);
            }
            NeembuuExtension.getInstance().getGUI().removeSession(jdds.getWatchAsYouDownloadSession().getFilePanel());
        }
    }

    private void postProcess() {
        synchronized (sessions) {
            if (sessions.size() == sessions.get(0).getDownloadLink().getFilePackage().size()) {
                // all files of the package added
                // post processing can be done now
                PostProcessors.postProcess(sessions, sessions.get(0).getWatchAsYouDownloadSession().getVirtualFileSystem(), sessions.get(0).getWatchAsYouDownloadSession().getMountLocation().getAbsolutePath());
            }
        }

        // if(sessions.size() >= 2)
        ConstrainUtility.constrain(rootDirectoryStream); // this makes
        // each file aware of existence of other filein the same virtual
        // directory. This way
        // if user is watching a few splits like movie.avi.001,movie.avi.002
        // ...movie.avi.007
        // and he forwards video from movie.avi.001 to movie.avi.002 , download
        // at .001 will stop,
        // as .001 will sense the movement of requests from it to other file.

        // If this line is commented out, then, watch as you download might be
        // inefficient
        // in slower connections OR when large number of splits exist
    }

    @Override
    public String toString() {
        return mount == null ? "not mounted" : mount.getMountLocation().toString();
    }
}