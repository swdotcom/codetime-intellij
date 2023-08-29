package com.software.codetime.events;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.List;

public class UserStateChangeModel {
    private final List<ChangeListener> listener = new ArrayList<>();

    public void dispatchAuthenticationCompletion() {
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
