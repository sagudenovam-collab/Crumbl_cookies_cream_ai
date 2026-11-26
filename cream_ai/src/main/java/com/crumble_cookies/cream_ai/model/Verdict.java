package com.crumble_cookies.cream_ai.model;

/** Итоговый вердикт готовности документа требований. */
public enum Verdict {
    READY,
    READY_WITH_RESERVATIONS,
    NOT_READY,
    INSUFFICIENT_INFORMATION
}
