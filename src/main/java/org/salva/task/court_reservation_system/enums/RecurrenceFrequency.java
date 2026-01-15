package org.salva.task.court_reservation_system.enums;

public enum RecurrenceFrequency {

    NINGUNA(0),
    SEMANAL(7);

    private final int daysToAdd;

    RecurrenceFrequency(int daysToAdd) {
        this.daysToAdd = daysToAdd;
    }
}
