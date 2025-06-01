package com.astroflexx.tasktracker;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * TaskTracker is a simple task management system that allows users to add,
 * update, and delete tasks.
 * It uses JSON for data storage and Gson for serialization/deserialization.
 */
public class TaskTracker {
    // The file path where tasks are stored
    private static final String filePath = "db/tasks.json";
    // Gson instance for JSON serialization/deserialization
    private static final Gson gson;
    private static final ArrayList<String> allowedOperations;

    // Static initializer block to initialize the Gson instance with pretty printing
    static {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        allowedOperations = new ArrayList<>(
                Arrays.asList("add", "update", "delete", "list", "mark-todo", "mark-in-progress", "mark-done"));
    }

    // simple Task class (Task type) to represent a task
    // we need this for reading a list of Tasks from the JSON file
    // i made the fields static only because the loadTasks method is static
    static class Task {
        private int id;
        private String description;
        private TaskStatus status = TaskStatus.TODO;
        // i tried to use LocalDateTime, but Gson doesn't support it out of the box
        // we need to write an adapter for it which i dont know how to do
        // so lets just use String and convert it to LocalDateTime when needed
        private String createdAt;
        private String updatedAt;

        public Task(int id, String description) {
            this.id = id;
            this.description = description;
            this.createdAt = LocalDateTime.now().toString();
            this.updatedAt = LocalDateTime.now().toString();
        }

        @Override
        public String toString() {
            DateTimeFormatter inputFormat = DateTimeFormatter.ISO_DATE_TIME;
            DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

            String created = LocalDateTime.parse(createdAt, inputFormat).format(outputFormat);
            String updated = LocalDateTime.parse(updatedAt, inputFormat).format(outputFormat);

            return String.format(
                    "Task ID: %d\nDescription: %s\nStatus: %s\nCreated At: %s\nUpdated At: %s\n",
                    id, description, status, created, updated);
        }
    }

    // simple enum to represent the status of a task
    enum TaskStatus {
        TODO,
        IN_PROGRESS,
        DONE
    }

    private static List<Task> loadTasks() {
        // try-with-resources, automatically closes the reader after use
        // like with open(...) in Python

        // the filereader object opens a connection to the file and returns a reader
        // object
        // that we will pass to Gson to read the JSON data
        try (Reader reader = new FileReader(filePath)) {
            // the way this works is:
            // we create a type token for a list of Tasks
            // and pass it to Gson to deserialize the JSON into a List<Task>
            // because Gson doesn't know the type of the list at runtime

            // create an anonymous class that extends TypeToken, with no implementation {}
            // this will embed the type information of List<Task> into the TypeToken
            // then we get it using getType() method

            // getType() will take myToken object
            // see its parent class is TypeToken<List<Task>>
            // from that parent class it will get the type List<Task>
            // and return it as a Type object

            // type erasure removes all generic type information at compile time
            // so we wont know the type of the list at runtime, to pass to Gson
            // but when we subclass TypeToken, the generic type info gets baked into its
            // superclass
            // which java reflection api can get at runtime
            Type taskListType = new TypeToken<List<Task>>() {
            }.getType();
            return gson.fromJson(reader, taskListType);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static void saveTasks(List<Task> tasks) {
        // same thing as with loadTasks, but we write the tasks to the file
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(tasks, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addTask(List<Task> tasks, String description) {
        int newId = tasks.isEmpty() ? 1 : tasks.get(tasks.size() - 1).id + 1; // auto-increment ID
        Task newTask = new Task(newId, description);
        tasks.add(newTask);
        saveTasks(tasks);
        System.out.println("Task added successfully: (ID: " + newId + ")");
    }

    private static void updateTask(List<Task> tasks, int taskId, String newDescription) {
        for (Task task : tasks) {
            if (task.id == taskId) {
                task.description = newDescription;
                task.updatedAt = LocalDateTime.now().toString();
                saveTasks(tasks);
                System.out.println("Task updated successfully (ID: " + taskId + ")");
                return;
            }
        }
        System.out.println("Task ID " + taskId + " not found.");
    }

    private static void deleteTask(List<Task> tasks, int taskId) {
        Iterator<Task> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.id == taskId) {
                iterator.remove();
                saveTasks(tasks);
                System.out.println("Task deleted successfully (ID: " + taskId + ")");
                return;
            }
        }
        System.out.println("Task ID " + taskId + " not found.");
    }

    private static void listTasks(List<Task> tasks, String filter) {
        if (filter == null || filter.isEmpty()) {
            filter = "all";
        }
        switch (filter.toLowerCase()) {
            case "all":
                for (Task task : tasks) {
                    System.out.println(task);
                }
                break;
            case "todo":
                for (Task task : tasks) {
                    if (task.status == TaskStatus.TODO) {
                        System.out.println(task);
                    }
                }
                break;
            case "in-progress":
                for (Task task : tasks) {
                    if (task.status == TaskStatus.IN_PROGRESS) {
                        System.out.println(task);
                    }
                }
                break;
            case "done":
                for (Task task : tasks) {
                    if (task.status == TaskStatus.DONE) {
                        System.out.println(task);
                    }
                }
                break;
            default:
                System.out.println("Invalid filter: " + filter);
                System.out.println("To list all tasks, use: `task-cli list`");
                System.out.println("Usage: task-cli list [todo|in-progress|done]");
                break;
        }
    }

    private static void markTodo(List<Task> tasks, int taskId) {
        for (Task task : tasks) {
            if (task.id == taskId) {
                task.status = TaskStatus.TODO;
                task.updatedAt = LocalDateTime.now().toString();
                saveTasks(tasks);
                System.out.println("Task marked as TODO (ID: " + taskId + ")");
                return;
            }
        }
        System.out.println("Task ID " + taskId + " not found.");
    }

    private static void markInProgress(List<Task> tasks, int taskId) {
        for (Task task : tasks) {
            if (task.id == taskId) {
                task.status = TaskStatus.IN_PROGRESS;
                task.updatedAt = LocalDateTime.now().toString();
                saveTasks(tasks);
                System.out.println("Task marked as IN PROGRESS (ID: " + taskId + ")");
                return;
            }
        }
        System.out.println("Task ID " + taskId + " not found.");
    }

    private static void markDone(List<Task> tasks, int taskId) {
        for (Task task : tasks) {
            if (task.id == taskId) {
                task.status = TaskStatus.DONE;
                task.updatedAt = LocalDateTime.now().toString();
                saveTasks(tasks);
                System.out.println("Task marked as DONE (ID: " + taskId + ")");
                return;
            }
        }
        System.out.println("Task ID " + taskId + " not found.");
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: task-cli <operation> [<args>]");
            return;
        }

        String operation = args[0].toLowerCase();
        if (!allowedOperations.contains(operation)) {
            System.out.println("Invalid operation: " + operation);
            System.out.println("Allowed operations: " + allowedOperations);
            return;
        }

        List<Task> tasks = loadTasks();

        switch (operation) {
            case "add":
                if (args.length < 2) {
                    System.out.println("Usage: task-cli add \"<description>\"");
                    return;
                }
                String description = args[1];
                addTask(tasks, description);
                break;
            case "update":
                if (args.length < 3) {
                    System.out.println("Usage: task-cli update <task_id> \"<new_description>\"");
                    return;
                }
                try {
                    int taskId = Integer.parseInt(args[1]);
                    String newDescription = args[2];
                    updateTask(tasks, taskId, newDescription);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid task ID: " + args[1]);
                    System.out.println(
                            "Use `task-cli list` to see all tasks and enter the ID of the task you want to update.");
                }
                break;
            case "delete":
                if (args.length < 2) {
                    System.out.println("Usage: task-cli delete <task_id>");
                    return;
                }
                try {
                    int deleteId = Integer.parseInt(args[1]);
                    deleteTask(tasks, deleteId);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid task ID: " + args[1]);
                    System.out.println(
                            "Use `task-cli list` to see all tasks and enter the ID of the task you want to delete.");
                }
                break;
            case "list":
                listTasks(tasks, args.length > 1 ? args[1] : null);
                break;
            case "mark-todo":
                if (args.length < 2) {
                    System.out.println("Usage: task-cli mark-todo <task_id>");
                    return;
                }
                try {
                    int todoId = Integer.parseInt(args[1]);
                    markTodo(tasks, todoId);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid task ID: " + args[1]);
                    System.out.println(
                            "Use `task-cli list` to see all tasks and enter the ID of the task you want to mark as TODO.");
                }
                break;
            case "mark-in-progress":
                if (args.length < 2) {
                    System.out.println("Usage: task-cli mark-in-progress <task_id>");
                    return;
                }
                try {
                    int inProgressId = Integer.parseInt(args[1]);
                    markInProgress(tasks, inProgressId);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid task ID: " + args[1]);
                    System.out.println(
                            "Use `task-cli list` to see all tasks and enter the ID of the task you want to mark as IN PROGRESS.");
                }
                break;
            case "mark-done":
                if (args.length < 2) {
                    System.out.println("Usage: task-cli mark-done <task_id>");
                    return;
                }
                try {
                    int doneId = Integer.parseInt(args[1]);
                    markDone(tasks, doneId);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid task ID: " + args[1]);
                    System.out.println(
                            "Use `task-cli list` to see all tasks and enter the ID of the task you want to mark as DONE.");
                }
                break;
            default:
                break;
        }
    }
}
