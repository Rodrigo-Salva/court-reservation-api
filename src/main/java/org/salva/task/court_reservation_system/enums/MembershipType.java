package org.salva.task.court_reservation_system.enums;

import lombok.Getter;

@Getter
public enum MembershipType {
    NINGUNA(0.0,7),
    BASICA(0.10, 14),
    PREMIUN(0.20, 30),
    VIP(0.30, 30);

    private final double discountPercentage;
    private final int maxDaysAdvance;

    MembershipType(double discountPercentage, int maxDaysAdvance) {
        this.discountPercentage = discountPercentage;
        this.maxDaysAdvance = maxDaysAdvance;
    }
}
