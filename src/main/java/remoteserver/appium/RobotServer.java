package remoteserver.appium;

import org.robotframework.remoteserver.RemoteServer;

public class RobotServer {

  public static void main(String[] args) {
    try {
      RemoteServer.configureLogging();
      RemoteServer server = new RemoteServer();
      server.putLibrary("/", new RobotLibrary());
      server.setPort(2001);
      server.start();
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }
}
