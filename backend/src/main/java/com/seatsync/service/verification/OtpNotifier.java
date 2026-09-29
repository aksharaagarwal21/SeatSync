package com.seatsync.service.verification;

import com.seatsync.entity.OtpPurpose;
import com.seatsync.entity.User;

import java.time.Duration;

/** Delivers a verification code to the user. */
public interface OtpNotifier {

    void send(User user, OtpPurpose purpose, String code, String summary, Duration validFor);
}
