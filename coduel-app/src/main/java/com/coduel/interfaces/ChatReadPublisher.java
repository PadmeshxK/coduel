package com.coduel.interfaces;

import com.coduel.model.data.ReadReceiptData;

/** Port for delivering a DM read-receipt to the message author (by their googleId principal). */
public interface ChatReadPublisher {

    void publish(String googleId, ReadReceiptData receipt);
}
