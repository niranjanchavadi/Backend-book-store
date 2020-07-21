package com.bridgelabz.bookstore.utility;

import java.time.LocalDateTime;

public class Util {
	
	
	
	private Util() {
		super();
	}

	public static final String SECRET_KEY = "secret";
	public static final String ISSUER = "Bridgelabz";
	public static final String SUBJECT = "authentication";
	public static final String SENDER_EMAIL_ID = "ubuntushell89@gmail.com";
	public static final String SENDER_PASSWORD = "8884305000";
	public static final String VERIFY_ADDRESS = "http://localhost:8085/parkinglot/verify?token=";
	public static final String RESET_PASSWORD = "http://localhost:8085/parkinglot/resetpassword/";
	public static final String VERIFICATION = "Registration Verification Link";
	public static final String REPLAY_MAILID = "forgotemail@gmail.com";
	public static final String MESSAGEING_RESPONSE = "Parkinglot Management";
	public static final String PASSWORD_UPDATE_MESSAGE = "Password Updations";
	public static final String CHECK_MAIL_MESSAGE = "Please check Your mail for Resetpassword";
	public static final LocalDateTime To_Day_DATE = LocalDateTime.now();
	public static final long REGISTER_EXP = 1800000000;
	public static final long LOGIN_EXP =(long) 5 * 60 * 60 * 10000000;
	public static final short HOURLY_RATE = 20;
	public static final String PARKING_LOT_IS_FULL = "you can't park the car parking lot is full";

}