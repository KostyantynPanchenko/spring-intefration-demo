package net.ukr.yougetit;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.io.File;
import java.util.Scanner;

@SpringBootApplication
@EnableIntegration
public class SpringIntegrationDemoApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(SpringIntegrationDemoApplication.class)
                .web(WebApplicationType.NONE)
                .application()
                .run(args);
        context.registerShutdownHook();

        Scanner scanner = new Scanner(System.in);
        System.out.print("Please enter `q` and press <enter> to exit the program: ");

        while (true) {
            String input = scanner.nextLine();
            if ("q".equals(input.trim())) {
                break;
            }
        }
        System.exit(0);
    }

    /**
     * <p>
     * A message channel represents the “pipe” of a pipes-and-filters architecture. Producers send messages to a channel,
     * and consumers receive messages from a channel. The message channel therefore decouples the messaging components
     * and also provides a convenient point for interception and monitoring of messages.
     * </p>
     * <p>
     * A message channel may follow either point-to-point or publish-subscribe semantics. With a point-to-point channel,
     * no more than one consumer can receive each message sent to the channel. Publish-subscribe channels, on the other
     * hand, attempt to broadcast each message to all subscribers on the channel. Spring Integration supports both of
     * these models.
     * </p>
     * <p>
     * The DirectChannel has point-to-point semantics.
     * </p>
     * <p>
     * In addition to being the simplest point-to-point channel option, one of its most important features is that it
     * enables a single thread to perform the operations on “both sides” of the channel. For example, if a handler
     * subscribes to a DirectChannel, then sending a Message to that channel triggers invocation of that handler’s
     * handleMessage(Message) method directly in the sender’s thread, before the send() method invocation can return.
     * </p>
     * <p>
     * The key motivation for providing a channel implementation with this behavior is to support transactions that must
     * span across the channel while still benefiting from the abstraction and loose coupling that the channel provides.
     * If the send call is invoked within the scope of a transaction, the outcome of the handler’s invocation
     * (for example, updating a database record) plays a role in determining the ultimate result of that transaction
     * (commit or rollback).
     * </p>
     * @return instance of DirectChannel
     */
    @Bean
    public MessageChannel fileMovingChannel() {
        return MessageChannels.direct().get(); // alternative - new DirectChannel()
    }

    /**
     * The Adapter is an enterprise integration pattern-based component that allows one to “plug-in” to a system or data
     * source. It is almost literally an adapter as we know it from plugging into a wall socket or electronic device.
     * It allows reusable connectivity to otherwise “black-box” systems like databases, FTP servers and messaging
     * systems such as JMS, AMQP, and social networks like Twitter.
     *
     * @return instance of MessageSource
     */
    @Bean
    @InboundChannelAdapter(value = "fileMovingChannel", poller = @Poller(fixedDelay = "1000"))
    public MessageSource<File> fileMessageSource() {
        FileReadingMessageSource fileSource = new FileReadingMessageSource();
        fileSource.setDirectory(new File("dir_src"));
        fileSource.setFilter(new SimplePatternFileListFilter("*.txt"));
        return fileSource;
    }

    /**
     * The Service Activator is any POJO that defines the @ServiceActivator annotation on a given method. This allows us
     * to execute any method on our POJO when a message is received from an inbound channel, and it allows us to write
     * messages to an outward channel.
     *
     * @return instance of MessageHandler
     */
    @Bean
    @ServiceActivator(inputChannel = "fileMovingChannel")
    public MessageHandler fileMessageHandler() {
        FileWritingMessageHandler messageHandler = new FileWritingMessageHandler(new File("dir_target"));
        messageHandler.setAutoCreateDirectory(true);
        messageHandler.setRequiresReply(false);
        messageHandler.setExpectReply(false);
        messageHandler.setFileExistsMode(FileExistsMode.REPLACE);
        messageHandler.setLoggingEnabled(true);
        return messageHandler;
    }
}
