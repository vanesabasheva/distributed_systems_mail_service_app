package dslab.transfer.tcp;

public class Email {
  private String sender;
  private String[] recipients;
  private String subject;
  private String data;

  public Email(String sender,
               String[] recipients,
               String subject,
               String data) {
    this.sender = sender;
    this.recipients = recipients;
    this.subject = subject;
    this.data = data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public void setRecipients(String[] recipients) {
    this.recipients = recipients;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getData() {
    return data;
  }

  public String getSender() {
    return sender;
  }

  public String getSubject() {
    return subject;
  }

  public String[] getRecipients() {
    return recipients;
  }
}
