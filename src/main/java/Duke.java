import java.util.Scanner;

import java.util.List;
import java.util.ArrayList;

import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

import java.util.stream.Stream;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Duke {
    private static List<Task> tasks;

    public static void main(String[] args) {
        greet();
        Scanner sc = new Scanner(System.in);
        tasks = loadTasks();
        String input = sc.nextLine();
        while (true) {
            try {
                String[] parsedInput = input.strip().split("\\s+", 2);
                switch (parsedInput[0]) {
                case "bye":
                    if (parsedInput.length > 1) {
                        throw new InvalidCommandException();
                    }
                    exit();
                    break;
                case "list":
                    if (parsedInput.length > 1) {
                        throw new InvalidCommandException();
                    }
                    list();
                    break;
                case "done":
                    try {
                        int taskNumber = Integer.parseInt(parsedInput[1]);
                        completeTask(taskNumber);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new MissingTaskNumberException();
                    } catch (NumberFormatException e) {
                        throw new InvalidTaskException();
                    }
                    break;
                case "delete":
                    try {
                        int taskNumber = Integer.parseInt(parsedInput[1]);
                        deleteTask(taskNumber);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new MissingTaskNumberException();
                    } catch (NumberFormatException e) {
                        throw new InvalidTaskException();
                    }
                    break;
                case "todo":
                    try {
                        addTodo(parsedInput[1]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new EmptyTodoException();
                    }
                    break;
                case "deadline":
                    try {
                        addDeadline(parsedInput[1]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new EmptyDeadlineException();
                    }
                    break;
                case "event":
                    try {
                        addEvent(parsedInput[1]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new EmptyEventException();
                    }
                    break;
                default:
                    throw new InvalidCommandException();
                }
            } catch (InvalidCommandException e) {
                System.out.println(style(e.getMessage()));
            }
            saveTasks();
            input = sc.nextLine();
        }
    }

    private static void greet() {
        String logo = " ____        _        \n"
                + "|  _ \\ _   _| | _____ \n"
                + "| | | | | | | |/ / _ \\\n"
                + "| |_| | |_| |   <  __/\n"
                + "|____/ \\__,_|_|\\_\\___|\n";
        String greeting = "Hello, I'm Duke!\n"
                + "What can I do for you?";
        System.out.println(logo + style(greeting));
    }

    private static void exit() {
        String bye = "Goodbye. Hope to see you again soon!";
        System.out.println(style(bye));
        System.exit(0);
    }

    private static void addTodo(String description) {
        Todo todo = new Todo(description);
        tasks.add(todo);
        printTask(todo);
    }

    private static void addDeadline(String info) throws MissingDeadlineException, InvalidDeadlineException {
        try {
            String[] parsedInfo = info.split("\\s*/by\\s*", 2);
            Deadline deadline = new Deadline(parsedInfo[0], LocalDate.parse(parsedInfo[1]));
            tasks.add(deadline);
            printTask(deadline);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new MissingDeadlineException();
        } catch (DateTimeParseException e) {
            throw new InvalidDeadlineException();
        }
    }

    private static void addEvent(String info) throws MissingTimeException {
        try {
            String[] parsedInfo = info.split("\\s*/at\\s*", 2);
            Event event = new Event(parsedInfo[0], parsedInfo[1]);
            tasks.add(event);
            printTask(event);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new MissingTimeException();
        }
    }

    private static void printTask(Task task) {
        String numberOfTasks;
        if (tasks.size() == 1) {
            numberOfTasks = "There is now 1 task in the list.";
        } else {
            numberOfTasks = "There are now " + tasks.size() + " tasks in the list.";
        }
        System.out.println(style("Got it. I've added this task:\n  " + task + "\n" + numberOfTasks));
    }

    private static void list() {
        if (tasks.size() == 0) {
            System.out.println(style("There are no tasks now."));
        } else {
            StringBuilder list = new StringBuilder();
            for (int i = 0; i < tasks.size(); i++) {
                if (i > 0) {
                    list.append("\n");
                }
                list.append((i + 1) + ". " + tasks.get(i));
            }
            System.out.println(style(list.toString()));
        }
    }

    private static void completeTask(int taskNumber) throws InvalidTaskNumberException {
        try {
            int taskIndex = taskNumber - 1;
            Task completedTask = tasks.get(taskIndex).complete();
            tasks.set(taskIndex, completedTask);
            System.out.println(style("Nice! I've marked this task as done:\n  " + completedTask));
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidTaskNumberException(taskNumber);
        }
    }

    private static void deleteTask(int taskNumber) throws InvalidTaskNumberException {
        try {
            int taskIndex = taskNumber - 1;
            Task deletedTask = tasks.remove(taskIndex);
            System.out.println(style("Noted. I've removed this task:\n  " + deletedTask));
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidTaskNumberException(taskNumber);
        }
    }

    private static List<Task> loadTasks() {
        try {
            return Files.readAllLines(Paths.get("../data/duke.txt"))
                    .stream()
                    .flatMap(x -> {
                            try {
                                Task task;
                                char taskType = x.charAt(1);
                                boolean isDone = (x.charAt(4) == '\u2713');
                                x = x.substring(7);
                                String[] info;
                                switch(taskType) {
                                case 'T':
                                    task = new Todo(x);
                                    break;
                                case 'D':
                                    x = x.substring(0, x.length() - 1);
                                    info = x.split(" \\(by: ");
                                    task = new Deadline(info[0],
                                            LocalDate.parse(info[1], DateTimeFormatter.ofPattern("d MMM yyyy")));
                                    break;
                                case 'E':
                                    x = x.substring(0, x.length() - 1);
                                    info = x.split(" \\(at: ");
                                    task = new Event(info[0], info[1]);
                                    break;
                                default:
                                    return Stream.empty();
                                }
                                if (isDone) {
                                    task = task.complete();
                                }
                                return Stream.of(task);
                            } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException e) {
                                return Stream.empty();
                            }
                        })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static void saveTasks() {
        try {
            if (Files.notExists(Paths.get("../data"))) {
                Files.createDirectory(Paths.get("../data"));
            }
            Files.write(Paths.get("../data/duke.txt"), tasks.stream().map(x -> x.toString()).collect(Collectors.toList()));
        } catch (IOException e) {
            System.out.println(style("Unable to save tasks."));
        }
    }

    private static String style(String message) {
        String horizontalLine = new String(new char[76]).replace("\0", "-");
        message = horizontalLine + "\n" + message + "\n" + horizontalLine;
        return message.lines()
            .map(x -> "    " + x + "\n")
            .reduce("", (x, y) -> x + y);
    }
}
