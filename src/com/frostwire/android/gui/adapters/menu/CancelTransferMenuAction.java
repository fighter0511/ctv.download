/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2013, FrostWire(R). All rights reserved.
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

package com.frostwire.android.gui.adapters.menu;

import android.content.Context;

import com.frostwire.android.R;
import com.frostwire.android.gui.transfers.Transfer;
import com.frostwire.android.gui.views.MenuAction;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class CancelTransferMenuAction extends MenuAction {

    private Transfer transfer;

    public CancelTransferMenuAction(Context context, Transfer transfer) {
        super(context, R.drawable.contextmenu_icon_remove_transfer, R.string.delete);

        this.transfer = transfer;
    }

    @Override
    protected void onClick(Context context) {
        transfer.cancel();
        UXStats.instance().log(UXAction.DOWNLOAD_REMOVE);
    }
}
