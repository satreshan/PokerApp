package com.sap.ase.poker.model;

public interface Playable {
    void check();
    void call();
    void raise(int amount);
    void fold();
}
