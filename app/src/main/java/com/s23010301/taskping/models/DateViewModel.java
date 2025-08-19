package com.s23010301.taskping.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DateViewModel extends ViewModel {
    private final MutableLiveData<String> selectedDate = new MutableLiveData<>();

    public LiveData<String> getDate() {
        return selectedDate;
    }

    public void setDate(String date) {
        selectedDate.setValue(date);
    }
}
