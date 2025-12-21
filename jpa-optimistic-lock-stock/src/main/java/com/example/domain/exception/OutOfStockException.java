package com.example.domain.exception;

public class OutOfStockException extends RuntimeException {

    private final Long stockId;
    private final long current;
    private final long requested;

    public OutOfStockException(Long stockId, long current, long requested) {
        super("OUT_OF_STOCK stockId=" + stockId + ", current=" + current + ", requested=" + requested);
        this.stockId = stockId;
        this.current = current;
        this.requested = requested;
    }

    public Long getStockId() {
        return stockId;
    }

    public long getCurrent() {
        return current;
    }

    public long getRequested() {
        return requested;
    }
}
