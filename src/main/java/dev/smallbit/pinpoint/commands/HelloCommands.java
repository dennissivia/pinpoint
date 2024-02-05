package dev.smallbit.pinpoint.commands;

import org.springframework.shell.context.InteractionMode;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class HelloCommands {

  @ShellMethod(key = "hello", value = "Say hello")
  public String hello() {
    return "Hello, World!";
  }

  @ShellMethod(key = "goodbye", value = "Say goodbye")
  public String goodbye() {
    return "Goodbye, World!";
  }
}
