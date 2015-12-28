/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(R). All rights reserved.
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

package com.frostwire.android.gui.activities.internal;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.andrew.apollo.ui.activities.AudioPlayerActivity;
import com.frostwire.android.R;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.activities.MainActivity;
import com.frostwire.android.gui.activities.MainActivity2;
import com.frostwire.android.gui.activities.SettingsActivity;
import com.frostwire.android.gui.activities.WizardActivity;
import com.frostwire.android.gui.adnetworks.Offers;
import com.frostwire.android.gui.fragments.TransfersFragment;
import com.frostwire.android.gui.fragments.TransfersFragment.TransferStatus;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.transfers.TransferManager;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class MainController2 {

    private final MainActivity2 activity2;

    public MainController2(MainActivity2 activity2) {
        this.activity2 = activity2;
    }

    public MainActivity2 getActivity2() {
        return activity2;
    }

    public void closeSlideMenu() {
        activity2.closeSlideMenu();
    }

    public void switchFragment(int itemId) {
        Fragment fragment = activity2.getFragmentByMenuId(itemId);
        if (fragment != null) {
            activity2.switchContent(fragment);
        }
    }

    public void showPreferences() {
        Intent i = new Intent(activity2, SettingsActivity.class);
        activity2.startActivity(i);
    }


    public void launchMyMusic() {
        Intent i = new Intent(activity2, com.andrew.apollo.ui.activities.HomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity2.startActivity(i);
    }


    public void startWizardActivity() {
        Intent i = new Intent(activity2, WizardActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity2.startActivity(i);
    }

    public void launchPlayerActivity() {
        if (Engine.instance().getMediaPlayer().getCurrentFD() != null) {
            Intent i = new Intent(activity2, AudioPlayerActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity2.startActivity(i);
        }
    }

}
