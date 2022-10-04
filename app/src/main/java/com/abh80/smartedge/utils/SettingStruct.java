package com.abh80.smartedge.utils;

public class SettingStruct {
    public static int TYPE_TOGGLE = 1;
    public static int TYPE_CUSTOM = 2;

    public String text;
    public String category;
    public int type;

    public SettingStruct(String text, String cat) {
        this.text = text;
        this.category = cat;
        type = TYPE_TOGGLE;
    }

    public SettingStruct(String text, String category, int type) {
        this.text = text;
        this.category = category;
        this.type = type;
    }

    public void onCheckChanged(boolean checked) {
    }

    public void onClick() {
    }

    public boolean onAttach() {
        return false;
    }
}
