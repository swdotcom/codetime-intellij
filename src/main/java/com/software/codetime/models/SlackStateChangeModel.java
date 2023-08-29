package com.software.codetime.models;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.List;

public class SlackStateChangeModel {
    private final List<ChangeListener> listener = new ArrayList<>();

    public void dispatchSlackStateChangeCompletion() {
        notifyListeners();
    }

    private void notifyListeners() {
        for (ChangeListener name : listener) {
            name.stateChanged(new ChangeEvent(this));
        }
    }

    public void addChangeListener(ChangeListener newListener) {
        listener.add(newListener);
    }
}
