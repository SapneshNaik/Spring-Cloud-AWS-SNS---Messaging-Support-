package com.kerneldev.demo;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import static com.kerneldev.demo.AwsSnsUtilityMethods.getAwsCredentials;
import static com.kerneldev.demo.AwsSnsUtilityMethods.getSnsClient;


@RestController
public class DemoController {
    @RequestMapping("/createTopic")
    private String createTopic(@RequestParam("topic_name") String topicName) throws URISyntaxException {
        //topic name cannot contain spaces
        final CreateTopicRequest topicCreateRequest = CreateTopicRequest.builder().name(topicName).build();

        SnsClient snsClient = getSnsClient();

        final CreateTopicResponse topicCreateResponse = snsClient.createTopic(topicCreateRequest);

        if (topicCreateResponse.sdkHttpResponse().isSuccessful()) {
            System.out.println("Topic creation successful");
            System.out.println("Topics: " + snsClient.listTopics());
        } else {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, topicCreateResponse.sdkHttpResponse().statusText().get()
            );
        }

        snsClient.close();
        return "Topic ARN: " + topicCreateResponse.topicArn();
    }





    @RequestMapping("/addSubscribers")
    private String addSubscriberToTopic(@RequestParam("arn") String arn) throws URISyntaxException {

        SnsClient snsClient = getSnsClient();

        final SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                .topicArn(arn)
                .protocol("email")
                .endpoint("sapnesh@kerneldev.com")
                .build();

        SubscribeResponse subscribeResponse = snsClient.subscribe(subscribeRequest);

        if (subscribeResponse.sdkHttpResponse().isSuccessful()) {
            System.out.println("Subscriber creation successful");
        } else {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, subscribeResponse.sdkHttpResponse().statusText().get()
            );
        }

        snsClient.close();

        return "Subscription ARN request is pending. To confirm the subscription, check your email.";
    }


    @RequestMapping("/sendEmail")
    private String sendEmail(@RequestParam("arn") String arn) throws URISyntaxException {

        SnsClient snsClient = getSnsClient();

        final SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                .topicArn(arn)
                .protocol("email")
                .endpoint("sapnesh@kerneldev.com")
                .build();

        final String msg = "This Stack Abuse Demo email works!";

        final PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(arn)
                .subject("Stack Abuse Demo email")
                .message(msg)
                .build();


        PublishResponse publishResponse = snsClient.publish(publishRequest);


        if (publishResponse.sdkHttpResponse().isSuccessful()) {
            System.out.println("Message publishing successful");
        } else {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, publishResponse.sdkHttpResponse().statusText().get()
            );
        }

        snsClient.close();

        return "Email sent to subscribers. Message ID: " + publishResponse.messageId();
    }

    @RequestMapping("/sendEmailWithAttachment")
    private String sendEmailWithAttachment(@RequestParam("arn") String arn) throws URISyntaxException, MessagingException, IOException {

        String subject = "Stack Abuse AWS SES Demo";

        String attachment = "C:\\Users\\sapneshn\\Google Drive\\Stack Abuse\\Topic.PNG";

        String body = "<html>"
                + "<body>"
                + "<h1>Hello Stack Abuser!</h1>"
                + "<p>Please check your email for attachment"
                + "</body>"
                + "</html>";

        Session session = Session.getDefaultInstance(new Properties(), null);

        MimeMessage message = new MimeMessage(session);

        // Add subject, from and to lines.
        message.setSubject(subject, "UTF-8");
        message.setFrom(new InternetAddress("sapneshwk@gmail.com")); // you aws account email
        message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse("sapnesh@kerneldev.com")); // recipient email

        MimeMultipart msg_body = new MimeMultipart("alternative");
        MimeBodyPart wrap = new MimeBodyPart();

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(body, "text/html; charset=UTF-8");
        msg_body.addBodyPart(htmlPart);
        wrap.setContent(msg_body);

        MimeMultipart msg = new MimeMultipart("mixed");

        // Add the parent container to the message.
        message.setContent(msg);

        // Add the multipart/alternative part to the message.
        msg.addBodyPart(wrap);

        // Define the attachment
        MimeBodyPart att = new MimeBodyPart();
        DataSource fds = new FileDataSource(attachment);
        att.setDataHandler(new DataHandler(fds));
        att.setFileName(fds.getName());

        // Add the attachment to the message.
        msg.addBodyPart(att);


        SesClient sesClient = SesClient.builder()
                .credentialsProvider(getAwsCredentials(
                        "Access Key ID",
                        "Secret Key/"))
                .region(Region.US_EAST_1) //Set your selected region
                .build();

        // Send the email.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(outputStream);

        RawMessage rawMessage = RawMessage.builder().data(SdkBytes.fromByteArray(outputStream.toByteArray())).build();

        SendRawEmailRequest rawEmailRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build();

        SendRawEmailResponse sendRawEmailResponse = sesClient.sendRawEmail(rawEmailRequest);
        // Display an error if something goes wrong.

        if (sendRawEmailResponse.sdkHttpResponse().isSuccessful()) {
            System.out.println("Message publishing successful");
        } else {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, sendRawEmailResponse.sdkHttpResponse().statusText().get()
            );
        }

        return "Email sent to subscribers. Message ID: " + sendRawEmailResponse.messageId();
    }

    @RequestMapping("/sendSMS")
    private String sendSMS(@RequestParam("phone") String phone) throws URISyntaxException {

        SnsClient snsClient = getSnsClient();

        final PublishRequest publishRequest = PublishRequest.builder()
                .phoneNumber(phone)
                .message("This is Stack Abuse SMS Demo")
                .build();


        PublishResponse publishResponse = snsClient.publish(publishRequest);


        if (publishResponse.sdkHttpResponse().isSuccessful()) {
            System.out.println("Message publishing to phone successful");
        } else {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, publishResponse.sdkHttpResponse().statusText().get()
            );
        }

        snsClient.close();

        return "SMS sent to "+phone+". Message ID: " + publishResponse.messageId();
    }

    @RequestMapping("/sendBulkSMS")
    private String sendBulkSMS(@RequestParam("arn") String arn) throws URISyntaxException {

        SnsClient snsClient = getSnsClient();

        String[] phoneNumbers = new String[]{"+917760041698", "917760041698", "7760041698" };

        for (String phoneNumber: phoneNumbers) {
            final SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                    .topicArn(arn)
                    .protocol("sms")
                    .endpoint(phoneNumber)
                    .build();

            SubscribeResponse subscribeResponse = snsClient.subscribe(subscribeRequest);
            if (subscribeResponse.sdkHttpResponse().isSuccessful()) {
                System.out.println(phoneNumber + " subscribed to topic "+arn);
            }
        }

        final PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(arn)
                .message("This is Stack Abuse SMS Demo")
                .build();


        PublishResponse publishResponse = snsClient.publish(publishRequest);


        if (publishResponse.sdkHttpResponse().isSuccessful()) {
            System.out.println("Bulk Message sending successful");
            System.out.println(publishResponse.messageId());
        } else {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, publishResponse.sdkHttpResponse().statusText().get()
            );
        }

        snsClient.close();

        return "Done";
    }
}
