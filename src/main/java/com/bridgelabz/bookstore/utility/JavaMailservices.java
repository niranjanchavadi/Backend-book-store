package com.bridgelabz.bookstore.utility;

import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;



@Component
public class JavaMailservices {

	private Logger logger = LoggerFactory.getLogger(JavaMailservices.class);

	public boolean send(String toEmail, String subject, String body) {

		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		Authenticator auth = new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(Util.SENDER_EMAIL_ID, Util.SENDER_PASSWORD);
			}
		};

		javax.mail.Session session = Session.getInstance(props, auth);

		try {
			MimeMessage msg = new MimeMessage(session);

			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");
			msg.addHeader("Content-Transfer-Encoding", "8bit");

			msg.setFrom(new InternetAddress(Util.REPLAY_MAILID, Util.MESSAGEING_RESPONSE));

			msg.setReplyTo(InternetAddress.parse(Util.SENDER_EMAIL_ID, false));

			msg.setSubject(subject, "UTF-8");

			msg.setText(body, "UTF-8");

			msg.setSentDate(new Date());

			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
			Transport.send(msg);
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
		return false;

	}
	
	
	public void send(String[] emails, String subject, String body) {

		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		Authenticator auth = new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(Util.SENDER_EMAIL_ID, Util.SENDER_PASSWORD);
			}
		};

		javax.mail.Session session = Session.getInstance(props, auth);

		try {
			MimeMessage msg = new MimeMessage(session);

			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");
			msg.addHeader("Content-Transfer-Encoding", "8bit");

			msg.setFrom(new InternetAddress(Util.REPLAY_MAILID, Util.MESSAGEING_RESPONSE));

			msg.setReplyTo(InternetAddress.parse(Util.SENDER_EMAIL_ID, false));

			msg.setSubject(subject, "UTF-8");

			msg.setText(body, "UTF-8");

			msg.setSentDate(new Date());
			
			

			msg.setRecipients(Message.RecipientType.TO, getAddressFromString(emails));
			Transport.send(msg);
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
	}
		
		
		private Address[] getAddressFromString(String[] emails) {
			Address[] addresses = new InternetAddress[emails.length];
			
			for(int i = 0 ; i < emails.length;i++) {
				try {
					addresses[i] = new InternetAddress(emails[i]);
				} catch (AddressException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return addresses;
		}

}