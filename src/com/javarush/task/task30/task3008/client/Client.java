package com.javarush.task.task30.task3008.client;

import static com.javarush.task.task30.task3008.MessageType.NAME_ACCEPTED;
import static com.javarush.task.task30.task3008.MessageType.NAME_REQUEST;
import static com.javarush.task.task30.task3008.MessageType.TEXT;
import static com.javarush.task.task30.task3008.MessageType.USER_ADDED;
import static com.javarush.task.task30.task3008.MessageType.USER_NAME;
import static com.javarush.task.task30.task3008.MessageType.USER_REMOVED;

import com.javarush.task.task30.task3008.Connection;
import com.javarush.task.task30.task3008.ConsoleHelper;
import com.javarush.task.task30.task3008.Message;
import java.io.IOException;
import java.net.Socket;

public class Client {

  protected Connection connection;
  private volatile boolean clientConnected = false;

  public static void main(String[] args) throws InterruptedException {
    Client client = new Client();
    client.run();
  }

  public class SocketThread extends Thread {

    // должен выводить текст message в консоль
    protected void processIncomingMessage(String message) {
      ConsoleHelper.writeMessage(message);
    }

    // должен выводить в консоль информацию о том, что участник с именем userName присоединился к чату
    protected void informAboutAddingNewUser(String userName) {
      ConsoleHelper.writeMessage("Пользователь с именем " + userName + " присоединился к чату");
    }

    // должен выводить в консоль, что участник с именем userName покинул чат
    protected void informAboutDeletingNewUser(String userName) {
      ConsoleHelper.writeMessage("Пользователь с именем " + userName + " покинул чат");
    }

    // метод должен:
    // - Устанавливать значение поля clientConnected внешнего объекта Client в соответствии с переданным параметром.
    // - Оповещать (пробуждать ожидающий) основной поток класса Client.
    protected void notifyConnectionStatusChanged(boolean clientConnected) {
      synchronized (Client.this) {
        Client.this.clientConnected = clientConnected;
        Client.this.notify();
      }

    }

    protected void clientHandshake() throws IOException, ClassNotFoundException {
      while (true) {
        Message message = connection.receive();
        if (message.getType() == NAME_REQUEST) {
          Message messageClient = new Message(USER_NAME, getUserName());
          connection.send(messageClient);
        } else if (message.getType() == NAME_ACCEPTED) {
          notifyConnectionStatusChanged(true);
          break;
        } else {
          throw new IOException("Unexpected MessageType");
        }
      }
    }

    // метод будет реализовывать главный цикл обработки сообщений сервера
    protected void clientMainLoop() throws IOException, ClassNotFoundException {
      while (true) {
        Message message = connection.receive();
        if (message.getType() == TEXT) {
          processIncomingMessage(message.getData());
        } else if (message.getType() == USER_ADDED) {
          informAboutAddingNewUser(message.getData());
        } else if (message.getType() == USER_REMOVED) {
          informAboutDeletingNewUser(message.getData());
        } else {
          throw new IOException("Unexpected MessageType");
        }
      }
    }

    public void run() {
      try {
        Socket socket = new Socket(getServerAddress(), getServerPort());
        connection = new Connection(socket);
        clientHandshake();
        clientMainLoop();
      } catch (IOException | ClassNotFoundException e) {
        notifyConnectionStatusChanged(false);
      }
    }
  }

  // должен запросить ввод адреса сервера у пользователя и вернуть введенное значение
  protected String getServerAddress() throws IOException {
    System.out.println("Пожалуйста, введите адрес сервера");
    return ConsoleHelper.readString();
  }

  // должен запрашивать ввод порта сервера и возвращать его
  protected int getServerPort() {
    System.out.println("Пожалуйста, введите порт");
    return ConsoleHelper.readInt();
  }

  // должен запрашивать и возвращать имя пользователя
  protected String getUserName() {
    System.out.println("Пожалуйста, введите username");
    return ConsoleHelper.readString();
  }

  // в данной реализации клиента всегда должен возвращать true (мы всегда отправляем текст введенный в консоль).
  //Этот метод может быть переопределен, если мы будем писать какой-нибудь другой клиент, унаследованный от нашего,
  // который не должен отправлять введенный в консоль текст.
  protected boolean shouldSendTextFromConsole() {
    return true;
  }

  // должен создавать и возвращать новый объект класса SocketThread
  protected SocketThread getSocketThread() {
    return new SocketThread();
  }

  // создает новое текстовое сообщение, используя переданный текст и отправляет его серверу через соединение connection.
  //Если во время отправки произошло исключение IOException, то необходимо вывести информацию об этом пользователю и присвоить false полю clientConnected.
  protected void sendTextMessage(String text) {
    try {
      Message message = new Message(TEXT, text);
      connection.send(message);
    } catch (IOException io) {
      System.out.println("Возникла ошибка");
      clientConnected = false;
    }
  }

  public void run() throws InterruptedException {
    SocketThread socketThread = getSocketThread();
    socketThread.setDaemon(true);
    socketThread.start();
    synchronized (this) {
      try {
        this.wait();
        if (clientConnected) {
          ConsoleHelper.writeMessage("Соединение установлено.\n "
              + "Для выхода наберите команду 'exit'");
        } else {
          ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }
        while (clientConnected) {
          String message = ConsoleHelper.readString();
          if (shouldSendTextFromConsole()) {
            sendTextMessage(message);
          }
          if (message.equals("exit")) {
            break;
          }
        }
      } catch (Exception io) {
        ConsoleHelper.writeMessage("Возникла ошибка");
        System.exit(0);
      }
    }
  }
}
