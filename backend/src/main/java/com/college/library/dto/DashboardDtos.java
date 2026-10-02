package com.college.library.dto;

import java.math.BigDecimal;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record NameValue(String name, Number value) {
    }

    /** One month on the charts, e.g. "Jun 2026". */
    public record MonthlyPoint(String month, long issued, long returned, BigDecimal finesCollected) {
    }

    public record AdminDashboard(long totalBooks, long totalCopies, long availableCopies, long borrowedCopies,
                                 long overdueBooks, long totalMembers, long activeMembers,
                                 BigDecimal outstandingFines, BigDecimal finesCollected, long activeReservations,
                                 List<MonthlyPoint> monthly, List<NameValue> mostBorrowedBooks,
                                 List<NameValue> mostActiveMembers, List<NameValue> booksByCategory) {
    }

    public record LibrarianDashboard(long issuedToday, long returnedToday, long overdueBooks, long dueSoon,
                                     long waitingReservations, long readyForPickup, BigDecimal outstandingFines,
                                     BigDecimal collectedToday, List<MonthlyPoint> monthly,
                                     List<CirculationDtos.BorrowingResponse> overdueList) {
    }

    public record MemberDashboard(String memberName, String memberCode, int borrowLimit, long currentBorrowed,
                                  long overdueCount, BigDecimal outstandingFine, BigDecimal accruingFine,
                                  long activeReservations, long readyReservations, long unreadNotifications,
                                  List<CirculationDtos.BorrowingResponse> currentBooks) {
    }
}
