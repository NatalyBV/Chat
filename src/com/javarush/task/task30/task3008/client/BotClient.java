package com.javarush.task.task30.task3008.client;

import com.javarush.task.task30.task3008.ConsoleHelper;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BotClient extends Client {

  public static void main(String[] args) throws InterruptedException {
    BotClient bot = new BotClient();
    bot.run();
  }

  protected String getUserName() {
    int x = (int) (1 + Math.random() * 99);
    return "date_bot_" + x;
  }

  protected boolean shouldSendTextFromConsole() {
    return false;
  }

  protected SocketThread getSocketThread() {
    return new BotSocketThread();
  }

  public class BotSocketThread extends SocketThread {

    protected void clientMainLoop() throws IOException, ClassNotFoundException {
      sendTextMessage(
          "Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
      super.clientMainLoop();
    }

    protected void processIncomingMessage(String message) {
      ConsoleHelper.writeMessage(message);
      if (message.contains(":")) {
        String delimiter = ": ";
        String username = message.split(delimiter)[0];
        String text = message.split(delimiter)[1];
        DateFormat sdf1 = new SimpleDateFormat("d.MM.YYYY");
        DateFormat sdf2 = new SimpleDateFormat("d");
        DateFormat sdf3 = new SimpleDateFormat("MMMM");
        DateFormat sdf4 = new SimpleDateFormat("YYYY");
        DateFormat sdf5 = new SimpleDateFormat("H:mm:ss");
        DateFormat sdf6 = new SimpleDateFormat("H");
        DateFormat sdf7 = new SimpleDateFormat("m");
        DateFormat sdf8 = new SimpleDateFormat("s");
        Calendar calendar = Calendar.getInstance();

        StringBuilder messageToSend = new StringBuilder();
        messageToSend.append("Информация для ").append(username).append(": ");

        boolean isInterpreted = true;
        switch (text) {
          case "дата":
            messageToSend.append(sdf1.format(calendar.getTime()));
            break;
          case "день":
            messageToSend.append(sdf2.format(calendar.getTime()));
            break;
          case "месяц":
            messageToSend.append(sdf3.format(calendar.getTime()));
            break;
          case "год":
            messageToSend.append(sdf4.format(calendar.getTime()));
            break;
          case "время":
            messageToSend.append(sdf5.format(calendar.getTime()));
            break;
          case "час":
            messageToSend.append(sdf6.format(calendar.getTime()));
            break;
          case "минуты":
            messageToSend.append(sdf7.format(calendar.getTime()));
            break;
          case "секунды":
            messageToSend.append(sdf8.format(calendar.getTime()));
            break;
          default:
            isInterpreted = false;
        }
        if (isInterpreted) {
          sendTextMessage(messageToSend.toString());
        }
      }
    }
  }
}