package com.tvminvestments.zscore;

/**
 * Created by horse on 26/11/14.
 */
public enum ResultCode {
    ENTRY_EXIT,
    ENTRY_NO_EXIT,
    NO_ENTRY, ENTRY, EXIT;

    @Override
    public String toString() {
        switch(this) {
            case ENTRY_EXIT: return "EE";
            case ENTRY_NO_EXIT: return "ENE";
            case NO_ENTRY: return "NE";
            case ENTRY: return "ENTRY";
            case EXIT: return "EXIT";
            default: return null;
        }
    }
}
