package rsv.process.lib;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
/* Obsoleted by service monitor
public class SendMail {

	private String from;
	private String to;
	private String subject;
	private String text;
	
	public SendMail(String from, String to, String subject, String text){
		this.from = from;
		this.to = to;
		this.subject = subject;
		this.text = text;
	}
	
	public void send(){
		
		Properties props = new Properties();
		props.put("mail.smtp.host", "localhost");
		props.put("mail.smtp.port", "25");
		
		Session mailSession = Session.getDefaultInstance(props);
		Message simpleMessage = new MimeMessage(mailSession);
		
		InternetAddress fromAddress = null;
		InternetAddress toAddress = null;
		try {
			fromAddress = new InternetAddress(from);
			toAddress = new InternetAddress(to);
		} catch (AddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			simpleMessage.setFrom(fromAddress);
			simpleMessage.setRecipient(RecipientType.TO, toAddress);
			simpleMessage.setSubject(subject);
			simpleMessage.setText(text);
			
			Transport.send(simpleMessage);			
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	static public void sendErrorEmail(String content) {
		String from = "goc@opensciencegrid.org";
		String to = "hayashis@opensciencegrid.org";
		String subject = "RSV Process Error";
		String message = "RSV Process has detected an error\r\n" + content;

		SendMail sendMail = new SendMail(from, to, subject, message);
		sendMail.send();
	}
}
*/