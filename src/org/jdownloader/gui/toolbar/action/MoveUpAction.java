package org.jdownloader.gui.toolbar.action;

import jd.gui.swing.jdgui.interfaces.View;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class MoveUpAction extends AbstractMoveAction {

    public MoveUpAction() {
        setName(_GUI._.MoveUpAction_MoveUpAction());
        setIconKey("go-up");

    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {
        if (newView instanceof DownloadsView) {
            DownloadsTable table = ((DownloadsTable) DownloadsTableModel.getInstance().getTable());
            setDelegateAction(table.getMoveUpAction());
        } else {
            setDelegateAction(null);
        }
    }

}
