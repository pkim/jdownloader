package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.Dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.translate._JDT;

public class SetDownloadFolderInDownloadTableAction extends AppAction {

    /**
     * 
     */
    private static final long                             serialVersionUID = -6632019767606316873L;

    private boolean                                       retOkay          = false;

    private HashSet<FilePackage>                          packages;
    private HashMap<FilePackage, ArrayList<DownloadLink>> newPackages;

    private FilePackage                                   pkg;

    private File                                          path;

    public boolean newValueSet() {
        return retOkay;
    }

    public SetDownloadFolderInDownloadTableAction(AbstractNode node, ArrayList<AbstractNode> selectio2) {
        if (selectio2 == null) {
            selectio2 = new ArrayList<AbstractNode>();
            selectio2.add(node);
        }

        packages = new HashSet<FilePackage>();
        HashSet<AbstractNode> map = new HashSet<AbstractNode>();
        for (AbstractNode n : selectio2) {
            map.add(n);
            if (n instanceof FilePackage) {
                if (pkg == null) pkg = (FilePackage) n;
                packages.add((FilePackage) n);
            } else {
                if (pkg == null) pkg = ((DownloadLink) n).getParentNode();
                packages.add(((DownloadLink) n).getParentNode());
            }

        }
        newPackages = new HashMap<FilePackage, ArrayList<DownloadLink>>();
        for (Iterator<FilePackage> it = packages.iterator(); it.hasNext();) {
            FilePackage next = it.next();
            boolean allChildren = true;
            boolean noChildren = true;
            ArrayList<DownloadLink> splitme = new ArrayList<DownloadLink>();
            for (DownloadLink l : next.getChildren()) {
                if (!map.contains(l)) {
                    allChildren = false;

                } else {
                    splitme.add(l);
                    noChildren = false;
                }
            }
            // if we do not have all links selected,
            if (allChildren || noChildren) {
                // keep package
            } else {
                // we might need to split packages
                it.remove();
                newPackages.put(next, splitme);

            }

        }
        setName(_GUI._.SetDownloadFolderAction_SetDownloadFolderAction_());
        path = LinkTreeUtils.getDownloadDirectory(node);
        setIconKey("save");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;

        try {

            final ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.OpenDownloadFolderAction_actionPerformed_object_(pkg.getName()), _GUI._.OpenDownloadFolderAction_actionPerformed_save_(), null) {
                @Override
                public JComponent layoutDialogContent() {

                    MigPanel ret = new MigPanel("ins 0,wrap 2", "[grow,fill][]", "[]");
                    ExtTextField lbl = new ExtTextField();
                    lbl.setText(_GUI._.OpenDownloadFolderAction_layoutDialogContent_current_(path.getAbsolutePath()));
                    lbl.setEditable(false);
                    if (CrossSystem.isOpenFileSupported()) {
                        ret.add(lbl);

                        ret.add(new JButton(new AppAction() {
                            {
                                setName(_GUI._.OpenDownloadFolderAction_actionPerformed_button_());
                            }

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                CrossSystem.openFile(path);
                            }

                        }), "height 20!");
                    } else {
                        ret.add(lbl, "spanx");
                    }

                    ret.add(new JSeparator(), "spanx");
                    ret.add(new JLabel(_GUI._.OpenDownloadFolderAction_layoutDialogContent_object_()), "spanx");
                    ret.add(super.layoutDialogContent(), "spanx");
                    return ret;

                }
            };
            if (CrossSystem.isOpenFileSupported()) {
                d.setLeftActions(new AppAction() {
                    {
                        setName(_GUI._.OpenDownloadFolderAction_actionPerformed_button_());
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CrossSystem.openFile(d.getSelection()[0] == null ? path : d.getSelection()[0]);
                    }

                });
            }
            d.setPreSelection(path);
            d.setFileSelectionMode(FileChooserSelectionMode.DIRECTORIES_ONLY);

            final File[] dest = Dialog.getInstance().showDialog(d);

            if (!isDownloadFolderValid(dest[0])) return;
            retOkay = true;
            IOEQ.add(new Runnable() {

                public void run() {

                    for (FilePackage pkg : packages) {
                        pkg.setDownloadDirectory(dest[0].getAbsolutePath());
                    }

                    for (final Entry<FilePackage, ArrayList<DownloadLink>> entry : newPackages.entrySet()) {

                        try {
                            File oldPath = LinkTreeUtils.getDownloadDirectory(entry.getKey().getDownloadDirectory());
                            File newPath = dest[0];
                            if (oldPath.equals(newPath)) continue;
                            Dialog.getInstance().showConfirmDialog(Dialog.LOGIC_DONOTSHOW_BASED_ON_TITLE_ONLY | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN,

                            _JDT._.SetDownloadFolderAction_actionPerformed_(entry.getKey().getName()), _JDT._.SetDownloadFolderAction_msg(entry.getKey().getName(), entry.getValue().size()), null, _JDT._.SetDownloadFolderAction_yes(), _JDT._.SetDownloadFolderAction_no());
                            entry.getKey().setDownloadDirectory(dest[0].getAbsolutePath());
                            continue;
                        } catch (DialogClosedException e) {
                            return;
                        } catch (DialogCanceledException e) {
                            /* user clicked no */
                        }

                        final FilePackage pkg = FilePackage.getInstance();
                        pkg.setExpanded(true);
                        pkg.setCreated(System.currentTimeMillis());
                        pkg.setName(entry.getKey().getName());
                        pkg.setComment(entry.getKey().getComment());
                        pkg.setPasswordList(new ArrayList<String>(Arrays.asList(entry.getKey().getPasswordList())));
                        pkg.getProperties().putAll(entry.getKey().getProperties());
                        pkg.setDownloadDirectory(dest[0].getAbsolutePath());
                        IOEQ.getQueue().add(new QueueAction<Object, RuntimeException>() {
                            @Override
                            protected Object run() {
                                DownloadController.getInstance().addmoveChildren(pkg, entry.getValue(), -1);
                                return null;
                            }
                        });
                    }
                }

            });
        } catch (DialogNoAnswerException e1) {
        }

    }

    @Override
    public boolean isEnabled() {
        return newPackages.size() > 0 || packages.size() > 0;
    }

    /**
     * checks if the given file is valid as a downloadfolder, this means it must
     * be an existing folder or at least its parent folder must exist
     * 
     * @param file
     * @return
     */
    public static boolean isDownloadFolderValid(File file) {
        if (file == null || file.isFile()) return false;
        if (file.isDirectory()) return true;
        File parent = file.getParentFile();
        if (parent != null && parent.isDirectory() && parent.exists()) return true;
        return false;
    }

}