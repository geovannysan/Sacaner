package com.tickets.myapplication.Aplication;

public interface BoletoCallback {
    void onSuccess(String jsonResponse);
    void onFailure(Exception e);
}