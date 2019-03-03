package remoteserver.appium;

import java.util.ArrayList;
import java.util.Arrays;

import org.robotframework.javalib.library.AnnotationLibrary;

public class RobotLibrary extends AnnotationLibrary {
  static ArrayList<String> keywordPatterns =
      new ArrayList<String>() {
        {
          add("remoteserver/appium/*.class");
        }
      };

  public RobotLibrary() {
    super(keywordPatterns);
    System.out.println(Arrays.toString(super.getKeywordNames()));
  }
}
