package com.javarush.task.task30.task3008;

import static com.javarush.task.task30.task3008.MessageType.NAME_ACCEPTED;
import static com.javarush.task.task30.task3008.MessageType.USER_ADDED;
import static com.javarush.task.task30.task3008.MessageType.USER_NAME;
import static com.javarush.task.task30.task3008.MessageType.USER_REMOVED;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

// основной класс сервера
public class Server {

  private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

  public static void main(String[] args) {
    try (ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())) {
      ConsoleHelper.writeMessage("Сервер запустился.");
      while (true) {
        new Handler(serverSocket.accept()).start();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sendBroadcastMessage(Message message) {
    try {
      for (Connection connection : connectionMap.values()) {
        connection.send(message);
      }
    } catch (IOException e) {
      System.out.println("Сообщение не может быть отправлено.");
    }
  }

  private static class Handler extends Thread {

    Socket socket;

    Handler(Socket socket) {
      this.socket = socket;
    }

    private String serverHandshake(Connection connection)
        throws IOException, ClassNotFoundException {
      Message nameRequest = new Message(MessageType.NAME_REQUEST, "Пожалуйста, введите имя.");
      connection.send(nameRequest);

      Message clientMessage = connection.receive();
      MessageType type = clientMessage.getType();
      String username = clientMessage.getData();
      if (type.equals(USER_NAME) && !username.isEmpty() && !connectionMap.containsKey(username)) {
        connectionMap.put(clientMessage.getData(), connection);
        Message nameAccepted = new Message(NAME_ACCEPTED, "Ваше имя принято.");
        connection.send(nameAccepted);
        return username;
      } else {
        return serverHandshake(connection);
      }
    }

    private void notifyUsers(Connection connection, String userName) throws IOException {
      for (Entry<String, Connection> user : connectionMap.entrySet()) {
        String username = user.getKey();
        Message message = new Message(USER_ADDED, username);
        if (!username.equals(userName)) {
          connection.send(message);
        }
      }
    }

    private void serverMainLoop(Connection connection, String userName)
        throws IOException, ClassNotFoundException {
      while (true) {
        Message message = connection.receive();
        if (message != null && message.getType() == MessageType.TEXT) {
          sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
        } else {
          ConsoleHelper.writeMessage("Ошибка!");
        }
      }
    }

    public void run() {
      String username = "";
      try {
        ConsoleHelper.writeMessage(
            "Новое соединение с " + socket.getRemoteSocketAddress() + " установлено.");
        Connection connection = new Connection(socket);
        username = serverHandshake(connection);
        sendBroadcastMessage(new Message(USER_ADDED, username));
        notifyUsers(connection, username);
        serverMainLoop(connection, username);
      } catch (IOException | ClassNotFoundException ex) {
        ConsoleHelper.writeMessage("Ошибка возникла во время соединения с удалённым адресом.");
      }
      connectionMap.remove(username);
      sendBroadcastMessage(new Message(USER_REMOVED, username));
      ConsoleHelper.writeMessage("Соединение с удалённым адресом было закрыто.");
    }
  }
}
