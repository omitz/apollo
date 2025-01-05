package com.apollo.commandserver;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CommandServerApplication implements CommandLineRunner {

	@Autowired
	private RabbitTemplate rabbitTemplate;
	public static void main(String[] args) {
		SpringApplication.run(CommandServerApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		System.out.println("Sending message to RabbitMQ");
		//rabbitTemplate.convertAndSend("Hello from our first message");
		//rabbitTemplate.convertAndSend( "TestExchange", "testroute", "Hello Wolex");

		ApolloMessage testmsg1 = new ApolloMessage("FaceDetection", "This message is for face detection");
		rabbitTemplate.convertAndSend( "TestExchange", "testroute", testmsg1);
	}
}
