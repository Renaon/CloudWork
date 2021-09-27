package com.polozov.cloudstorage.lesson01.server;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
	// TODO: 14.06.2021
	// организовать корректный вывод статуса
	// подумать почему так реализован цикл в ClientHandler
	//ошибку со статусом не поймал, она у меня попросту не появилась
	//цикл реализован таким образом для того, чтоб корректно был передан бит четности,
	// который также необходимо прочитать
	//TODO close.
	public Server() {
		ExecutorService service = Executors.newFixedThreadPool(4);
		try (ServerSocket server = new ServerSocket(5678)){
			System.out.println("Server started");
			while (true) {
				service.execute(new ClientHandler(server.accept()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Server();
	}
}
