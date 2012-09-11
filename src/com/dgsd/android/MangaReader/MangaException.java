package com.dgsd.android.MangaReader;

public class MangaException extends RuntimeException {
    public MangaException() {
        super();
    }

    public MangaException(String detailMessage) {
        super(detailMessage);
    }

    public MangaException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public MangaException(Throwable throwable) {
        super(throwable);
    }
}
