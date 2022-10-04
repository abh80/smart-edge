package com.abh80.smartedge.utils;

import com.google.android.material.materialswitch.MaterialSwitch;

public class ToggleSetting {
    public String text;
    public String category;

    public ToggleSetting(String text, String cat) {
        this.text = text;
        this.category = cat;
    }

    public void onCheckChanged(boolean checked) {
    }

    public boolean onAttach() {
        return false;
    }
}
