package com.saif.jobnet;

import android.text.Editable;
import android.text.TextWatcher;

public abstract class SimpleTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // No action required
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // No action required
    }

    @Override
    public void afterTextChanged(Editable s) {
        onTextChanged(s.toString());
    }

    public abstract void onTextChanged(String newText);
}
